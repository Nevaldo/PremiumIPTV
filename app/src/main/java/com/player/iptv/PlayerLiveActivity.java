package com.player.iptv;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.player.iptv.adapter.PlayerChannelAdapter;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.CredentialPreferences;
import com.player.iptv.data.HistoricoDao;
import com.player.iptv.model.Historico;
import com.player.iptv.model.LiveStream;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class PlayerLiveActivity extends AppCompatActivity {

    // ─── Extras ───────────────────────────────────────────────────────────────
    public static final String EXTRA_STREAM_ID       = "stream_id";
    public static final String EXTRA_STREAM_URL      = "stream_url";
    public static final String EXTRA_CHANNEL_NAME    = "channel_name";
    public static final String EXTRA_CHANNEL_ICON    = "channel_icon";
    public static final String EXTRA_EPG_TITLE       = "epg_title";
    public static final String EXTRA_EPG_SUBTITLE    = "epg_subtitle";
    public static final String EXTRA_EPG_START       = "epg_start";   // "HH:mm"
    public static final String EXTRA_EPG_END         = "epg_end";     // "HH:mm"
    public static final String EXTRA_EPG_DATE        = "epg_date";    // readable
    public static final String EXTRA_EPG_STADIUM     = "epg_stadium";
    public static final String EXTRA_EPG_CITY        = "epg_city";
    public static final String EXTRA_NEXT_TITLE      = "next_title";
    public static final String EXTRA_NEXT_TIME       = "next_time";
    public static final String EXTRA_NEXT_ICON       = "next_icon";
    /** Optional: pass a list of channel names/icons for the switcher */
    public static final String EXTRA_CHANNEL_LIST    = "channel_list";

    // ─── Views ────────────────────────────────────────────────────────────────
    private PlayerView playerView;
    private View controlsContainer;
    private ImageView ivPlayPauseIcon;
    private ImageView btnPlaySmall;
    private TextView txtClock;
    private TextView txtDate;
    private TextView txtMatch;
    private TextView txtTeams;
    private TextView txtStartTime;
    private TextView txtEndTime;
    private ProgressBar progressLive;
    private ImageView ivChannelLogo;
    private TextView txtInfoTitle;
    private TextView txtInfoSubtitle;
    private TextView txtInfoDate;
    private TextView txtInfoTime;
    private TextView txtInfoStadium;
    private TextView txtInfoCity;
    private ImageView ivNextChannelLogo;
    private TextView txtNextTitle;
    private TextView txtNextTime;
    private RecyclerView recyclerChannels;

    // ─── Quality buttons ──────────────────────────────────────────────────────
    private LinearLayout btnQualityAuto;
    private LinearLayout btnQuality1080;
    private LinearLayout btnQuality720;
    private LinearLayout btnQuality480;

    // ─── Player & state ───────────────────────────────────────────────────────
    private ExoPlayer player;
    private boolean isPlaying = true;
    private boolean controlsVisible = true;

    private final Handler handler = new Handler();
    private final CompositeDisposable disposables = new CompositeDisposable();

    // ─── Data ─────────────────────────────────────────────────────────────────
    private int currentStreamId;
    private String currentStreamUrl;
    private String currentChannelName;
    private String currentChannelIcon;
    private String epgStart = "00:00";
    private String epgEnd   = "23:59";
    private HistoricoDao historicoDao;
    private PlayerChannelAdapter channelAdapter;

    // ─── Runnables ────────────────────────────────────────────────────────────
    private final Runnable hideRunnable = () -> {
        if (controlsVisible) {
            controlsVisible = false;
            if (controlsContainer != null)
                controlsContainer.animate().alpha(0f).setDuration(400).start();
        }
    };

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClockViews();
            updateLiveProgress();
            handler.postDelayed(this, 30_000);
        }
    };

    // ──────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_player_live);

        historicoDao = AppDatabase.getInstance(this).historicoDao();

        readExtras();
        initViews();
        initPlayer();
        bindChannelInfo();
        setupControls();
        setupQuality();
        setupActionBar();
        setupChannelList();
        updateClockViews();
        updateLiveProgress();
        handler.post(clockRunnable);
        resetAutoHide();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    // ─── Read Intent extras ───────────────────────────────────────────────────
    private void readExtras() {
        Intent i = getIntent();
        currentStreamId   = i.getIntExtra(EXTRA_STREAM_ID, 0);
        currentStreamUrl  = i.getStringExtra(EXTRA_STREAM_URL);
        currentChannelName = i.getStringExtra(EXTRA_CHANNEL_NAME);
        currentChannelIcon = i.getStringExtra(EXTRA_CHANNEL_ICON);
        epgStart = nonNull(i.getStringExtra(EXTRA_EPG_START), "19:30");
        epgEnd   = nonNull(i.getStringExtra(EXTRA_EPG_END),   "22:00");

        if (currentChannelName == null) currentChannelName = "AO VIVO";
    }

    // ─── Views ────────────────────────────────────────────────────────────────
    @SuppressLint("WrongViewCast")
    private void initViews() {
        playerView         = findViewById(R.id.playerView);
        controlsContainer  = findViewById(R.id.controlsContainer);
        ivPlayPauseIcon    = findViewById(R.id.ivPlayPauseIcon);
        btnPlaySmall       = findViewById(R.id.btnPlaySmall);
        txtClock           = findViewById(R.id.txtClock);
        txtDate            = findViewById(R.id.txtDate);
        txtMatch           = findViewById(R.id.txtMatch);
        txtTeams           = findViewById(R.id.txtTeams);
        txtStartTime       = findViewById(R.id.txtStartTime);
        txtEndTime         = findViewById(R.id.txtEndTime);
        progressLive       = findViewById(R.id.progressLive);
        ivChannelLogo      = findViewById(R.id.ivChannelLogo);
        txtInfoTitle       = findViewById(R.id.txtInfoTitle);
        txtInfoSubtitle    = findViewById(R.id.txtInfoSubtitle);
        txtInfoDate        = findViewById(R.id.txtInfoDate);
        txtInfoTime        = findViewById(R.id.txtInfoTime);
        txtInfoStadium     = findViewById(R.id.txtInfoStadium);
        txtInfoCity        = findViewById(R.id.txtInfoCity);
        ivNextChannelLogo  = findViewById(R.id.ivNextChannelLogo);
        txtNextTitle       = findViewById(R.id.txtNextTitle);
        txtNextTime        = findViewById(R.id.txtNextTime);
        recyclerChannels   = findViewById(R.id.recyclerChannels);
        btnQualityAuto     = findViewById(R.id.btnQualityAuto);
        btnQuality1080     = findViewById(R.id.btnQuality1080);
        btnQuality720      = findViewById(R.id.btnQuality720);
        btnQuality480      = findViewById(R.id.btnQuality480);
    }

    // ─── Player ───────────────────────────────────────────────────────────────
    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        String url = buildStreamUrl();
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        isPlaying = true;
    }

    private String buildStreamUrl() {
        // If explicit URL was provided, use it directly
        if (currentStreamUrl != null && !currentStreamUrl.isEmpty()) {
            return currentStreamUrl;
        }
        // Otherwise build from credentials + stream_id
        CredentialPreferences prefs = new CredentialPreferences(this);
        String server = prefs.getServerUrl();
        String user   = prefs.getUsername();
        String pass   = prefs.getPassword();
        if (currentStreamId > 0 && !server.isEmpty()) {
            return server + "live/" + user + "/" + pass + "/" + currentStreamId + ".m3u8";
        }
        // Fallback test stream
        return "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";
    }

    // ─── Bind channel / EPG info ──────────────────────────────────────────────
    private void bindChannelInfo() {
        Intent i = getIntent();

        // Channel logo
        if (currentChannelIcon != null && !currentChannelIcon.isEmpty()) {
            Glide.with(this)
                    .load(currentChannelIcon)
                    .transform(new CenterInside())
                    .into(ivChannelLogo);
        }

        // Event title / teams
        String epgTitle    = nonNull(i.getStringExtra(EXTRA_EPG_TITLE),    currentChannelName);
        String epgSubtitle = nonNull(i.getStringExtra(EXTRA_EPG_SUBTITLE), "Ao Vivo");
        txtMatch.setText(epgTitle);
        txtTeams.setText(epgSubtitle);

        // Progress times
        txtStartTime.setText(epgStart);
        txtEndTime.setText(epgEnd);

        // Side panel info
        txtInfoTitle.setText(epgTitle);
        txtInfoSubtitle.setText(nonNull(i.getStringExtra(EXTRA_EPG_SUBTITLE), ""));

        String dateStr = nonNull(i.getStringExtra(EXTRA_EPG_DATE), getTodayReadable());
        txtInfoDate.setText(dateStr);
        txtInfoTime.setText(epgStart + " - " + epgEnd);

        txtInfoStadium.setText(nonNull(i.getStringExtra(EXTRA_EPG_STADIUM), ""));
        txtInfoCity.setText(nonNull(i.getStringExtra(EXTRA_EPG_CITY), ""));

        // Next event
        String nextTitle = i.getStringExtra(EXTRA_NEXT_TITLE);
        String nextTime  = i.getStringExtra(EXTRA_NEXT_TIME);
        String nextIcon  = i.getStringExtra(EXTRA_NEXT_ICON);
        if (txtNextTitle != null) txtNextTitle.setText(nonNull(nextTitle, "—"));
        if (txtNextTime  != null) txtNextTime.setText(nonNull(nextTime, ""));
        if (ivNextChannelLogo != null && nextIcon != null && !nextIcon.isEmpty()) {
            Glide.with(this).load(nextIcon).transform(new CenterInside()).into(ivNextChannelLogo);
        } else if (currentChannelIcon != null && ivNextChannelLogo != null) {
            Glide.with(this).load(currentChannelIcon).transform(new CenterInside()).into(ivNextChannelLogo);
        }
    }

    // ─── Controls ─────────────────────────────────────────────────────────────
    private void setupControls() {
        // Touch anywhere to toggle controls
        controlsContainer.setOnClickListener(v -> toggleControls());

        // Main play/pause button
        View btnPause = findViewById(R.id.btnPause);
        if (btnPause != null) btnPause.setOnClickListener(v -> togglePlayPause());

        // Small play button
        if (btnPlaySmall != null) btnPlaySmall.setOnClickListener(v -> togglePlayPause());

        // Back
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Rewind/Forward (live: only seek within tv_archive buffer)
        View btnRewind = findViewById(R.id.btnRewind10);
        if (btnRewind != null) btnRewind.setOnClickListener(v -> {
            if (player != null) {
                long target = Math.max(0, player.getCurrentPosition() - 10_000);
                player.seekTo(target);
            }
            resetAutoHide();
        });

        View btnForward = findViewById(R.id.btnForward10);
        if (btnForward != null) btnForward.setOnClickListener(v -> {
            if (player != null) {
                player.seekTo(player.getCurrentPosition() + 10_000);
            }
            resetAutoHide();
        });

        // Settings
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v ->
                showToast("Configurações do player"));

        // Fullscreen (no-op, already fullscreen)
        View btnFullscreen = findViewById(R.id.btnFullscreen);
        if (btnFullscreen != null) btnFullscreen.setOnClickListener(v ->
                showToast("Modo de tela cheia ativo"));

        // Premium badge
        View btnPremium = findViewById(R.id.btnPremium);
        if (btnPremium != null) btnPremium.setOnClickListener(v ->
                showToast("Premium ativo! Desfrute de todos os canais."));

        // Ver programação
        View btnVer = findViewById(R.id.btnVerProgramacao);
        if (btnVer != null) btnVer.setOnClickListener(v ->
                showToast("Guia de programação em breve"));
    }

    private void togglePlayPause() {
        if (player == null) return;
        if (isPlaying) {
            player.pause();
            isPlaying = false;
            ivPlayPauseIcon.setImageResource(R.drawable.ic_play);
            if (btnPlaySmall != null) btnPlaySmall.setImageResource(R.drawable.ic_play);
        } else {
            player.play();
            isPlaying = true;
            ivPlayPauseIcon.setImageResource(R.drawable.ic_pause_big);
            if (btnPlaySmall != null) btnPlaySmall.setImageResource(R.drawable.ic_pause_big);
        }
        resetAutoHide();
    }

    private void toggleControls() {
        if (controlsVisible) {
            controlsVisible = false;
            controlsContainer.animate().alpha(0f).setDuration(300).start();
            handler.removeCallbacks(hideRunnable);
        } else {
            controlsVisible = true;
            controlsContainer.setAlpha(1f);
            resetAutoHide();
        }
    }

    private void resetAutoHide() {
        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, 5_000);
    }

    // ─── Quality selector ─────────────────────────────────────────────────────
    private int selectedQuality = 0; // 0=AUTO, 1=1080, 2=720, 3=480

    private void setupQuality() {
        View[] btns = {btnQualityAuto, btnQuality1080, btnQuality720, btnQuality480};
        String[] labels = {"AUTO (Recomendado)", "1080p Full HD", "720p HD", "480p SD"};

        for (int idx = 0; idx < btns.length; idx++) {
            if (btns[idx] == null) continue;
            final int q = idx;
            btns[idx].setOnClickListener(v -> {
                selectQuality(q);
                showToast("Qualidade: " + labels[q]);
                resetAutoHide();
            });
        }
        selectQuality(0); // default AUTO
    }

    private void selectQuality(int q) {
        selectedQuality = q;
        LinearLayout[] btns = {btnQualityAuto, btnQuality1080, btnQuality720, btnQuality480};
        for (int i = 0; i < btns.length; i++) {
            if (btns[i] == null) continue;
            btns[i].setBackgroundResource(
                    i == q ? R.drawable.bg_card_history : R.drawable.bg_quality_inactive);
        }
    }

    // ─── Action Bar ───────────────────────────────────────────────────────────
    private void setupActionBar() {
        setAction(R.id.btnGuia,       "Guia de Programação em breve");
        setAction(R.id.btnFavoritos,  "Canal adicionado aos favoritos!");
        setAction(R.id.btnMultiTela,  "Multi-tela disponível em breve");
        setAction(R.id.btnAudio,      "Faixa de áudio: Padrão");
        setAction(R.id.btnLegendas,   "Legendas: Desativadas");
        setAction(R.id.btnComentarios,"Comentários ao vivo");
        setAction(R.id.btnDenunciar,  "Denúncia enviada. Obrigado!");
    }

    private void setAction(int viewId, String message) {
        View v = findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> {
            showToast(message);
            resetAutoHide();
        });
    }

    // ─── Channel switcher ─────────────────────────────────────────────────────
    private final Gson gson = new Gson();

    private void setupChannelList() {
        if (recyclerChannels == null) return;

        channelAdapter = new PlayerChannelAdapter();
        recyclerChannels.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerChannels.setAdapter(channelAdapter);

        // Load channels from DB (same pattern as LiveChannelViewModel)
        disposables.add(
                Single.<List<LiveStream>>create(emitter -> {
                    List<com.player.iptv.model.ContentCache> caches =
                            AppDatabase.getInstance(this).contentCacheDao().getByType("live_streams");
                    List<LiveStream> result = new ArrayList<>();
                    for (com.player.iptv.model.ContentCache cache : caches) {
                        try {
                            LiveStream s = gson.fromJson(cache.getJson(), LiveStream.class);
                            if (s != null) result.add(s);
                        } catch (Exception ignored) {}
                    }
                    emitter.onSuccess(result);
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                streams -> {
                                    if (!streams.isEmpty()) {
                                        channelAdapter.submitList(streams);
                                        markCurrentChannel(streams);
                                    }
                                },
                                err -> { /* silent fail, channel list just stays empty */ }
                        )
        );

        channelAdapter.setOnChannelSelectedListener((channel, position) -> {
            switchToChannel(channel);
            resetAutoHide();
        });
    }

    private void markCurrentChannel(List<LiveStream> streams) {
        for (int i = 0; i < streams.size(); i++) {
            if (streams.get(i).getStreamId() == currentStreamId) {
                channelAdapter.setSelectedPosition(i);
                recyclerChannels.scrollToPosition(i);
                break;
            }
        }
    }



    private void switchToChannel(LiveStream channel) {
        if (player == null) return;

        currentStreamId   = channel.getStreamId();
        currentChannelName = channel.getName();
        currentChannelIcon = channel.getStreamIcon();

        // Update logo
        if (currentChannelIcon != null && !currentChannelIcon.isEmpty()) {
            Glide.with(this).load(currentChannelIcon).transform(new CenterInside()).into(ivChannelLogo);
        }
        // Update title
        txtMatch.setText(channel.getName());
        txtTeams.setText("Ao Vivo");
        txtInfoTitle.setText(channel.getName());
        txtInfoSubtitle.setText("Canal");

        // Switch stream
        CredentialPreferences prefs = new CredentialPreferences(this);
        String url = prefs.getServerUrl() + "live/" + prefs.getUsername() + "/" + prefs.getPassword() + "/" + channel.getStreamId() + ".m3u8";

        MediaItem item = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(item);
        player.prepare();
        player.play();
        isPlaying = true;
        ivPlayPauseIcon.setImageResource(R.drawable.ic_pause_big);

        saveChannelHistory(channel);
    }

    // ─── Clock & progress ─────────────────────────────────────────────────────
    private void updateClockViews() {
        Date now = new Date();
        if (txtClock != null)
            txtClock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        if (txtDate != null)
            txtDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now));
    }

    private void updateLiveProgress() {
        try {
            int startMin = parseTimeToMinutes(epgStart);
            int endMin   = parseTimeToMinutes(epgEnd);

            Calendar cal = Calendar.getInstance();
            int nowMin   = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

            if (endMin <= startMin) endMin += 24 * 60; // handle midnight crossing
            if (nowMin  <  startMin) nowMin += 24 * 60;

            int progress = 0;
            if (endMin > startMin) {
                progress = (int) (((float)(nowMin - startMin) / (endMin - startMin)) * 100);
                progress = Math.max(0, Math.min(100, progress));
            }
            if (progressLive != null) progressLive.setProgress(progress);
        } catch (Exception ignored) {
            if (progressLive != null) progressLive.setProgress(50);
        }
    }

    private int parseTimeToMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // ─── History ──────────────────────────────────────────────────────────────
    private void saveChannelHistory(LiveStream channel) {
        disposables.add(Single.fromCallable(() -> {
            Historico existing = historicoDao.getByStream(channel.getStreamId(), "live");
            Historico h = new Historico(
                    channel.getStreamId(), channel.getName(), "Ao vivo",
                    "", channel.getStreamIcon(), "live", "m3u8",
                    0, 0, System.currentTimeMillis());
            if (existing != null) h.setId(existing.getId());
            historicoDao.insert(h);
            historicoDao.trimTo50();
            return true;
        }).subscribeOn(Schedulers.io()).subscribe());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private String nonNull(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }

    private String getTodayReadable() {
        return new SimpleDateFormat("dd 'de' MMMM", new Locale("pt", "BR")).format(new Date());
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && !player.isPlaying() && isPlaying) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        disposables.dispose();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}