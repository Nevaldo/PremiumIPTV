package com.player.iptv.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.Movie;
import com.player.iptv.model.Series;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class HomeViewModel extends AndroidViewModel {

    private final ContentCacheDao cacheDao;
    private final Gson gson = new Gson();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<Movie> bannerMovie = new MutableLiveData<>();
    private final MutableLiveData<List<Movie>> featuredMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Series>> featuredSeries = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> topCategories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Series> destaqueSeries = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private static class HomeData {
        final List<Movie> movies;
        final List<Series> series;
        final List<String> categories;
        HomeData(List<Movie> movies, List<Series> series, List<String> categories) {
            this.movies = movies;
            this.series = series;
            this.categories = categories;
        }
    }

    public HomeViewModel(Application application) {
        super(application);
        cacheDao = AppDatabase.getInstance(application).contentCacheDao();
    }

    public LiveData<Movie> getBannerMovie() { return bannerMovie; }
    public LiveData<List<Movie>> getFeaturedMovies() { return featuredMovies; }
    public LiveData<List<Series>> getFeaturedSeries() { return featuredSeries; }
    public LiveData<List<String>> getTopCategories() { return topCategories; }
    public LiveData<Series> getDestaqueSeries() { return destaqueSeries; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadHomeData() {
        disposables.add(
            io.reactivex.Single.zip(
                loadMovies(),
                loadSeries(),
                loadCategories(),
                HomeData::new
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(data -> {
                bannerMovie.setValue(data.movies.isEmpty() ? null : data.movies.get(0));
                featuredMovies.setValue(data.movies.size() > 10 ? data.movies.subList(0, 10) : data.movies);
                featuredSeries.setValue(data.series.size() > 10 ? data.series.subList(0, 10) : data.series);
                topCategories.setValue(data.categories);
                destaqueSeries.setValue(data.series.isEmpty() ? null : data.series.get(0));
            }, e -> {
                errorMessage.setValue("Erro ao carregar home: " + e.getMessage());
            })
        );
    }

    private io.reactivex.Single<List<Movie>> loadMovies() {
        return io.reactivex.Single.<List<Movie>>create(emitter -> {
            List<ContentCache> caches = cacheDao.getByType("vod_streams");
            List<Movie> result = new ArrayList<>();
            for (ContentCache cache : caches) {
                try {
                    Movie m = gson.fromJson(cache.getJson(), Movie.class);
                    if (m != null) result.add(m);
                } catch (Exception ignored) {}
            }
            emitter.onSuccess(result);
        });
    }

    private io.reactivex.Single<List<Series>> loadSeries() {
        return io.reactivex.Single.<List<Series>>create(emitter -> {
            List<ContentCache> caches = cacheDao.getByType("series");
            List<Series> result = new ArrayList<>();
            for (ContentCache cache : caches) {
                try {
                    Series s = gson.fromJson(cache.getJson(), Series.class);
                    if (s != null) result.add(s);
                } catch (Exception ignored) {}
            }
            emitter.onSuccess(result);
        });
    }

    private io.reactivex.Single<List<String>> loadCategories() {
        return io.reactivex.Single.<List<String>>create(emitter -> {
            List<ContentCache> caches = cacheDao.getByType("live_categories");
            List<String> names = new ArrayList<>();
            for (ContentCache c : caches) {
                try {
                    JSONObject obj = new JSONObject(c.getJson());
                    String name = obj.optString("category_name", "");
                    if (!name.isEmpty()) names.add(name);
                } catch (Exception ignored) {}
            }
            if (names.size() > 5) names = names.subList(0, 5);
            emitter.onSuccess(names);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
