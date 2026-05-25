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
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.R;
import com.player.iptv.adapter.SeriesAdapter;
import com.player.iptv.model.Series;
import com.player.iptv.viewmodel.SeriesViewModel;

import java.util.ArrayList;
import java.util.List;

public class SeriesFragment extends Fragment {

    private SeriesViewModel viewModel;

    private RecyclerView rvFeatured;
    private RecyclerView rvNew;
    private RecyclerView rvContinue;
    private LinearLayout categoriesContainer;

    private SeriesAdapter featuredAdapter;
    private SeriesAdapter newAdapter;
    private SeriesAdapter continueAdapter;

    public SeriesFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_series, container, false);
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

    // ─── Adapters ─────────────────────────────────────────────────────────────

    private void setupAdapters() {
        featuredAdapter = new SeriesAdapter(R.layout.item_series_featured);
        newAdapter      = new SeriesAdapter(R.layout.item_series_new);
        continueAdapter = new SeriesAdapter(R.layout.item_series_continue);

        rvFeatured.setAdapter(featuredAdapter);
        rvNew.setAdapter(newAdapter);
        rvContinue.setAdapter(continueAdapter);

        SeriesAdapter.OnItemClickListener click = (series, pos) ->
            Toast.makeText(getContext(), series.getName(), Toast.LENGTH_SHORT).show();

        featuredAdapter.setOnItemClickListener(click);
        newAdapter.setOnItemClickListener(click);
        continueAdapter.setOnItemClickListener(click);
    }

    // ─── ViewModel + Observers ────────────────────────────────────────────────

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(SeriesViewModel.class);

        // Observe series list → distribute into the 3 carousels
        viewModel.getFilteredSeries().observe(getViewLifecycleOwner(), this::distributeToAdapters);

        // Observe categories → build the chips row
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::buildCategoryChips);

        // Observe errors
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        // Trigger load (only once — ViewModel survives config changes)
        viewModel.loadSeries();
    }

    // ─── Category chips ───────────────────────────────────────────────────────

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

        // Select first chip ("Todas") by default
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

    // ─── Distribute list into 3 carousels ─────────────────────────────────────

    private void distributeToAdapters(List<Series> list) {
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

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
