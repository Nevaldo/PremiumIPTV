package com.player.iptv.view;

import android.content.Intent;
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
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.PlayerVodActivity;
import com.player.iptv.R;
import com.player.iptv.adapter.MovieAdapter;
import com.player.iptv.model.Movie;
import com.player.iptv.viewmodel.MoviesViewModel;

import java.util.ArrayList;
import java.util.List;

public class MoviesFragment extends Fragment {

    private MoviesViewModel viewModel;

    private RecyclerView rvFeatured;
    private RecyclerView rvNew;
    private RecyclerView rvContinue;
    private LinearLayout categoriesContainer;

    private MovieAdapter featuredAdapter;
    private MovieAdapter newAdapter;
    private MovieAdapter continueAdapter;

    public MoviesFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFeatured = view.findViewById(R.id.rvFeatured);
        rvNew      = view.findViewById(R.id.rvNew);
        rvContinue = view.findViewById(R.id.rvContinue);
        categoriesContainer = view.findViewById(R.id.categoriesContainer);

        setupAdapters();
        setupViewModel();
    }

    private void setupAdapters() {
        featuredAdapter = new MovieAdapter();
        newAdapter      = new MovieAdapter();
        continueAdapter = new MovieAdapter();

        rvFeatured.setAdapter(featuredAdapter);
        rvNew.setAdapter(newAdapter);
        rvContinue.setAdapter(continueAdapter);

        MovieAdapter.OnItemClickListener click = (movie, pos) -> openMovieDetail(movie);

        featuredAdapter.setOnItemClickListener(click);
        newAdapter.setOnItemClickListener(click);
        continueAdapter.setOnItemClickListener(click);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MoviesViewModel.class);

        viewModel.getFilteredMovies().observe(getViewLifecycleOwner(), this::distributeToAdapters);
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::buildCategoryChips);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.loadMovies();
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

    private void distributeToAdapters(List<Movie> list) {
        if (list == null || list.isEmpty()) {
            featuredAdapter.submitList(new ArrayList<>());
            newAdapter.submitList(new ArrayList<>());
            continueAdapter.submitList(new ArrayList<>());
            return;
        }

        int total     = list.size();
        int chunk     = Math.max(1, total / 3);
        int end1      = Math.min(chunk, total);
        int end2      = Math.min(chunk * 2, total);

        featuredAdapter.submitList(new ArrayList<>(list.subList(0, end1)));
        newAdapter.submitList(new ArrayList<>(list.subList(end1, end2)));
        continueAdapter.submitList(new ArrayList<>(list.subList(end2, total)));
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
