package com.player.iptv.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.Series;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class SeriesViewModel extends AndroidViewModel {

    private final ContentCacheDao cacheDao;
    private final Gson gson = new Gson();
    private final CompositeDisposable disposables = new CompositeDisposable();

    // All series (unfiltered)
    private final MutableLiveData<List<Series>> allSeries = new MutableLiveData<>(new ArrayList<>());

    // Currently filtered/displayed series
    private final MutableLiveData<List<Series>> filteredSeries = new MutableLiveData<>(new ArrayList<>());

    // Category names list for the chips row
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>(new ArrayList<>());

    // Loading and error states
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public SeriesViewModel(Application application) {
        super(application);
        cacheDao = AppDatabase.getInstance(application).contentCacheDao();
    }

    // ─── Public LiveData accessors ────────────────────────────────────────────

    public LiveData<List<Series>> getFilteredSeries() { return filteredSeries; }
    public LiveData<List<String>> getCategories()     { return categories; }
    public LiveData<Boolean>      getIsLoading()      { return isLoading; }
    public LiveData<String>       getErrorMessage()   { return errorMessage; }

    // ─── Load all series from DB ──────────────────────────────────────────────

    public void loadSeries() {
        isLoading.setValue(true);

        disposables.add(
            io.reactivex.Single.<List<Series>>create(emitter -> {
                List<ContentCache> caches = cacheDao.getByType("series");
                List<Series> result = new ArrayList<>();
                for (ContentCache cache : caches) {
                    try {
                        Series s = gson.fromJson(cache.getJson(), Series.class);
                        if (s != null) result.add(s);
                    } catch (Exception ignored) {}
                }
                emitter.onSuccess(result);
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(series -> {
                isLoading.setValue(false);
                allSeries.setValue(series);
                filteredSeries.setValue(series); // show all by default
                loadCategories();
            }, e -> {
                isLoading.setValue(false);
                errorMessage.setValue("Erro ao carregar séries: " + e.getMessage());
            })
        );
    }

    // ─── Load categories from DB ──────────────────────────────────────────────

    private void loadCategories() {
        disposables.add(
            io.reactivex.Single.<List<String>>create(emitter -> {
                List<ContentCache> caches = cacheDao.getByType("series_categories");
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
                // fallback: expose default list
                List<String> def = new ArrayList<>();
                def.add("Todas");
                categories.setValue(def);
            })
        );
    }

    // ─── Filter by category name ──────────────────────────────────────────────

    public void filterByCategory(String categoryName) {
        if (categoryName.isEmpty() || categoryName.equals("Todas")) {
            filteredSeries.setValue(allSeries.getValue());
            return;
        }

        // Resolve category_id from DB then filter
        disposables.add(
            io.reactivex.Single.<String>create(emitter -> {
                List<ContentCache> cats = cacheDao.getByType("series_categories");
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
                List<Series> source = allSeries.getValue();
                if (source == null || categoryId.isEmpty()) {
                    filteredSeries.setValue(source);
                    return;
                }
                List<Series> filtered = new ArrayList<>();
                for (Series s : source) {
                    if (categoryId.equals(s.getCategoryId())) {
                        filtered.add(s);
                    }
                }
                filteredSeries.setValue(filtered);
            }, e -> filteredSeries.setValue(allSeries.getValue()))
        );
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
