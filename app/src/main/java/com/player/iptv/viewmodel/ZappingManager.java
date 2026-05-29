package com.player.iptv.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import com.player.iptv.model.LiveStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZappingManager {
    private static final String TAG = "ZappingManager";

    private Context context;
    private ExoPlayer mainPlayer;
    private DefaultTrackSelector trackSelector;
    private PlayerView playerView;

    private List<LiveStream> channelList;
    private int currentChannelIndex = 0;
    private boolean isZapping = false;
    private long lastZappingTime = 0;

    private String baseUrl;
    private String username;
    private String password;

    private ExecutorService executorService;
    private Handler mainHandler;

    private Map<Integer, ExoPlayer> preloadedPlayers = new HashMap<>();
    private Map<Integer, Boolean> playerReadyState = new HashMap<>();

    private OnChannelChangeListener channelChangeListener;
    private OnZappingErrorListener errorListener;

    private boolean released = false;

    public interface OnChannelChangeListener {
        void onChannelChanged(LiveStream channel, int channelNumber);
        void onZappingStart();
        void onZappingEnd(long durationMs);
    }

    public interface OnZappingErrorListener {
        void onError(String error, LiveStream channel);
    }

    public ZappingManager(Context context, PlayerView playerView) {
        this.context = context;
        this.playerView = playerView;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.channelList = new ArrayList<>();

        initializePlayers();
    }

    public void setCredentials(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayers() {
        trackSelector = new DefaultTrackSelector(context);
        trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .setMaxVideoSize(1920, 1080)
                        .setMaxVideoBitrate(8000000)
                        .setMaxAudioBitrate(320000)
                        .build()
        );

        mainPlayer = new ExoPlayer.Builder(context).setTrackSelector(trackSelector).build();

        playerView.setPlayer(mainPlayer);

        setupPlayerListeners();
    }

    private void setupPlayerListeners() {
        mainPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY && isZapping) {
                    long duration = System.currentTimeMillis() - lastZappingTime;
                    if (channelChangeListener != null) {
                        channelChangeListener.onZappingEnd(duration);
                    }
                    isZapping = false;
                    Log.d(TAG, "Zapping completo em " + duration + "ms");
                    preloadAdjacentChannels();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                isZapping = false;
                if (errorListener != null) {
                    errorListener.onError("Erro no canal: " + error.getMessage(),
                            getCurrentChannel());
                }
                handlePlaybackError(error);
            }
        });
    }

    public void setChannelList(List<LiveStream> channels) {
        this.channelList = new ArrayList<>(channels);
        if (!channels.isEmpty()) {
            channelList.sort((a, b) -> Integer.compare(a.getNum(), b.getNum()));
            loadChannel(0);
        }
    }

    public void replaceChannelList(List<LiveStream> channels) {
        this.channelList = new ArrayList<>(channels);
        if (!channels.isEmpty()) {
            channelList.sort((a, b) -> Integer.compare(a.getNum(), b.getNum()));
        }
    }

    public void loadChannel(int index) {
        if (index < 0 || index >= channelList.size()) return;

        currentChannelIndex = index;
        LiveStream channel = channelList.get(index);

        mainPlayer.stop();
        loadChannelWithOptimization(channel);

        if (channelChangeListener != null) {
            channelChangeListener.onChannelChanged(channel, channel.getNum());
        }
    }

    public void zapUp() {
        if (channelList.isEmpty()) return;

        int newIndex = currentChannelIndex + 1;
        if (newIndex >= channelList.size()) {
            newIndex = 0;
        }
        zapToChannelByIndex(newIndex);
    }

    public void zapDown() {
        if (channelList.isEmpty()) return;

        int newIndex = currentChannelIndex - 1;
        if (newIndex < 0) {
            newIndex = channelList.size() - 1;
        }
        zapToChannelByIndex(newIndex);
    }

    public void zapToNumber(int number) {
        if (channelList.isEmpty()) return;

        for (int i = 0; i < channelList.size(); i++) {
            if (channelList.get(i).getNum() == number) {
                zapToChannelByIndex(i);
                return;
            }
        }

        if (errorListener != null) {
            errorListener.onError("Canal " + number + " não encontrado", null);
        }
    }

    public void zapToNumber(String numberStr) {
        try {
            int number = Integer.parseInt(numberStr);
            zapToNumber(number);
        } catch (NumberFormatException e) {
            if (errorListener != null) {
                errorListener.onError("Número inválido: " + numberStr, null);
            }
        }
    }

    public void zapToChannel(int channelNumber) {
        for (int i = 0; i < channelList.size(); i++) {
            if (channelList.get(i).getNum() == channelNumber) {
                zapToChannelByIndex(i);
                return;
            }
        }
    }

    public void zapToChannelByName(String channelName) {
        for (int i = 0; i < channelList.size(); i++) {
            if (channelList.get(i).getName().equalsIgnoreCase(channelName)) {
                zapToChannelByIndex(i);
                return;
            }
        }
    }

    private void zapToChannelByIndex(int index) {
        if (isZapping) {
            Log.d(TAG, "Já em zapping, ignorando");
            return;
        }

        if (index < 0 || index >= channelList.size()) return;
        if (index == currentChannelIndex) return;

        isZapping = true;
        lastZappingTime = System.currentTimeMillis();

        if (channelChangeListener != null) {
            channelChangeListener.onZappingStart();
        }

        LiveStream newChannel = channelList.get(index);
        int oldIndex = currentChannelIndex;
        currentChannelIndex = index;

        if (preloadedPlayers.containsKey(index) && playerReadyState.getOrDefault(index, false)) {
            swapToPreloadedPlayer(index);
        } else {
            loadChannelWithOptimization(newChannel);
        }

        if (channelChangeListener != null) {
            channelChangeListener.onChannelChanged(newChannel, newChannel.getNum());
        }

        if (oldIndex >= 0 && oldIndex != index) {
            preloadChannel(oldIndex);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void loadChannelWithOptimization(LiveStream channel) {
        if (released || executorService.isShutdown()) return;
        executorService.execute(() -> {
            try {
                DefaultHttpDataSource.Factory dataSourceFactory =
                        new DefaultHttpDataSource.Factory()
                                .setConnectTimeoutMs(2000)
                                .setReadTimeoutMs(2000)
                                .setAllowCrossProtocolRedirects(true);

                String url = buildUrl(channel);
                MediaSource mediaSource;

                if (url.contains(".m3u8") || url.contains(".m3u")) {
                    mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                            .setAllowChunklessPreparation(true)
                            .setMetadataType(HlsMediaSource.METADATA_TYPE_ID3)
                            .createMediaSource(MediaItem.fromUri(url));
                } else {
                    mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(url));
                }

                final MediaSource finalMediaSource = mediaSource;
                mainHandler.post(() -> {
                    mainPlayer.setMediaSource(finalMediaSource);
                    mainPlayer.prepare();
                    mainPlayer.setPlayWhenReady(true);
                });

            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar canal: " + e.getMessage());
                if (errorListener != null) {
                    mainHandler.post(() -> errorListener.onError(
                            "Falha ao carregar canal: " + e.getMessage(), channel));
                }
                isZapping = false;
            }
        });
    }

    private String buildUrl(LiveStream channel) {
        return baseUrl + "live/" + username + "/" + password + "/" + channel.getStreamId() + ".m3u8";
    }

    private void swapToPreloadedPlayer(int index) {
        ExoPlayer preloaded = preloadedPlayers.get(index);

        if (preloaded != null && playerReadyState.getOrDefault(index, false)) {
            mainPlayer.stop();

            ExoPlayer temp = mainPlayer;
            mainPlayer = preloaded;
            preloadedPlayers.put(index, temp);

            playerView.setPlayer(mainPlayer);
            mainPlayer.setVolume(1f);
            mainPlayer.setPlayWhenReady(true);

            playerReadyState.put(index, false);

            Log.d(TAG, "Zapping ultrarrápido para canal " + index);
        } else {
            loadChannelWithOptimization(channelList.get(index));
        }
    }

    private void preloadAdjacentChannels() {
        int nextIndex = currentChannelIndex + 1;
        int prevIndex = currentChannelIndex - 1;
        int nextNextIndex = currentChannelIndex + 2;

        if (nextIndex < channelList.size()) {
            preloadChannel(nextIndex);
        }
        if (prevIndex >= 0) {
            preloadChannel(prevIndex);
        }
        if (nextNextIndex < channelList.size()) {
            preloadChannel(nextNextIndex);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void preloadChannel(int index) {
        if (index < 0 || index >= channelList.size()) return;
        if (preloadedPlayers.containsKey(index)) return;
        if (index == currentChannelIndex) return;
        if (released || executorService.isShutdown()) return;

        executorService.execute(() -> {
            try {
                LiveStream channel = channelList.get(index);
                ExoPlayer preloadPlayer = new ExoPlayer.Builder(context)
                        .setTrackSelector(trackSelector)
                        .build();

                DefaultHttpDataSource.Factory dataSourceFactory =
                        new DefaultHttpDataSource.Factory()
                                .setConnectTimeoutMs(3000)
                                .setReadTimeoutMs(3000)
                                .setAllowCrossProtocolRedirects(true);

                String url = buildUrl(channel);
                MediaSource mediaSource;

                if (url.contains(".m3u8") || url.contains(".m3u")) {
                    mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                            .setAllowChunklessPreparation(true)
                            .createMediaSource(MediaItem.fromUri(url));
                } else {
                    mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(url));
                }

                preloadPlayer.setMediaSource(mediaSource);
                preloadPlayer.prepare();
                preloadPlayer.setVolume(0f);
                preloadPlayer.setPlayWhenReady(false);

                Thread.sleep(300);

                preloadedPlayers.put(index, preloadPlayer);
                playerReadyState.put(index, true);

                Log.d(TAG, "Canal pré-carregado: " + channel.getName());

            } catch (Exception e) {
                Log.e(TAG, "Erro ao pré-carregar: " + e.getMessage());
                playerReadyState.put(index, false);
            }
        });
    }

    private void handlePlaybackError(PlaybackException error) {
        mainHandler.postDelayed(() -> {
            if (!released && getCurrentChannel() != null) {
                loadChannelWithOptimization(getCurrentChannel());
            }
        }, 1000);
    }

    public LiveStream getCurrentChannel() {
        if (channelList.isEmpty() || currentChannelIndex >= channelList.size()) return null;
        return channelList.get(currentChannelIndex);
    }

    public int getCurrentChannelNumber() {
        if (channelList.isEmpty() || currentChannelIndex >= channelList.size()) return 0;
        return channelList.get(currentChannelIndex).getNum();
    }

    public List<LiveStream> getChannelList() {
        return channelList;
    }

    public int getTotalChannels() {
        return channelList.size();
    }

    public boolean isZapping() {
        return isZapping;
    }

    public void release() {
        released = true;
        mainHandler.removeCallbacksAndMessages(null);
        if (mainPlayer != null) {
            mainPlayer.release();
            mainPlayer = null;
        }
        for (ExoPlayer player : preloadedPlayers.values()) {
            if (player != null) {
                player.release();
            }
        }
        preloadedPlayers.clear();
        playerReadyState.clear();
        executorService.shutdown();
    }

    public void setOnChannelChangeListener(OnChannelChangeListener listener) {
        this.channelChangeListener = listener;
    }

    public void setOnErrorListener(OnZappingErrorListener listener) {
        this.errorListener = listener;
    }
}
