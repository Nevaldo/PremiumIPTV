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

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;

    // Banner hero
    private CardView bannerCard;
    private ImageView bannerImage;
    private TextView bannerTitle;
    private TextView bannerSubtitle;
    private TextView bannerDescription;
    private TextView bannerBadge;
    private View btnBannerPlay;
    private View btnBannerInfo;

    // Seções principais
    private RecyclerView rvFeaturedMovies;
    private RecyclerView rvFeaturedSeries;
    private RecyclerView rvContinueWatching;

    // Botões "Ver todos"
    private TextView btnVerTodosFilmes;
    private TextView btnVerTodosSeries;

    // Card destaque / lançamentos
    private CardView cardDestaque;
    private ImageView destaqueImage;
    private TextView destaqueTitle;
    private TextView destaqueSubtitle;
    private View btnDestaquePlay;

    // Adapters
    private MovieAdapter featuredMoviesAdapter;
    private SeriesAdapter featuredSeriesAdapter;
    private ContinuarAssistindoAdapter continueWatchingAdapter;

    // Dados do banner atual
    private Movie currentBannerMovie;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Banner hero
        bannerCard        = view.findViewById(R.id.bannerCard);
        bannerImage       = view.findViewById(R.id.bannerImage);
        bannerTitle       = view.findViewById(R.id.bannerTitle);
        bannerSubtitle    = view.findViewById(R.id.bannerSubtitle);
        bannerDescription = view.findViewById(R.id.bannerDescription);
        bannerBadge       = view.findViewById(R.id.bannerBadge);
        btnBannerPlay     = view.findViewById(R.id.btnBannerPlay);
        btnBannerInfo     = view.findViewById(R.id.btnBannerInfo);

        // RecyclerViews
        rvFeaturedMovies  = view.findViewById(R.id.rvFeaturedMovies);
        rvFeaturedSeries  = view.findViewById(R.id.rvFeaturedSeries);
        rvContinueWatching = view.findViewById(R.id.rvContinueWatching);

        // Botões "Ver todos"
        btnVerTodosFilmes = view.findViewById(R.id.btnVerTodosFilmes);
        btnVerTodosSeries = view.findViewById(R.id.btnVerTodosSeries);

        // Lançamento / destaque
        cardDestaque    = view.findViewById(R.id.cardDestaque);
        destaqueImage   = view.findViewById(R.id.destaqueImage);
        destaqueTitle   = view.findViewById(R.id.destaqueTitle);
        destaqueSubtitle = view.findViewById(R.id.destaqueSubtitle);
        btnDestaquePlay = view.findViewById(R.id.btnDestaquePlay);

        setupAdapters();
        setupListeners();
        setupViewModel();
        loadContinueWatching();
    }

    private void setupAdapters() {
        featuredMoviesAdapter  = new MovieAdapter(false);
        featuredSeriesAdapter  = new SeriesAdapter(SeriesAdapter.TYPE_NORMAL);
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

    private void setupListeners() {
        // Botão "Assistir agora" no banner
        if (btnBannerPlay != null) {
            btnBannerPlay.setOnClickListener(v -> {
                if (currentBannerMovie != null) openMovieDetail(currentBannerMovie);
            });
        }

        // Botão "Mais informações" no banner
        if (btnBannerInfo != null) {
            btnBannerInfo.setOnClickListener(v -> {
                if (currentBannerMovie != null) openMovieDetail(currentBannerMovie);
            });
        }

        // Ver todos — filmes
        if (btnVerTodosFilmes != null) {
            btnVerTodosFilmes.setOnClickListener(v -> {
                if (getActivity() instanceof com.player.iptv.MainActivity) {
                    ((com.player.iptv.MainActivity) getActivity()).navigateTo(R.id.menuFilmes);
                }
            });
        }

        // Ver todas — séries
        if (btnVerTodosSeries != null) {
            btnVerTodosSeries.setOnClickListener(v -> {
                if (getActivity() instanceof com.player.iptv.MainActivity) {
                    ((com.player.iptv.MainActivity) getActivity()).navigateTo(R.id.menuSeries);
                }
            });
        }

        // Botão "Assistir agora" no card de lançamento
        if (btnDestaquePlay != null) {
            btnDestaquePlay.setOnClickListener(v ->
                Toast.makeText(getContext(), "Assistir lançamento", Toast.LENGTH_SHORT).show());
        }

        // Click no card destaque
        if (cardDestaque != null) {
            cardDestaque.setOnClickListener(v ->
                Toast.makeText(getContext(), "Lançamento", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadContinueWatching() {
        AppDatabase.getInstance(requireContext()).historicoDao().getContinueWatching()
            .observe(getViewLifecycleOwner(), list -> {
                if (list != null && list.size() > 4) {
                    continueWatchingAdapter.setItems(list.subList(0, 4));
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
        currentBannerMovie = movie;

        String title = movie.getName() != null ? movie.getName() : movie.getTitle();
        bannerTitle.setText(title);

        // Subtítulo: ano • duração • classificação
        StringBuilder sub = new StringBuilder();
        if (movie.getInfo() != null) {
            if (movie.getInfo().getReleaseDate() != null) {
                String year = movie.getInfo().getReleaseDate();
                if (year.length() >= 4) year = year.substring(0, 4);
                sub.append(year);
            }
            if (movie.getInfo().getDurationSecs() != null) {
                if (sub.length() > 0) sub.append("  •  ");
                try {
                    int mins = Integer.parseInt(movie.getInfo().getDurationSecs()) / 60;
                    sub.append(mins).append("m");
                } catch (Exception e) {
                    sub.append(movie.getInfo().getDurationSecs());
                }
            }
            if (movie.getInfo().getRating() != null) {
                if (sub.length() > 0) sub.append("  •  ");
                sub.append(movie.getInfo().getRating());
            }
        }
        bannerSubtitle.setText(sub.toString());

        // Descrição / sinopse
        if (bannerDescription != null && movie.getInfo() != null) {
            String plot = movie.getInfo().getPlot();
            bannerDescription.setText(plot != null ? plot : "");
        }

        // Badge "EM ALTA" sempre visível no banner
        if (bannerBadge != null) bannerBadge.setVisibility(View.VISIBLE);

        // Imagem
        String imageUrl = movie.getStreamIcon();
        if (imageUrl == null && movie.getInfo() != null) {
            imageUrl = movie.getInfo().getMovieImage();
        }

        Glide.with(this)
            .load(imageUrl)
            .transform(new CenterCrop())
            .placeholder(R.color.bg_surface)
            .error(R.color.bg_surface)
            .into(bannerImage);
    }

    private void updateDestaque(Series series) {
        if (series == null) return;

        destaqueTitle.setText(series.getName());

        // Subtítulo do lançamento
        StringBuilder sub = new StringBuilder();
        if (series.getInfo() != null) {
            if (series.getInfo().getReleaseDate() != null) {
                String year = series.getInfo().getReleaseDate();
                if (year.length() >= 4) year = year.substring(0, 4);
                sub.append(year);
            }
            // Subtítulo usando apenas o ano, já que run_time não está disponível

        }
        destaqueSubtitle.setText(sub.toString());

        String imageUrl = series.getCover();
        if (imageUrl == null && series.getInfo() != null) {
            imageUrl = series.getInfo().getCover();
        }

        Glide.with(this)
            .load(imageUrl)
            .transform(new CenterCrop())
            .placeholder(R.color.bg_surface)
            .error(R.color.bg_surface)
            .into(destaqueImage);
    }

    private void openMovieDetail(Movie movie) {
        String title = movie.getName() != null ? movie.getName() : movie.getTitle();
        String imageUrl = movie.getStreamIcon();
        if (imageUrl == null && movie.getInfo() != null) {
            imageUrl = movie.getInfo().getMovieImage();
        }
        String subtitle = movie.getInfo() != null && movie.getInfo().getReleaseDate() != null
                ? movie.getInfo().getReleaseDate() : "";
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
