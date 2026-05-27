package com.player.iptv.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.player.iptv.R;
import com.player.iptv.adapter.SeriesFilterAdapter;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.data.TMDBClient;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.Series;
import com.player.iptv.model.TmdbModels.TmdbSeriesDetails;
import com.player.iptv.utils.TitleCleaner;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    private ImageView bgImage;
    private TextView txtGenres, txtTitle, txtYear, txtSeasons, txtEpisodes, txtSynopsis;
    private View btnAssistir;
    private LinearLayout btnVoltar;
    private RecyclerView rvSeriesFilter;
    private SeriesFilterAdapter seriesFilterAdapter;

    private int seriesId;
    private String name;
    private String cover;
    private String plot;
    private String genre;
    private String releaseDate;
    private String categoryId;

    public static SeriesDetailFragment newInstance(int seriesId, String name, String cover, String plot, String genre, String releaseDate, String categoryId) {
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

        bgImage = view.findViewById(R.id.bgImage);
        txtGenres = view.findViewById(R.id.txtGenres);
        txtTitle = view.findViewById(R.id.txtTitle);
        txtYear = view.findViewById(R.id.txtYear);
        txtSeasons = view.findViewById(R.id.txtSeasons);
        txtEpisodes = view.findViewById(R.id.txtEpisodes);
        txtSynopsis = view.findViewById(R.id.txtSynopsis);
        btnAssistir = view.findViewById(R.id.btnAssistirSeries);
        btnVoltar = view.findViewById(R.id.btnVoltar);
        rvSeriesFilter = view.findViewById(R.id.rvSeriesFilter);

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

        btnVoltar.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        btnAssistir.setOnClickListener(v -> {
        });

        loadRelatedSeries();
        searchTmdb();
    }

    private void loadRelatedSeries() {
        if (categoryId == null || categoryId.isEmpty()) return;

        seriesFilterAdapter = new SeriesFilterAdapter();
        seriesFilterAdapter.setOnSeriesClickListener(this::openSeries);
        rvSeriesFilter.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSeriesFilter.setAdapter(seriesFilterAdapter);
        rvSeriesFilter.setHasFixedSize(true);

        disposables.add(Single.fromCallable(() -> {
                    ContentCacheDao dao = AppDatabase.getInstance(requireContext()).contentCacheDao();
                    List<ContentCache> cached = dao.getByTypeAndCategory("series", categoryId);
                    List<Series> seriesList = new ArrayList<>();
                    int index = 0;
                    int count = 0;
                    for (ContentCache item : cached) {
                        if (count >= 10) break;
                        try {
                            JSONObject obj = new JSONObject(item.getJson());
                            Series s = new com.google.gson.Gson().fromJson(obj.toString(), Series.class);
                            if (s.getSeriesId() != seriesId) {
                                if (index == 0 || index >= 9) {
                                    seriesList.add(s);
                                    count++;
                                }
                                index++;
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
        if (sCover == null && s.getInfo() != null) {
            sCover = s.getInfo().getCover();
        }
        String sPlot = s.getPlot();
        if (sPlot == null && s.getInfo() != null) {
            sPlot = s.getInfo().getPlot();
        }
        String sGenre = s.getGenre();
        if (sGenre == null && s.getInfo() != null) {
            sGenre = s.getInfo().getGenre();
        }
        String sRelease = s.getReleaseDate();
        if (sRelease == null && s.getInfo() != null) {
            sRelease = s.getInfo().getReleaseDate();
        }

        SeriesDetailFragment detail = SeriesDetailFragment.newInstance(
                s.getSeriesId(), sName, sCover, sPlot, sGenre, sRelease, s.getCategoryId()
        );

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
                        int tmdbId = response.results.get(0).id;
                        fetchDetails(tmdbId);
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
        if (details.genres != null && !details.genres.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < details.genres.size(); i++) {
                if (i > 0) sb.append(" • ");
                sb.append(details.genres.get(i).name.toUpperCase());
            }
            txtGenres.setText(sb.toString());
        }

        if (details.firstAirDate != null && details.firstAirDate.length() >= 4) {
            if (details.lastAirDate != null && details.lastAirDate.length() >= 4) {
                txtYear.setText(details.firstAirDate.substring(0, 4) + " - " + details.lastAirDate.substring(0, 4));
            } else {
                txtYear.setText(details.firstAirDate.substring(0, 4));
            }
        }

        if (details.numberOfSeasons > 0) {
            txtSeasons.setText(" " + details.numberOfSeasons + (details.numberOfSeasons == 1 ? " Temporada " : " Temporadas "));
        }

        if (details.numberOfEpisodes > 0) {
            txtEpisodes.setText(" " + details.numberOfEpisodes + (details.numberOfEpisodes == 1 ? " Episódio " : " Episódios "));
        }

        if (details.overview != null && !details.overview.isEmpty()) {
            txtSynopsis.setText(details.overview);
        }

        if (details.backdropPath != null) {
            Glide.with(this)
                .load("https://image.tmdb.org/t/p/w1280" + details.backdropPath)
                .transform(new CenterCrop())
                .placeholder(R.color.bg_surface)
                .error(R.color.bg_surface)
                .into(bgImage);
        }else{
            Glide.with(this)
                .load(cover)
                .transform(new CenterCrop())
                .placeholder(R.color.bg_surface)
                .error(R.color.bg_surface)
                .into(bgImage);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
