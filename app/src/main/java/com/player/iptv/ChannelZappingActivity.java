package com.player.iptv;

import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.player.iptv.adapter.LiveChannelAdapter;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.data.CredentialRepository;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.LiveStream;
import com.player.iptv.viewmodel.ZappingManager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ChannelZappingActivity extends AppCompatActivity {

    private ZappingManager zappingManager;
    private LiveChannelAdapter channelAdapter;
    private List<LiveStream> liveStreams = new ArrayList<>();
    private CompositeDisposable disposables = new CompositeDisposable();
    private Gson gson = new Gson();
    private String baseUrl, username, password;

    private FrameLayout playerContainer;
    private TextView channelInfoText, channelNumberText;
    private RecyclerView channelListRecycler;

    private StringBuilder numberBuffer = new StringBuilder();
    private Handler numberHandler = new Handler();
    private boolean showingChannelList = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_zapping);

        playerContainer = findViewById(R.id.player_container);
        channelInfoText = findViewById(R.id.channel_info_text);
        channelNumberText = findViewById(R.id.channel_number_text);
        channelListRecycler = findViewById(R.id.channel_list_recycler);

        PlayerView playerView = new PlayerView(this);
        playerContainer.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        zappingManager = new ZappingManager(this, playerView);
        zappingManager.setOnChannelChangeListener(new ZappingManager.OnChannelChangeListener() {
            @Override
            public void onChannelChanged(LiveStream channel, int channelNumber) {
                channelInfoText.setText(channel.getName());
                channelNumberText.setText(String.valueOf(channelNumber));
                channelInfoText.setVisibility(View.VISIBLE);
                channelNumberText.setVisibility(View.VISIBLE);
                channelInfoText.removeCallbacks(null);
                channelInfoText.postDelayed(() -> channelInfoText.setVisibility(View.GONE), 3000);
            }

            @Override
            public void onZappingStart() {
                channelNumberText.setVisibility(View.VISIBLE);
            }

            @Override
            public void onZappingEnd(long durationMs) {
                channelNumberText.postDelayed(() -> channelNumberText.setVisibility(View.GONE), 2000);
            }
        });

        zappingManager.setOnErrorListener((error, channel) ->
            Toast.makeText(this, error + ": " + channel.getName(), Toast.LENGTH_SHORT).show()
        );

        loadData();
    }

    private void loadData() {
        CredentialRepository repo = new CredentialRepository(this);
        disposables.add(repo.getActiveCredential()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(cred -> {
                baseUrl = cred.getServerUrl();
                username = cred.getUsername();
                password = cred.getPassword();
                zappingManager.setCredentials(baseUrl, username, password);
                loadChannels();
            }, e -> {
                Toast.makeText(this, "Erro ao carregar credenciais", Toast.LENGTH_SHORT).show();
                finish();
            })
        );
    }

    private void loadChannels() {
        disposables.add(Single.fromCallable(() -> {
            ContentCacheDao dao = AppDatabase.getInstance(this).contentCacheDao();
            List<ContentCache> caches = dao.getByType("live_streams");
            List<LiveStream> result = new ArrayList<>();
            for (ContentCache c : caches) {
                LiveStream s = gson.fromJson(c.getJson(), LiveStream.class);
                if (s != null) result.add(s);
            }
            return result;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(channels -> {
            liveStreams = channels;
            if (liveStreams.isEmpty()) {
                Toast.makeText(this, "Nenhum canal encontrado", Toast.LENGTH_SHORT).show();
                return;
            }
            zappingManager.replaceChannelList(liveStreams);
            zappingManager.loadChannel(0);
            setupChannelList();
        }, e -> {
            Toast.makeText(this, "Erro ao carregar canais", Toast.LENGTH_SHORT).show();
        }));
    }

    private void setupChannelList() {
        channelAdapter = new LiveChannelAdapter();
        channelAdapter.setOnItemClickListener((channel, pos) -> {
            zappingManager.zapToChannel(channel.getNum());
            hideChannelList();
        });
        channelAdapter.submitList(liveStreams);
        channelListRecycler.setLayoutManager(new LinearLayoutManager(this));
        channelListRecycler.setAdapter(channelAdapter);
    }

    private void showChannelList() {
        showingChannelList = true;
        channelListRecycler.setVisibility(View.VISIBLE);
        channelInfoText.setVisibility(View.GONE);
        channelNumberText.setVisibility(View.GONE);
        channelListRecycler.requestFocus();
    }

    private void hideChannelList() {
        showingChannelList = false;
        channelListRecycler.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!showingChannelList) { zappingManager.zapUp(); return true; }
                return false;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!showingChannelList) { zappingManager.zapDown(); return true; }
                return false;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (showingChannelList) return false;
                showChannelList();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!showingChannelList) zappingManager.zapDown();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!showingChannelList) zappingManager.zapUp();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (showingChannelList) { hideChannelList(); return true; }
                return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_0: case KeyEvent.KEYCODE_1: case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3: case KeyEvent.KEYCODE_4: case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6: case KeyEvent.KEYCODE_7: case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
                numberBuffer.append(keyCode - KeyEvent.KEYCODE_0);
                channelNumberText.setText(numberBuffer.toString());
                channelNumberText.setVisibility(View.VISIBLE);
                numberHandler.removeCallbacksAndMessages(null);
                numberHandler.postDelayed(() -> {
                    if (numberBuffer.length() > 0) {
                        zappingManager.zapToNumber(numberBuffer.toString());
                        numberBuffer.setLength(0);
                        channelNumberText.setVisibility(View.GONE);
                    }
                }, 2000);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (zappingManager != null) zappingManager.release();
        disposables.clear();
    }
}
