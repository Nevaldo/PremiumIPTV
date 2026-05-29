package com.player.iptv.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.R;
import com.player.iptv.adapter.LiveChannelAdapter;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.CredentialRepository;
import com.player.iptv.data.HistoricoDao;
import com.player.iptv.model.Historico;
import com.player.iptv.model.LiveStream;
import com.player.iptv.viewmodel.LiveChannelViewModel;
import com.player.iptv.viewmodel.ZappingManager;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class LiveChannelsFragment extends Fragment {

    private LiveChannelViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private HistoricoDao historicoDao;

    private RecyclerView rvDestaques;
    private LinearLayout categoriesContainer;
    private PlayerView playerView;
    private View playerOverlay;
    private TextView tvPlayerTitle;
    private TextView tvPlayerSubtitle;
    private TextView tvChannelNumber;
    private LiveChannelAdapter destaquesAdapter;
    private ZappingManager zappingManager;
    private String baseUrl;
    private String username;
    private String password;

    private List<LiveStream> currentChannels;
    private boolean hasSelectedChannel = false;

    public LiveChannelsFragment() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (zappingManager != null) {
            zappingManager.release();
        }
        disposables.clear();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_channels, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvDestaques = view.findViewById(R.id.rvDestaques);
        categoriesContainer = view.findViewById(R.id.categoriesContainer);
        playerView = view.findViewById(R.id.playerView);
        playerOverlay = view.findViewById(R.id.playerOverlay);
        tvPlayerTitle = view.findViewById(R.id.tvPlayerTitle);
        tvPlayerSubtitle = view.findViewById(R.id.tvPlayerSubtitle);
        tvChannelNumber = view.findViewById(R.id.tvChannelNumber);

        historicoDao = AppDatabase.getInstance(requireContext()).historicoDao();

        setupAdapters();
        loadCredentials();
    }

    private void loadCredentials() {
        CredentialRepository repo = new CredentialRepository(requireContext());
        disposables.add(repo.getActiveCredential()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(credential -> {
                baseUrl = credential.getServerUrl();
                username = credential.getUsername();
                password = credential.getPassword();
                setupZappingManager();
                setupViewModel();
            }, e -> {
                Toast.makeText(getContext(), "Erro ao carregar credenciais", Toast.LENGTH_SHORT).show();
            })
        );
    }

    private void setupZappingManager() {
        zappingManager = new ZappingManager(requireContext(), playerView);
        zappingManager.setCredentials(baseUrl, username, password);

        zappingManager.setOnChannelChangeListener(new ZappingManager.OnChannelChangeListener() {
            @Override
            public void onChannelChanged(LiveStream channel, int channelNumber) {
                tvPlayerTitle.setText(channel.getName());
                tvPlayerSubtitle.setText("Ao vivo");
                tvChannelNumber.setText(String.valueOf(channelNumber));
                tvChannelNumber.setVisibility(View.VISIBLE);
            }

            @Override
            public void onZappingStart() {}

            @Override
            public void onZappingEnd(long durationMs) {}
        });

        zappingManager.setOnErrorListener((error, channel) -> {
            Toast.makeText(getContext(), error + ": " + channel.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAdapters() {
        destaquesAdapter = new LiveChannelAdapter();
        rvDestaques.setAdapter(destaquesAdapter);

        destaquesAdapter.setOnItemClickListener((channel, pos) -> playChannel(channel));
    }

    private void playChannel(LiveStream channel) {
        if (zappingManager == null) return;
        hasSelectedChannel = true;

        zappingManager.replaceChannelList(currentChannels);
        zappingManager.zapToChannel(channel.getNum());

        playerView.setVisibility(View.VISIBLE);
        saveChannelHistory(channel);
    }

    private void saveChannelHistory(LiveStream channel) {
        disposables.add(io.reactivex.Single.fromCallable(() -> {
            Historico existing = historicoDao.getByStream(channel.getStreamId(), "live");
            Historico historico = new Historico(
                    channel.getStreamId(),
                    channel.getName(),
                    "Ao vivo",
                    "",
                    channel.getStreamIcon(),
                    "live",
                    "m3u8",
                    0, 0,
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

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(LiveChannelViewModel.class);

        viewModel.getFilteredChannels().observe(getViewLifecycleOwner(), this::onChannelsLoaded);
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::buildCategoryChips);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.loadChannels();
    }

    private void onChannelsLoaded(List<LiveStream> channels) {
        if (channels == null || channels.isEmpty()) return;
        currentChannels = channels;
        destaquesAdapter.submitList(channels);

        if (zappingManager == null) return;

        if (!hasSelectedChannel) {
            hasSelectedChannel = true;
            zappingManager.replaceChannelList(channels);
            zappingManager.loadChannel(0);
            playerView.setVisibility(View.VISIBLE);
            tvChannelNumber.setVisibility(View.VISIBLE);
            if (zappingManager.getCurrentChannel() != null) {
                saveChannelHistory(zappingManager.getCurrentChannel());
            }
        } else {
            int currentNum = zappingManager.getCurrentChannelNumber();
            zappingManager.replaceChannelList(channels);
            if (currentNum > 0) {
                zappingManager.zapToChannel(currentNum);
            }
        }
    }

    private void buildCategoryChips(List<String> categories) {
        categoriesContainer.removeAllViews();

        for (String cat : categories) {
            TextView tv = new TextView(getContext());
            tv.setText(cat);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tv.setBackgroundResource(R.drawable.bg_menu_item);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dpToPx(12), 0);
            tv.setLayoutParams(params);
            tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            tv.setFocusable(true);
            tv.setClickable(true);

            tv.setOnClickListener(v -> {
                deselectAllChips();
                selectChip(tv);
                viewModel.filterByCategory(cat.equals("Todas") ? "" : cat);
            });

            categoriesContainer.addView(tv);
        }

        if (categoriesContainer.getChildCount() > 0) {
            selectChip((TextView) categoriesContainer.getChildAt(0));
        }
    }

    private void deselectAllChips() {
        for (int i = 0; i < categoriesContainer.getChildCount(); i++) {
            TextView tv = (TextView) categoriesContainer.getChildAt(i);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tv.setBackgroundResource(R.drawable.bg_menu_item);
        }
    }

    private void selectChip(TextView tv) {
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        tv.setBackgroundResource(R.drawable.bg_menu_item_active);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
