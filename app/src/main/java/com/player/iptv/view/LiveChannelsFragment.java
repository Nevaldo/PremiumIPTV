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
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.R;
import com.player.iptv.adapter.LiveChannelAdapter;
import com.player.iptv.data.CredentialRepository;
import com.player.iptv.model.LiveStream;
import com.player.iptv.viewmodel.LiveChannelViewModel;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class LiveChannelsFragment extends Fragment {

    private LiveChannelViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    private RecyclerView rvDestaques;
    private LinearLayout categoriesContainer;
    private PlayerView playerView;
    private View playerOverlay;
    private TextView tvPlayerTitle;
    private TextView tvPlayerSubtitle;
    private LiveChannelAdapter destaquesAdapter;
    private ExoPlayer player;
    private String baseUrl;
    private String username;
    private String password;

    public LiveChannelsFragment() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
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

        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);

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
                setupViewModel();
            }, e -> {
                Toast.makeText(getContext(), "Erro ao carregar credenciais", Toast.LENGTH_SHORT).show();
            })
        );
    }

    private void setupAdapters() {
        destaquesAdapter = new LiveChannelAdapter();
        rvDestaques.setAdapter(destaquesAdapter);

        destaquesAdapter.setOnItemClickListener((channel, pos) -> playChannel(channel));
    }

    private void playChannel(LiveStream channel) {
        if (baseUrl == null || username == null || password == null) return;

        String url = baseUrl + "live/" + username + "/" + password + "/" + channel.getStreamId() + ".m3u8";
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        playerView.setVisibility(View.VISIBLE);
        tvPlayerTitle.setText(channel.getName());
        tvPlayerSubtitle.setText("Ao vivo");
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
        if (channels == null) return;
        destaquesAdapter.submitList(channels);
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
