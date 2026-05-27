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
    private ImageView btnPlayPause;
    private ImageView btnPlay;
    private ImageView btnVolume;
    private ImageView btnFullscreen;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;

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
        btnPlay = findViewById(R.id.btnPlay);
        btnVolume = findViewById(R.id.btnVolume);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);

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
        if (currentSubtitle != null) {
            tvSubtitle.setText(currentSubtitle);
        }

        setupPlayer();
        setupControls();
        loadHistory();

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

        btnVolume.setOnClickListener(v -> {
            isMuted = !isMuted;
            player.setVolume(isMuted ? 0f : 1f);
            btnVolume.setColorFilter(getColor(isMuted ? R.color.text_secondary : R.color.text_primary));
        });

        btnFullscreen.setOnClickListener(v -> {
            finish();
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
        btnPlayPause.setImageResource(icon);
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
}
