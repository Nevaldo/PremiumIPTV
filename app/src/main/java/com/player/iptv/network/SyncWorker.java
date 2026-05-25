package com.player.iptv.network;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.data.CredentialPreferences;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.IptvCredential;

import org.json.JSONArray;
import org.json.JSONObject;

import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        CredentialPreferences prefs = new CredentialPreferences(context);

        if (!prefs.isLoggedIn()) {
            return Result.success();
        }

        String baseUrl = prefs.getServerUrl();
        String username = prefs.getUsername();
        String password = prefs.getPassword();

        try {
            RetrofitClient client = RetrofitClient.getInstance(baseUrl);
            IptvApiService api = client.getApiService();

            Response<ResponseBody> authResponse = api.authenticate(username, password)
                .subscribeOn(Schedulers.io())
                .blockingGet();

            if (!authResponse.isSuccessful()) {
                return Result.retry();
            }

            prefs.setLastSync(System.currentTimeMillis());

            IptvCredential credential = new IptvCredential(baseUrl, username, password);
            credential.setLastSync(System.currentTimeMillis());

            AppDatabase db = AppDatabase.getInstance(context);
            db.iptvCredentialDao().deactivateAll();
            db.iptvCredentialDao().insert(credential);

            ContentCacheDao cacheDao = db.contentCacheDao();

            cacheData(api, cacheDao, username, password, "get_live_categories", "live_categories");
            cacheData(api, cacheDao, username, password, "get_live_streams", "live_streams");
            cacheData(api, cacheDao, username, password, "get_vod_categories", "vod_categories");
            cacheData(api, cacheDao, username, password, "get_vod_streams", "vod_streams");
            cacheData(api, cacheDao, username, password, "get_series_categories", "series_categories");
            cacheData(api, cacheDao, username, password, "get_series", "series");

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private void cacheData(IptvApiService api, ContentCacheDao dao,
                           String username, String password,
                           String action, String contentType) {
        try {
            Response<ResponseBody> response = api.getData(username, password, action)
                .subscribeOn(Schedulers.io())
                .blockingGet();

            if (!response.isSuccessful() || response.body() == null) return;

            String json = response.body().string();

            JSONArray array;
            try {
                array = new JSONArray(json);
            } catch (Exception e) {
                JSONObject obj = new JSONObject(json);
                if (obj.has("data")) {
                    array = obj.getJSONArray("data");
                } else {
                    return;
                }
            }

            dao.deleteByType(contentType);

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String itemId = item.optString("stream_id", "");
                if (itemId.isEmpty()) itemId = item.optString("series_id", "");
                if (itemId.isEmpty()) itemId = item.optString("category_id", "");
                if (itemId.isEmpty()) itemId = String.valueOf(i);

                String categoryId = item.optString("category_id", "");
                String itemJson = item.toString();

                dao.insert(new ContentCache(contentType, itemId, categoryId, itemJson));
            }
        } catch (Exception ignored) {}
    }
}
