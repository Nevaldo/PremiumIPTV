package com.player.iptv.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.gson.Gson;
import com.player.iptv.PlayerVodActivity;
import com.player.iptv.R;
import com.player.iptv.adapter.MovieFilterAdapter;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.data.TMDBClient;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.Movie;
import com.player.iptv.model.TmdbModels.TmdbMovieDetails;
import com.player.iptv.utils.TitleCleaner;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MovieDetailFragment extends Fragment {

    private static final String ARG_STREAM_ID = "stream_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_STREAM_URL = "stream_url";
    private static final String ARG_CONTAINER_EXT = "container_ext";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_SUBTITLE = "subtitle";
    private static final String ARG_CATEGORY_ID = "category_id";

    private CompositeDisposable disposables = new CompositeDisposable();

    private ImageView bgImage;
    private TextView txtGenres, txtTitle, txtYear, txtDuration, txtSynopsis;
    private Button btnAssistir;
    private LinearLayout btnVoltar;
    private RecyclerView rvMovieFilter;
    private MovieFilterAdapter movieFilterAdapter;

    private int streamId;
    private String title;
    private String streamUrl;
    private String containerExt;
    private String imageUrl;
    private String subtitle;
    private String categoryId;
    private String tmdbBackdropUrl;

    public static MovieDetailFragment newInstance(int streamId, String title, String streamUrl, String containerExt, String imageUrl, String subtitle, String categoryId) {
        MovieDetailFragment f = new MovieDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STREAM_ID, streamId);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_STREAM_URL, streamUrl);
        args.putString(ARG_CONTAINER_EXT, containerExt);
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_SUBTITLE, subtitle);
        args.putString(ARG_CATEGORY_ID, categoryId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            streamId = getArguments().getInt(ARG_STREAM_ID);
            title = getArguments().getString(ARG_TITLE);
            streamUrl = getArguments().getString(ARG_STREAM_URL);
            containerExt = getArguments().getString(ARG_CONTAINER_EXT);
            imageUrl = getArguments().getString(ARG_IMAGE_URL);
            subtitle = getArguments().getString(ARG_SUBTITLE);
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movie_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bgImage = view.findViewById(R.id.bgImage);
        txtGenres = view.findViewById(R.id.txtGenres);
        txtTitle = view.findViewById(R.id.txtTitle);
        txtYear = view.findViewById(R.id.txtYear);
        txtDuration = view.findViewById(R.id.txtDuration);
        txtSynopsis = view.findViewById(R.id.txtSynopsis);
        btnAssistir = view.findViewById(R.id.btnAssistir);
        btnVoltar = view.findViewById(R.id.btnVoltar);
        rvMovieFilter = view.findViewById(R.id.rvMovieFilter);

        txtTitle.setText(title != null ? title : "");

        btnAssistir.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), PlayerVodActivity.class);
            intent.putExtra(PlayerVodActivity.EXTRA_TITLE, title);
            intent.putExtra(PlayerVodActivity.EXTRA_STREAM_ID, streamId);
            intent.putExtra(PlayerVodActivity.EXTRA_CONTAINER_EXT, containerExt);
            intent.putExtra(PlayerVodActivity.EXTRA_IMAGE_URL, tmdbBackdropUrl != null ? tmdbBackdropUrl : imageUrl);
            intent.putExtra(PlayerVodActivity.EXTRA_SUBTITLE, subtitle);
            if (streamUrl != null && !streamUrl.isEmpty()) {
                intent.putExtra(PlayerVodActivity.EXTRA_STREAM_URL, streamUrl);
            }
            startActivity(intent);
        });

        loadRelatedMovies();
        searchTmdb();
    }

    private void loadRelatedMovies() {
        if (categoryId == null || categoryId.isEmpty()) return;

        movieFilterAdapter = new MovieFilterAdapter();
        movieFilterAdapter.setOnMovieClickListener(this::openMovie);
        rvMovieFilter.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMovieFilter.setAdapter(movieFilterAdapter);
        rvMovieFilter.setHasFixedSize(true);

        disposables.add(Single.fromCallable(() -> {
                    ContentCacheDao dao = AppDatabase.getInstance(requireContext()).contentCacheDao();
                    List<ContentCache> cached = dao.getByTypeAndCategory("vod_streams", categoryId);
                    List<Movie> movies = new ArrayList<>();
                    int index = 0;
                    int count = 0;
                    for (ContentCache item : cached) {
                        if (count >=10) break;
                        try {
                            JSONObject obj = new JSONObject(item.getJson());
                            Movie movie = new Gson().fromJson(obj.toString(), Movie.class);
                            if (movie.getStreamId() != streamId) {
                                if (index == 0 || index >= 9) {
                                    movies.add(movie);
                                    count++;
                                }
                                index++;
                            }
                        } catch (Exception ignored) {}
                    }
                    return movies;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movies -> movieFilterAdapter.submitList(movies), throwable -> {}));
    }

    private void openMovie(Movie movie) {
        String name = movie.getName() != null ? movie.getName() : movie.getTitle();
        String poster = movie.getStreamIcon();
        if (poster == null && movie.getInfo() != null) {
            poster = movie.getInfo().getMovieImage();
        }
        String sub = movie.getInfo() != null && movie.getInfo().getReleaseDate() != null ? movie.getInfo().getReleaseDate() : "";
        String source = movie.getDirectSource();

        MovieDetailFragment detail = MovieDetailFragment.newInstance(movie.getStreamId(), name, source, movie.getContainerExtension(), poster, sub, movie.getCategoryId());

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, detail)
                .addToBackStack(null)
                .commit();
    }

    private void searchTmdb() {
        if (title == null || title.isEmpty()) return;
        disposables.add(TMDBClient.getApi().searchMovie(TitleCleaner.clean(title), TMDBClient.API_KEY, TMDBClient.LANGUAGE)
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
        disposables.add(TMDBClient.getApi().getMovieDetails(tmdbId, TMDBClient.API_KEY, TMDBClient.LANGUAGE, "credits")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::bindTmdbData, throwable -> {}));
    }

    private void bindTmdbData(TmdbMovieDetails details) {
        if (details.genres != null && !details.genres.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < details.genres.size(); i++) {
                if (i > 0) sb.append(" • ");
                sb.append(details.genres.get(i).name.toUpperCase());
            }
            txtGenres.setText(sb.toString());
        }

        if (details.releaseDate != null && details.releaseDate.length() >= 4) {
            txtYear.setText(details.releaseDate.substring(0, 4));
        }

        if (details.runtime > 0) {
            int h = details.runtime / 60;
            int m = details.runtime % 60;
            txtDuration.setText(" " + h + "h " + m + "min ");
        }

        if (details.overview != null && !details.overview.isEmpty()) {
            txtSynopsis.setText(details.overview);
        }

        if (details.backdropPath != null) {
            tmdbBackdropUrl = "https://image.tmdb.org/t/p/w1280" + details.backdropPath;
            loadImage(tmdbBackdropUrl, R.color.bg_surface);
        } else {
            loadImage(imageUrl, R.color.bg_surface);
        }
    }

    private void loadImage(String url, int fallback) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .transform(new CenterCrop())
                    .placeholder(fallback)
                    .error(fallback)
                    .into(bgImage);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }
}
