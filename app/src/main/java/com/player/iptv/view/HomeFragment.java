package com.player.iptv.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.player.iptv.PlayerVodActivity;
import com.player.iptv.R;
import com.player.iptv.adapter.ContinuarAssistindoAdapter;
import com.player.iptv.adapter.MovieAdapter;
import com.player.iptv.adapter.SeriesAdapter;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.model.Movie;
import com.player.iptv.model.Series;
import com.player.iptv.viewmodel.HomeViewModel;

import java.util.List;


public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;

    private CardView bannerCard;
    private ImageView bannerImage;
    private TextView bannerTitle;
    private TextView bannerSubtitle;

    private RecyclerView rvFeaturedMovies;
    private RecyclerView rvFeaturedSeries;
    private RecyclerView rvContinueWatching;
    private LinearLayout categoriesContainer;

    private CardView cardDestaque;
    private ImageView destaqueImage;
    private TextView destaqueTitle;
    private TextView destaqueSubtitle;

    private MovieAdapter featuredMoviesAdapter;
    private SeriesAdapter featuredSeriesAdapter;
    private ContinuarAssistindoAdapter continueWatchingAdapter;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bannerCard = view.findViewById(R.id.bannerCard);
        bannerImage = view.findViewById(R.id.bannerImage);
        bannerTitle = view.findViewById(R.id.bannerTitle);
        bannerSubtitle = view.findViewById(R.id.bannerSubtitle);

        rvFeaturedMovies = view.findViewById(R.id.rvFeaturedMovies);
        rvFeaturedSeries = view.findViewById(R.id.rvFeaturedSeries);
        rvContinueWatching = view.findViewById(R.id.rvContinueWatching);
        categoriesContainer = view.findViewById(R.id.categoriesContainer);

        cardDestaque = view.findViewById(R.id.cardDestaque);
        destaqueImage = view.findViewById(R.id.destaqueImage);
        destaqueTitle = view.findViewById(R.id.destaqueTitle);
        destaqueSubtitle = view.findViewById(R.id.destaqueSubtitle);

        setupAdapters();
        setupViewModel();
        loadContinueWatching();
    }

    private void setupAdapters() {
        featuredMoviesAdapter = new MovieAdapter(false);
        featuredSeriesAdapter = new SeriesAdapter(5);
        continueWatchingAdapter = new ContinuarAssistindoAdapter(ContinuarAssistindoAdapter.TYPE_LIST);

        rvFeaturedMovies.setAdapter(featuredMoviesAdapter);
        rvFeaturedSeries.setAdapter(featuredSeriesAdapter);
        rvContinueWatching.setAdapter(continueWatchingAdapter);

        featuredMoviesAdapter.setOnItemClickListener((movie, pos) -> openMovieDetail(movie));

        featuredSeriesAdapter.setOnItemClickListener((series, pos) ->
            Toast.makeText(getContext(), series.getName(), Toast.LENGTH_SHORT).show());

        continueWatchingAdapter.setOnItemClickListener((item, pos) -> {
            Intent intent = new Intent(getContext(), PlayerVodActivity.class);
            intent.putExtra(PlayerVodActivity.EXTRA_TITLE, item.getTitulo());
            intent.putExtra(PlayerVodActivity.EXTRA_STREAM_ID, item.getStreamId());
            intent.putExtra(PlayerVodActivity.EXTRA_CONTAINER_EXT, item.getContainerExt());
            intent.putExtra(PlayerVodActivity.EXTRA_SUBTITLE, item.getInfo());
            intent.putExtra(PlayerVodActivity.EXTRA_IMAGE_URL, item.getImageUrl());
            startActivity(intent);
        });
    }

    private void loadContinueWatching() {
        AppDatabase.getInstance(requireContext()).historicoDao().getContinueWatching().observe(getViewLifecycleOwner(), list -> {
            if (list != null && list.size() > 3) {
                continueWatchingAdapter.setItems(list.subList(0, 3));
            } else {
                continueWatchingAdapter.setItems(list);
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        viewModel.getBannerMovie().observe(getViewLifecycleOwner(), this::updateBanner);
        viewModel.getFeaturedMovies().observe(getViewLifecycleOwner(), featuredMoviesAdapter::submitList);
        viewModel.getFeaturedSeries().observe(getViewLifecycleOwner(), featuredSeriesAdapter::submitList);
        viewModel.getTopCategories().observe(getViewLifecycleOwner(), this::buildCategoryChips);
        viewModel.getDestaqueSeries().observe(getViewLifecycleOwner(), this::updateDestaque);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.loadHomeData();
    }

    private void updateBanner(Movie movie) {
        if (movie == null) return;

        String title = movie.getName() != null ? movie.getName() : movie.getTitle();
        bannerTitle.setText(title);
        bannerSubtitle.setText("Filme em destaque");

        String imageUrl = movie.getStreamIcon();
        if (imageUrl == null && movie.getInfo() != null) {
            imageUrl = movie.getInfo().getMovieImage();
        }

        Glide.with(this)
            .load(imageUrl)
            .transform(new CenterCrop(), new RoundedCorners(16))
            .placeholder(R.color.bg_surface)
            .error(R.color.bg_surface)
            .into(bannerImage);
    }

    private void updateDestaque(Series series) {
        if (series == null) return;

        destaqueTitle.setText(series.getName());
        destaqueSubtitle.setText("Série em destaque");

        String imageUrl = series.getCover();
        if (imageUrl == null && series.getInfo() != null) {
            imageUrl = series.getInfo().getCover();
        }

        Glide.with(this)
            .load(imageUrl)
            .transform(new CenterCrop(), new RoundedCorners(16))
            .placeholder(R.color.bg_surface)
            .error(R.color.bg_surface)
            .into(destaqueImage);
    }

    private void buildCategoryChips(List<String> categories) {
        categoriesContainer.removeAllViews();

        for (String cat : categories) {
            TextView tv = new TextView(getContext());
            tv.setText(cat);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tv.setBackgroundResource(R.drawable.bg_menu_item);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dpToPx(12), 0);
            tv.setLayoutParams(params);
            tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            tv.setFocusable(true);
            tv.setClickable(true);

            categoriesContainer.addView(tv);
        }
    }

    private void openMovieDetail(Movie movie) {
        String title = movie.getName() != null ? movie.getName() : movie.getTitle();
        String imageUrl = movie.getStreamIcon();
        if (imageUrl == null && movie.getInfo() != null) {
            imageUrl = movie.getInfo().getMovieImage();
        }
        String subtitle = movie.getInfo() != null && movie.getInfo().getReleaseDate() != null ? movie.getInfo().getReleaseDate() : "";
        String directSource = movie.getDirectSource();

        MovieDetailFragment detail = MovieDetailFragment.newInstance(
                movie.getStreamId(), title, directSource,
                movie.getContainerExtension(), imageUrl, subtitle,
                movie.getCategoryId()
        );

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, detail)
                .addToBackStack(null)
                .commit();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
