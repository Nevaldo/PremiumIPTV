package com.player.iptv.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.LiveStream;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class LiveChannelViewModel extends AndroidViewModel {

    private final ContentCacheDao cacheDao;
    private final Gson gson = new Gson();
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<LiveStream>> allChannels = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<LiveStream>> filteredChannels = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveChannelViewModel(Application application) {
        super(application);
        cacheDao = AppDatabase.getInstance(application).contentCacheDao();
    }

    public LiveData<List<LiveStream>> getFilteredChannels() { return filteredChannels; }
    public LiveData<List<String>> getCategories()           { return categories; }
    public LiveData<Boolean>      getIsLoading()            { return isLoading; }
    public LiveData<String>       getErrorMessage()         { return errorMessage; }

    public void loadChannels() {
        isLoading.setValue(true);

        disposables.add(
            io.reactivex.Single.<List<LiveStream>>create(emitter -> {
                List<ContentCache> caches = cacheDao.getByType("live_streams");
                List<LiveStream> result = new ArrayList<>();
                for (ContentCache cache : caches) {
                    try {
                        LiveStream s = gson.fromJson(cache.getJson(), LiveStream.class);
                        if (s != null) result.add(s);
                    } catch (Exception ignored) {}
                }
                emitter.onSuccess(result);
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(channels -> {
                isLoading.setValue(false);
                allChannels.setValue(channels);
                filteredChannels.setValue(channels);
                loadCategories();
            }, e -> {
                isLoading.setValue(false);
                errorMessage.setValue("Erro ao carregar canais: " + e.getMessage());
            })
        );
    }

    private void loadCategories() {
        disposables.add(
            io.reactivex.Single.<List<String>>create(emitter -> {
                List<ContentCache> caches = cacheDao.getByType("live_categories");
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
            filteredChannels.setValue(allChannels.getValue());
            return;
        }

        disposables.add(
            io.reactivex.Single.<String>create(emitter -> {
                List<ContentCache> cats = cacheDao.getByType("live_categories");
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
                List<LiveStream> source = allChannels.getValue();
                if (source == null || categoryId.isEmpty()) {
                    filteredChannels.setValue(source);
                    return;
                }
                List<LiveStream> filtered = new ArrayList<>();
                for (LiveStream s : source) {
                    if (categoryId.equals(s.getCategoryId())) {
                        filtered.add(s);
                    }
                }
                filteredChannels.setValue(filtered);
            }, e -> filteredChannels.setValue(allChannels.getValue()))
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
