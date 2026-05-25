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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MoviesViewModel extends AndroidViewModel {

    private final ContentCacheDao cacheDao;
    private final Gson gson = new Gson();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<Movie>> allMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> filteredMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MoviesViewModel(Application application) {
        super(application);
        cacheDao = AppDatabase.getInstance(application).contentCacheDao();
    }

    public LiveData<List<Movie>> getFilteredMovies() { return filteredMovies; }
    public LiveData<List<String>> getCategories()     { return categories; }
    public LiveData<Boolean>      getIsLoading()      { return isLoading; }
    public LiveData<String>       getErrorMessage()   { return errorMessage; }

    public void loadMovies() {
        isLoading.setValue(true);

        disposables.add(
            io.reactivex.Single.<List<Movie>>create(emitter -> {
                List<ContentCache> caches = cacheDao.getByType("vod_streams");
                List<Movie> result = new ArrayList<>();
                for (ContentCache cache : caches) {
                    try {
                        Movie m = gson.fromJson(cache.getJson(), Movie.class);
                        if (m != null) result.add(m);
                    } catch (Exception ignored) {}
                }
                emitter.onSuccess(result);
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(movies -> {
                isLoading.setValue(false);
                allMovies.setValue(movies);
                filteredMovies.setValue(movies);
                loadCategories();
            }, e -> {
                isLoading.setValue(false);
                errorMessage.setValue("Erro ao carregar filmes: " + e.getMessage());
            })
        );
    }

    private void loadCategories() {
        disposables.add(
            io.reactivex.Single.<List<String>>create(emitter -> {
                List<ContentCache> caches = cacheDao.getByType("vod_categories");
                List<String> names = new ArrayList<>();
                names.add("Todas");
                for (ContentCache c : caches) {
                    try {
                        JSONObject obj = new JSONObject(c.getJson());
                        String name = obj.optString("category_name", "");
                        if (!name.isEmpty()) names.add(name);
                    } catch (Exception ignored) {}
                }
                emitter.onSuccess(names);
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(categories::setValue, e -> {
                List<String> def = new ArrayList<>();
                def.add("Todas");
                categories.setValue(def);
            })
        );
    }

    public void filterByCategory(String categoryName) {
        if (categoryName.isEmpty() || categoryName.equals("Todas")) {
            filteredMovies.setValue(allMovies.getValue());
            return;
        }

        disposables.add(
            io.reactivex.Single.<String>create(emitter -> {
                List<ContentCache> cats = cacheDao.getByType("vod_categories");
                String foundId = "";
                for (ContentCache c : cats) {
                    try {
                        JSONObject obj = new JSONObject(c.getJson());
                        if (categoryName.equalsIgnoreCase(obj.optString("category_name", ""))) {
                            foundId = obj.optString("category_id", "");
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                emitter.onSuccess(foundId);
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(categoryId -> {
                List<Movie> source = allMovies.getValue();
                if (source == null || categoryId.isEmpty()) {
                    filteredMovies.setValue(source);
                    return;
                }
                List<Movie> filtered = new ArrayList<>();
                for (Movie m : source) {
                    if (categoryId.equals(m.getCategoryId())) {
                        filtered.add(m);
                    }
                }
                filteredMovies.setValue(filtered);
            }, e -> filteredMovies.setValue(allMovies.getValue()))
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
