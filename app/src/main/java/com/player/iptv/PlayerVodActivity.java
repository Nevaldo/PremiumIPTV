package com.player.iptv;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.CredentialPreferences;
import com.player.iptv.data.HistoricoDao;
import com.player.iptv.model.Historico;
import com.player.iptv.utils.ContinueWatchingDialog;
import com.player.iptv.utils.DialogUtils;
import com.player.iptv.utils.MediaCacheManager;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import com.bumptech.glide.Glide;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class PlayerVodActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SUBTITLE = "subtitle";
    public static final String EXTRA_STREAM_ID = "stream_id";
    public static final String EXTRA_STREAM_URL = "stream_url";
    public static final String EXTRA_CONTAINER_EXT = "container_ext";
    public static final String EXTRA_IMAGE_URL = "image_url";

    private PlayerView playerView;
    private ExoPlayer player;
    private View controlsContainer;
    private View btnPlayPause;
    private ImageView ivPlayPauseCentral;
    private ImageView btnPlay;
    private ImageView btnVolume;
    private ImageView btnFullscreen;
    private SeekBar seekBar;
    private SeekBar volumeSeekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvSystemTime;
    private TextView tvSystemDate;
    private LinearLayout chaptersContainer;
    private int activeChapterIndex = 0;

    private boolean controlsVisible = true;
    private boolean isMuted = false;

    private final Handler handler = new Handler();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private HistoricoDao historicoDao;
    private String currentTitle;
    private String currentSubtitle;
    private int currentStreamId;
    private String currentExt;
    private String currentImageUrl;
    private long resumePosition;

    private final Runnable hideRunnable = () -> {
        if (controlsVisible) {
            controlsVisible = false;
            controlsContainer.animate().alpha(0f).setDuration(300).start();
        }
    };

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            java.util.Date now = new java.util.Date();
            if (tvSystemTime != null) tvSystemTime.setText(timeFormat.format(now));
            if (tvSystemDate != null) tvSystemDate.setText(dateFormat.format(now));
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_player_vod);

        historicoDao = AppDatabase.getInstance(this).historicoDao();

        playerView = findViewById(R.id.playerView);
        controlsContainer = findViewById(R.id.controlsContainer);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        ivPlayPauseCentral = findViewById(R.id.ivPlayPauseCentral);
        btnPlay = findViewById(R.id.btnPlay);
        btnVolume = findViewById(R.id.btnVolume);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        seekBar = findViewById(R.id.seekBar);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvSystemTime = findViewById(R.id.tvSystemTime);
        tvSystemDate = findViewById(R.id.tvSystemDate);
        chaptersContainer = findViewById(R.id.chaptersContainer);

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvYear = findViewById(R.id.tvYear);
        TextView tvDuration = findViewById(R.id.tvDuration);
        TextView tvGenres = findViewById(R.id.tvGenres);
        TextView tvSynopsis = findViewById(R.id.tvSynopsis);

        Intent intent = getIntent();
        currentTitle = intent.getStringExtra(EXTRA_TITLE);
        currentSubtitle = intent.getStringExtra(EXTRA_SUBTITLE);
        currentStreamId = intent.getIntExtra(EXTRA_STREAM_ID, 0);
        currentExt = intent.getStringExtra(EXTRA_CONTAINER_EXT);
        currentImageUrl = intent.getStringExtra(EXTRA_IMAGE_URL);
        if (currentExt == null || currentExt.isEmpty()) currentExt = "mp4";

        if (currentTitle != null) {
            tvTitle.setText(currentTitle);
        }
        if (currentSubtitle != null && !currentSubtitle.trim().isEmpty()) {
            tvGenres.setText(currentSubtitle);
            tvSynopsis.setText("Assistindo a " + currentTitle + " (" + currentSubtitle + "). " +
                "Paul Atreides se une a Chani e aos Fremen enquanto busca vingança contra os conspiradores que destruíram sua família.");
        } else {
            tvSynopsis.setText("Paul Atreides se une a Chani e aos Fremen enquanto busca vingança contra os conspiradores que destruíram sua família. Diante de uma escolha entre o amor de sua vida e o destino do universo, ele tenta evitar um futuro terrível.");
        }

        handler.post(clockRunnable);

        setupPlayer();
        setupControls();
        loadHistory();
        setupChapters();
        setupNextInQueue();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                DialogUtils.showExitPlayerDialog(PlayerVodActivity.this, PlayerVodActivity.this::saveAndExit);
            }
        });
    }

    private void loadHistory() {
        if (currentStreamId <= 0) return;
        disposables.add(Single.fromCallable(() ->
                historicoDao.getByStream(currentStreamId, "movie"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(historico -> {
                    if (historico != null && historico.getPosition() > 0 && historico.getDuration() > 0) {
                        resumePosition = historico.getPosition();
                        player.pause();
                        String remaining = formatTime(historico.getDuration() - historico.getPosition());
                        ContinueWatchingDialog.show(this, remaining, new ContinueWatchingDialog.Listener() {
                            @Override
                            public void onContinue() {
                                player.seekTo(resumePosition);
                                player.play();
                            }

                            @Override
                            public void onStartOver() {
                                player.play();
                                io.reactivex.Completable.fromAction(() -> {
                                    historico.setPosition(0);
                                    historicoDao.insert(historico);
                                    historicoDao.trimTo50();
                                }).subscribeOn(Schedulers.io()).subscribe();
                            }
                        });
                    }
                }, throwable -> {})
        );
    }

    private void setupPlayer() {
        @SuppressLint("UnsafeOptInUsageError") CacheDataSource.Factory cacheFactory = MediaCacheManager.getInstance(this).getCacheDataSourceFactory();
        @SuppressLint("UnsafeOptInUsageError") DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(cacheFactory);

        player = new ExoPlayer.Builder(this).setMediaSourceFactory(mediaSourceFactory).build();
        playerView.setPlayer(player);
        playerView.setVisibility(View.VISIBLE);

        String streamUrl = buildStreamUrl();

        MediaItem mediaItem = MediaItem.fromUri(streamUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long duration = player.getDuration();
                    if (duration > 0) {
                        seekBar.setMax((int) duration);
                        tvTotalTime.setText(formatTime(duration));
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayButtons(isPlaying);
                if (isPlaying) {
                    startProgressUpdate();
                } else {
                    handler.removeCallbacks(progressRunnable);
                }
            }
        });
    }

    private final Runnable progressRunnable = new Runnable() {
        private long lastSave = 0;
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long pos = player.getCurrentPosition();
                seekBar.setProgress((int) pos);
                tvCurrentTime.setText(formatTime(pos));

                int newChapter = 0;
                if (pos >= 4215000) newChapter = 3;
                else if (pos >= 2850000) newChapter = 2;
                else if (pos >= 1425000) newChapter = 1;

                if (newChapter != activeChapterIndex) {
                    activeChapterIndex = newChapter;
                    updateChaptersUI();
                }

                long now = System.currentTimeMillis();
                if (now - lastSave > 5000) {
                    lastSave = now;
                    saveHistory(pos);
                }

                handler.postDelayed(this, 1000);
            }
        }
    };

    private void startProgressUpdate() {
        handler.removeCallbacks(progressRunnable);
        handler.post(progressRunnable);
    }

    private void saveHistory(long position) {
        if (currentStreamId <= 0 || currentTitle == null) return;

        long duration = player != null ? player.getDuration() : 0;
        String info = currentSubtitle != null ? currentSubtitle : "";

        disposables.add(io.reactivex.Single.fromCallable(() -> {
            Historico existing = historicoDao.getByStream(currentStreamId, "movie");
            Historico historico = new Historico(
                    currentStreamId,
                    currentTitle,
                    info,
                    formatTime(duration - position) + " restantes",
                    currentImageUrl,
                    "movie",
                    currentExt,
                    position,
                    duration,
                    System.currentTimeMillis()
            );
            if (existing != null) {
                historico.setId(existing.getId());
            }
            historicoDao.insert(historico);
            historicoDao.trimTo50();
            return true;
        }).subscribeOn(Schedulers.io()).subscribe());
    }

    private String buildStreamUrl() {
        Intent intent = getIntent();

        String directUrl = intent.getStringExtra(EXTRA_STREAM_URL);
        if (directUrl != null && !directUrl.isEmpty()) {
            return directUrl;
        }

        CredentialPreferences prefs = new CredentialPreferences(this);
        String server = prefs.getServerUrl();
        String user = prefs.getUsername();
        String pass = prefs.getPassword();

        return server + "movie/" + user + "/" + pass + "/" + currentStreamId + "." + currentExt;
    }

    private void setupControls() {
        controlsContainer.setOnClickListener(v -> toggleControls());

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnPlay.setOnClickListener(v -> togglePlayPause());

        View btnRewind10 = findViewById(R.id.btnRewind10);
        View btnForward10 = findViewById(R.id.btnForward10);
        if (btnRewind10 != null) btnRewind10.setOnClickListener(v -> seekRelative(-10000));
        if (btnForward10 != null) btnForward10.setOnClickListener(v -> seekRelative(10000));

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> saveAndExit());
        
        View btnPremium = findViewById(R.id.btnPremium);
        if (btnPremium != null) btnPremium.setOnClickListener(v -> showMockToast("Premium Ativo!"));

        setupMockListeners();

        if (volumeSeekBar != null) {
            volumeSeekBar.setProgress(80);
            if (player != null) player.setVolume(0.8f);
            volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && player != null) {
                        player.setVolume(progress / 100f);
                        isMuted = (progress == 0);
                        btnVolume.setColorFilter(getColor(isMuted ? R.color.text_secondary : R.color.text_primary));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { handler.removeCallbacks(hideRunnable); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { resetAutoHide(); }
            });
        }

        btnVolume.setOnClickListener(v -> {
            isMuted = !isMuted;
            if (player != null) player.setVolume(isMuted ? 0f : 0.8f);
            if (volumeSeekBar != null) volumeSeekBar.setProgress(isMuted ? 0 : 80);
            btnVolume.setColorFilter(getColor(isMuted ? R.color.text_secondary : R.color.text_primary));
        });

        btnFullscreen.setOnClickListener(v -> {
            saveAndExit();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(hideRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                resetAutoHide();
            }
        });

        controlsContainer.setAlpha(1f);
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

    private void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    private void updatePlayButtons(boolean isPlaying) {
        int icon = isPlaying ? R.drawable.ic_pause_big : R.drawable.ic_play;
        if (ivPlayPauseCentral != null) ivPlayPauseCentral.setImageResource(icon);
        btnPlay.setImageResource(isPlaying ? R.drawable.ic_pause_big : R.drawable.ic_play);
    }

    private void resetAutoHide() {
        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, 5000);
    }

    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long min = (totalSec % 3600) / 60;
        long sec = totalSec % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, min, sec);
        }
        return String.format("%d:%02d", min, sec);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            saveHistory(player.getCurrentPosition());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            saveHistory(player.getCurrentPosition());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        disposables.dispose();
        if (player != null) {
            player.release();
        }
    }

    private void saveAndExit() {
        if (player == null || currentStreamId <= 0 || currentTitle == null) {
            finish();
            return;
        }
        long pos = player.getCurrentPosition();
        long duration = player.getDuration();
        String info = currentSubtitle != null ? currentSubtitle : "";

        disposables.add(io.reactivex.Single.fromCallable(() -> {
            Historico existing = historicoDao.getByStream(currentStreamId, "movie");
            Historico historico = new Historico(
                    currentStreamId, currentTitle, info,
                    formatTime(duration - pos) + " restantes",
                    currentImageUrl, "movie", currentExt,
                    pos, duration, System.currentTimeMillis()
            );
            if (existing != null) {
                historico.setId(existing.getId());
            }
            historicoDao.insert(historico);
            historicoDao.trimTo50();
            return true;
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(success -> finish(), throwable -> finish()));
    }

    private void seekRelative(long ms) {
        if (player == null) return;
        long target = player.getCurrentPosition() + ms;
        if (target < 0) target = 0;
        if (target > player.getDuration()) target = player.getDuration();
        player.seekTo(target);
        seekBar.setProgress((int) target);
        tvCurrentTime.setText(formatTime(target));
        resetAutoHide();
    }

    private void setupNextInQueue() {
        ImageView ivNextPoster = findViewById(R.id.ivNextPoster);
        TextView tvNextTitle = findViewById(R.id.tvNextTitle);
        TextView tvNextDuration = findViewById(R.id.tvNextDuration);
        
        if (tvNextTitle != null) tvNextTitle.setText("Mad Max: Estrada da Fúria");
        if (tvNextDuration != null) tvNextDuration.setText("2h 0min");
        
        if (ivNextPoster != null && currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(this)
                .load(currentImageUrl)
                .transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop())
                .into(ivNextPoster);
        }
    }

    private void setupChapters() {
        if (chaptersContainer == null) return;
        chaptersContainer.removeAllViews();
        
        String[] chapterTitles = {"1. Início", "2. A Chegada", "3. Aliança", "4. A Batalha"};
        long[] chapterTimes = {0, 1425000, 2850000, 4215000};
        
        for (int i = 0; i < chapterTitles.length; i++) {
            final int index = i;
            final long timeMs = chapterTimes[i];
            final String title = chapterTitles[i];
            
            View item = getLayoutInflater().inflate(R.layout.item_chapter, chaptersContainer, false);
            TextView tvTitle = item.findViewById(R.id.tvChapterTitle);
            TextView tvTime = item.findViewById(R.id.tvChapterTime);
            ImageView ivThumb = item.findViewById(R.id.ivChapterThumb);
            View cardFrame = item.findViewById(R.id.chapterCardFrame);
            
            tvTitle.setText(title);
            tvTime.setText(formatTime(timeMs));
            
            if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                Glide.with(this)
                    .load(currentImageUrl)
                    .transform(new com.bumptech.glide.load.resource.bitmap.CenterCrop())
                    .into(ivThumb);
            }
            
            if (i == activeChapterIndex) {
                cardFrame.setBackgroundResource(R.drawable.bg_chapter_card);
                tvTitle.setTextColor(getColor(R.color.colorAccent));
            } else {
                cardFrame.setBackgroundResource(android.R.color.transparent);
                tvTitle.setTextColor(getColor(R.color.text_secondary));
            }
            
            item.setOnClickListener(v -> {
                if (player != null) {
                    player.seekTo(timeMs);
                    player.play();
                    activeChapterIndex = index;
                    updateChaptersUI();
                }
            });
            
            chaptersContainer.addView(item);
        }
    }

    private void updateChaptersUI() {
        if (chaptersContainer == null) return;
        for (int i = 0; i < chaptersContainer.getChildCount(); i++) {
            View item = chaptersContainer.getChildAt(i);
            TextView tvTitle = item.findViewById(R.id.tvChapterTitle);
            View cardFrame = item.findViewById(R.id.chapterCardFrame);
            if (i == activeChapterIndex) {
                cardFrame.setBackgroundResource(R.drawable.bg_chapter_card);
                tvTitle.setTextColor(getColor(R.color.colorAccent));
            } else {
                cardFrame.setBackgroundResource(android.R.color.transparent);
                tvTitle.setTextColor(getColor(R.color.text_secondary));
            }
        }
    }

    private void setupMockListeners() {
        View btnSearch = findViewById(R.id.btnSearch);
        if (btnSearch != null) btnSearch.setOnClickListener(v -> showMockToast("Busca integrada em breve..."));

        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) btnSettings.setOnClickListener(v -> showMockToast("Ajustes de reprodução e vídeo"));

        View btnMinhaLista = findViewById(R.id.btnMinhaLista);
        if (btnMinhaLista != null) btnMinhaLista.setOnClickListener(v -> showMockToast("Adicionado à sua lista"));

        View btnFavorito = findViewById(R.id.btnFavorito);
        if (btnFavorito != null) btnFavorito.setOnClickListener(v -> showMockToast("Adicionado aos favoritos"));

        View btnCompartilhar = findViewById(R.id.btnCompartilhar);
        if (btnCompartilhar != null) btnCompartilhar.setOnClickListener(v -> showMockToast("Compartilhando conteúdo"));

        View btnMoreInfo = findViewById(R.id.btnMoreInfo);
        if (btnMoreInfo != null) btnMoreInfo.setOnClickListener(v -> showMockToast("Título: " + currentTitle));

        View btnLegendas = findViewById(R.id.btnLegendas);
        if (btnLegendas != null) btnLegendas.setOnClickListener(v -> showMockToast("Seleção de Legenda (Padrão: Desativado)"));

        View btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) btnLanguage.setOnClickListener(v -> showMockToast("Faixa de áudio atual: PT-BR"));

        View btnQualidade = findViewById(R.id.btnQualidade);
        if (btnQualidade != null) btnQualidade.setOnClickListener(v -> showMockToast("Ajuste automático de bitrate"));

        View btnPrev = findViewById(R.id.btnPrev);
        if (btnPrev != null) btnPrev.setOnClickListener(v -> showMockToast("Reiniciando vídeo..."));

        View btnNext = findViewById(R.id.btnNext);
        if (btnNext != null) btnNext.setOnClickListener(v -> showMockToast("Próximo vídeo da fila"));

        View btnNextChapter = findViewById(R.id.btnNextChapter);
        if (btnNextChapter != null) {
            btnNextChapter.setOnClickListener(v -> {
                int nextIndex = (activeChapterIndex + 1) % 4;
                long[] chapterTimes = {0, 1425000, 2850000, 4215000};
                if (player != null) {
                    player.seekTo(chapterTimes[nextIndex]);
                    activeChapterIndex = nextIndex;
                    updateChaptersUI();
                }
            });
        }
    }

    private void showMockToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
