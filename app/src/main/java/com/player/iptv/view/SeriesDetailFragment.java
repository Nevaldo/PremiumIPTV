package com.player.iptv.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.player.iptv.R;
import com.player.iptv.adapter.EpisodeAdapter;
import com.player.iptv.adapter.SeriesFilterAdapter;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.data.TMDBClient;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.Episode;
import com.player.iptv.model.Series;
import com.player.iptv.model.TmdbModels.TmdbSeriesDetails;
import com.player.iptv.utils.TitleCleaner;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SeriesDetailFragment extends Fragment {

    private static final String ARG_SERIES_ID = "series_id";
    private static final String ARG_NAME = "name";
    private static final String ARG_COVER = "cover";
    private static final String ARG_PLOT = "plot";
    private static final String ARG_GENRE = "genre";
    private static final String ARG_RELEASE_DATE = "release_date";
    private static final String ARG_CATEGORY_ID = "category_id";

    private CompositeDisposable disposables = new CompositeDisposable();

    // Views
    private ImageView bgImage;
    private TextView txtGenres, txtTitle, txtYear, txtSeasons, txtEpisodes, txtSynopsis;
    private TextView txtAssistirEp;
    private TextView tvSeasonSelected;
    private TextView tvInfoPremiere, tvInfoCreator, tvInfoCountry, tvInfoPlatform, tvInfoLanguage, tvInfoSubtitles;

    // Buttons
    private LinearLayout btnAssistirSeries, btnMinhaLista, seasonSelector;
    private FrameLayout btnLike, btnHeart, btnShare;

    // Tabs
    private TextView tabEpisodes, tabSeasons, tabDetails, tabCast, tabRecommendations;
    private View tabIndicator;

    // RecyclerViews
    private RecyclerView rvEpisodes, rvSeriesFilter;
    private EpisodeAdapter episodeAdapter;
    private SeriesFilterAdapter seriesFilterAdapter;

    // Data
    private int seriesId;
    private String name, cover, plot, genre, releaseDate, categoryId;
    private String currentBackdropUrl;

    public static SeriesDetailFragment newInstance(int seriesId, String name, String cover, String plot,
            String genre, String releaseDate, String categoryId) {
        SeriesDetailFragment f = new SeriesDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SERIES_ID, seriesId);
        args.putString(ARG_NAME, name);
        args.putString(ARG_COVER, cover);
        args.putString(ARG_PLOT, plot);
        args.putString(ARG_GENRE, genre);
        args.putString(ARG_RELEASE_DATE, releaseDate);
        args.putString(ARG_CATEGORY_ID, categoryId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            seriesId = getArguments().getInt(ARG_SERIES_ID);
            name = getArguments().getString(ARG_NAME);
            cover = getArguments().getString(ARG_COVER);
            plot = getArguments().getString(ARG_PLOT);
            genre = getArguments().getString(ARG_GENRE);
            releaseDate = getArguments().getString(ARG_RELEASE_DATE);
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_series_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        populateStaticData();
        setupListeners();
        setupEpisodesRecyclerView();
        setupRecommendationsRecyclerView();
        loadRelatedSeries();
        searchTmdb();
    }

    private void bindViews(View view) {
        bgImage = view.findViewById(R.id.bgImage);
        txtGenres = view.findViewById(R.id.txtGenres);
        txtTitle = view.findViewById(R.id.txtTitle);
        txtYear = view.findViewById(R.id.txtYear);
        txtSeasons = view.findViewById(R.id.txtSeasons);
        txtEpisodes = view.findViewById(R.id.txtEpisodes);
        txtSynopsis = view.findViewById(R.id.txtSynopsis);
        txtAssistirEp = view.findViewById(R.id.txtAssistirEp);
        tvSeasonSelected = view.findViewById(R.id.tvSeasonSelected);

        // Footer info
        tvInfoPremiere = view.findViewById(R.id.tvInfoPremiere);
        tvInfoCreator = view.findViewById(R.id.tvInfoCreator);
        tvInfoCountry = view.findViewById(R.id.tvInfoCountry);
        tvInfoPlatform = view.findViewById(R.id.tvInfoPlatform);
        tvInfoLanguage = view.findViewById(R.id.tvInfoLanguage);
        tvInfoSubtitles = view.findViewById(R.id.tvInfoSubtitles);

        // Action buttons
        btnAssistirSeries = view.findViewById(R.id.btnAssistirSeries);
        btnMinhaLista = view.findViewById(R.id.btnMinhaLista);
        seasonSelector = view.findViewById(R.id.seasonSelector);
        btnLike = view.findViewById(R.id.btnLike);
        btnHeart = view.findViewById(R.id.btnHeart);
        btnShare = view.findViewById(R.id.btnShare);

        // Tabs
        tabEpisodes = view.findViewById(R.id.tabEpisodes);
        tabSeasons = view.findViewById(R.id.tabSeasons);
        tabDetails = view.findViewById(R.id.tabDetails);
        tabCast = view.findViewById(R.id.tabCast);
        tabRecommendations = view.findViewById(R.id.tabRecommendations);
        tabIndicator = view.findViewById(R.id.tabIndicator);

        // RecyclerViews
        rvEpisodes = view.findViewById(R.id.rvEpisodes);
        rvSeriesFilter = view.findViewById(R.id.rvSeriesFilter);
    }

    private void populateStaticData() {
        txtTitle.setText(name != null ? name : "");

        if (genre != null && !genre.isEmpty()) {
            txtGenres.setText(genre.toUpperCase().replace(",", " •"));
        }

        if (releaseDate != null && releaseDate.length() >= 4) {
            txtYear.setText(releaseDate.substring(0, 4));
        }

        if (plot != null && !plot.isEmpty()) {
            txtSynopsis.setText(plot);
        }


    }

    private void setupListeners() {
        btnAssistirSeries.setOnClickListener(v -> showToast("Assistindo T1 • E1"));
        btnMinhaLista.setOnClickListener(v -> showToast("Adicionado à Minha Lista!"));
        btnLike.setOnClickListener(v -> showToast("Você curtiu esta série"));
        btnHeart.setOnClickListener(v -> showToast("Adicionado aos Favoritos"));
        btnShare.setOnClickListener(v -> showToast("Compartilhando..."));

        seasonSelector.setOnClickListener(v -> showToast("Seletor de temporada (Em breve)"));

        // Tab clicks
        View.OnClickListener tabClick = v -> {
            resetTabs();
            TextView tab = (TextView) v;
            tab.setTextColor(getResources().getColor(R.color.colorAccent, null));
            tab.setTypeface(null, android.graphics.Typeface.BOLD);
            int id = v.getId();
            if (id == R.id.tabSeasons) showToast("Aba Temporadas (Em breve)");
            else if (id == R.id.tabDetails) showToast("Aba Detalhes (Em breve)");
            else if (id == R.id.tabCast) showToast("Aba Elenco (Em breve)");
            else if (id == R.id.tabRecommendations) showToast("Aba Recomendações (Em breve)");
        };
        tabEpisodes.setOnClickListener(tabClick);
        tabSeasons.setOnClickListener(tabClick);
        tabDetails.setOnClickListener(tabClick);
        tabCast.setOnClickListener(tabClick);
        tabRecommendations.setOnClickListener(tabClick);
    }

    private void resetTabs() {
        int secondaryColor = getResources().getColor(R.color.text_secondary, null);
        for (TextView tab : Arrays.asList(tabEpisodes, tabSeasons, tabDetails, tabCast, tabRecommendations)) {
            tab.setTextColor(secondaryColor);
            tab.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setupEpisodesRecyclerView() {
        episodeAdapter = new EpisodeAdapter();
        episodeAdapter.setOnEpisodeClickListener(ep -> showToast("Assistindo: " + ep.getTitle()));

        rvEpisodes.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvEpisodes.setAdapter(episodeAdapter);
        rvEpisodes.setHasFixedSize(true);

        // Mock episodes for Season 1 (will be replaced when real API endpoint is added)
        List<Episode> mockEps = new ArrayList<>();
        mockEps.add(new Episode(1, "Winter Is Coming", "1h 02min", currentBackdropUrl, true));
        mockEps.add(new Episode(2, "The Kingsroad", "56min", null, false));
        mockEps.add(new Episode(3, "Lord Snow", "58min", null, false));
        mockEps.add(new Episode(4, "Cripples, Bastards and Broken Things", "55min", null, false));
        mockEps.add(new Episode(5, "The Wolf and the Lion", "55min", null, false));
        mockEps.add(new Episode(6, "A Golden Crown", "52min", null, false));
        mockEps.add(new Episode(7, "You Win or You Die", "58min", null, false));
        mockEps.add(new Episode(8, "The Pointy End", "54min", null, false));
        mockEps.add(new Episode(9, "Baelor", "57min", null, false));
        mockEps.add(new Episode(10, "Fire and Blood", "53min", null, false));
        episodeAdapter.setEpisodes(mockEps);
    }

    private void setupRecommendationsRecyclerView() {
        seriesFilterAdapter = new SeriesFilterAdapter();
        seriesFilterAdapter.setOnSeriesClickListener(this::openSeries);
        seriesFilterAdapter.setUseRowLayout(true);
        // Vertical layout for the right-side panel
        rvSeriesFilter.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        rvSeriesFilter.setAdapter(seriesFilterAdapter);
        rvSeriesFilter.setHasFixedSize(false);
    }

    private void loadRelatedSeries() {
        if (categoryId == null || categoryId.isEmpty()) return;

        disposables.add(Single.fromCallable(() -> {
                    ContentCacheDao dao = AppDatabase.getInstance(requireContext()).contentCacheDao();
                    List<ContentCache> cached = dao.getByTypeAndCategory("series", categoryId);
                    List<Series> seriesList = new ArrayList<>();
                    int count = 0;
                    for (ContentCache item : cached) {
                        if (count >= 5) break; // Only 5 recommendations in the vertical list
                        try {
                            JSONObject obj = new JSONObject(item.getJson());
                            Series s = new com.google.gson.Gson().fromJson(obj.toString(), Series.class);
                            if (s.getSeriesId() != seriesId) {
                                seriesList.add(s);
                                count++;
                            }
                        } catch (Exception ignored) {}
                    }
                    return seriesList;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> seriesFilterAdapter.submitList(list), throwable -> {}));
    }

    private void openSeries(Series s) {
        String sName = s.getName() != null ? s.getName() : "";
        String sCover = s.getCover();
        if (sCover == null && s.getInfo() != null) sCover = s.getInfo().getCover();
        String sPlot = s.getPlot();
        if (sPlot == null && s.getInfo() != null) sPlot = s.getInfo().getPlot();
        String sGenre = s.getGenre();
        if (sGenre == null && s.getInfo() != null) sGenre = s.getInfo().getGenre();
        String sRelease = s.getReleaseDate();
        if (sRelease == null && s.getInfo() != null) sRelease = s.getInfo().getReleaseDate();

        SeriesDetailFragment detail = SeriesDetailFragment.newInstance(
                s.getSeriesId(), sName, sCover, sPlot, sGenre, sRelease, s.getCategoryId());

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, detail)
                .addToBackStack(null)
                .commit();
    }

    private void searchTmdb() {
        if (name == null || name.isEmpty()) return;
        disposables.add(TMDBClient.getApi().searchSeries(TitleCleaner.clean(name), TMDBClient.API_KEY, TMDBClient.LANGUAGE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.results != null && !response.results.isEmpty()) {
                        fetchDetails(response.results.get(0).id);
                    }
                }, throwable -> {}));
    }

    private void fetchDetails(int tmdbId) {
        disposables.add(TMDBClient.getApi().getSeriesDetails(tmdbId, TMDBClient.API_KEY, TMDBClient.LANGUAGE, "credits")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::bindTmdbData, throwable -> {}));
    }

    private void bindTmdbData(TmdbSeriesDetails details) {
        // Genres
        if (details.genres != null && !details.genres.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < details.genres.size(); i++) {
                if (i > 0) sb.append(" • ");
                sb.append(details.genres.get(i).name.toUpperCase());
            }
            txtGenres.setText(sb.toString());
        }

        // Years
        if (details.firstAirDate != null && details.firstAirDate.length() >= 4) {
            String yearStr = details.firstAirDate.substring(0, 4);
            if (details.lastAirDate != null && details.lastAirDate.length() >= 4) {
                yearStr += " – " + details.lastAirDate.substring(0, 4);
            }
            txtYear.setText(yearStr);
        }

        // Seasons & Episodes
        if (details.numberOfSeasons > 0) {
            txtSeasons.setText(details.numberOfSeasons + (details.numberOfSeasons == 1 ? " Temporada" : " Temporadas"));
        }
        if (details.numberOfEpisodes > 0) {
            txtEpisodes.setText(details.numberOfEpisodes + (details.numberOfEpisodes == 1 ? " Episódio" : " Episódios"));
        }

        // Overview
        if (details.overview != null && !details.overview.isEmpty()) {
            txtSynopsis.setText(details.overview);
        }

        // Backdrop image
        if (details.backdropPath != null) {
            currentBackdropUrl = "https://image.tmdb.org/t/p/w1280" + details.backdropPath;
            Glide.with(this)
                    .load(currentBackdropUrl)
                    .transform(new CenterCrop())
                    .placeholder(R.color.bg_surface)
                    .error(R.color.bg_surface)
                    .into(bgImage);

            // Update first episode thumbnail once we have a backdrop
            updateFirstEpisodeThumbnail(currentBackdropUrl);
        }else{
            // Load cover as placeholder until TMDB responds
            if (cover != null && !cover.isEmpty()) {
                Glide.with(this)
                        .load(cover)
                        .transform(new CenterCrop())
                        .placeholder(R.color.bg_surface)
                        .error(R.color.bg_surface)
                        .into(bgImage);
            }
        }

        // Footer data
        if (details.firstAirDate != null && !details.firstAirDate.isEmpty()) {
            try {
                SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat outFmt = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
                Date d = inFmt.parse(details.firstAirDate);
                if (d != null) tvInfoPremiere.setText(outFmt.format(d));
            } catch (ParseException ignored) {
                tvInfoPremiere.setText(details.firstAirDate);
            }
        }

        // Season selector
        if (details.numberOfSeasons > 0) {
            tvSeasonSelected.setText("Temporada 1");
        }

        // Creators (from TMDB credits — will use static mock matching design)
        tvInfoCreator.setText("David Benioff,\nD. B. Weiss");
        tvInfoCountry.setText("Estados Unidos");
        tvInfoPlatform.setText("HBO");
        tvInfoLanguage.setText("Inglês");
        tvInfoSubtitles.setText("Português, Inglês,\nEspanhol +");
    }

    private void updateFirstEpisodeThumbnail(String url) {
        // Refresh the first episode thumbnail once backdrop is available
        if (episodeAdapter != null) {
            List<Episode> current = new ArrayList<>();
            current.add(new Episode(1, "Winter Is Coming", "1h 02min", url, true));
            current.add(new Episode(2, "The Kingsroad", "56min", null, false));
            current.add(new Episode(3, "Lord Snow", "58min", null, false));
            current.add(new Episode(4, "Cripples, Bastards and Broken Things", "55min", null, false));
            current.add(new Episode(5, "The Wolf and the Lion", "55min", null, false));
            current.add(new Episode(6, "A Golden Crown", "52min", null, false));
            current.add(new Episode(7, "You Win or You Die", "58min", null, false));
            current.add(new Episode(8, "The Pointy End", "54min", null, false));
            current.add(new Episode(9, "Baelor", "57min", null, false));
            current.add(new Episode(10, "Fire and Blood", "53min", null, false));
            episodeAdapter.setEpisodes(current);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
