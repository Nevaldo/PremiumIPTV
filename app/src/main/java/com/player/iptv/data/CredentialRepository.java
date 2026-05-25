package com.player.iptv.data;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.player.iptv.model.IptvCredential;
import com.player.iptv.network.RetrofitClient;
import com.player.iptv.network.SyncWorker;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class CredentialRepository {

    private static final long SYNC_INTERVAL_HOURS = 5;
    private static final String SYNC_WORK_NAME = "iptv_sync";

    private final AppDatabase database;
    private final CredentialPreferences preferences;
    private final Context context;

    public CredentialRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.preferences = new CredentialPreferences(context);
    }

    public Single<IptvCredential> getActiveCredential() {
        return database.iptvCredentialDao().getActiveCredential()
            .subscribeOn(Schedulers.io());
    }

    public Flowable<IptvCredential> observeActiveCredential() {
        return database.iptvCredentialDao().observeActiveCredential()
            .subscribeOn(Schedulers.io());
    }

    public Single<Boolean> login(String serverUrl, String username, String password) {
        String baseUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";

        return RetrofitClient.getInstance(baseUrl)
            .getApiService()
            .authenticate(username, password)
            .subscribeOn(Schedulers.io())
            .map(response -> {
                if (response.isSuccessful()) {
                    database.iptvCredentialDao().deactivateAll();

                    IptvCredential credential = new IptvCredential(baseUrl, username, password);
                    credential.setLastSync(System.currentTimeMillis());
                    database.iptvCredentialDao().insert(credential);

                    preferences.saveCredentials(baseUrl, username, password);
                    preferences.setLastSync(System.currentTimeMillis());

                    return true;
                }
                return false;
            });
    }

    public boolean isLoggedIn() {
        return preferences.isLoggedIn();
    }

    public void logout() {
        preferences.clear();
    }

    public void scheduleSync() {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
            SyncWorker.class,
            SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
        .setConstraints(constraints)
        .build();

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            );
    }
}
