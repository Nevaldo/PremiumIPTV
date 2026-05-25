package com.player.iptv.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.player.iptv.R;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.ContentCacheDao;
import com.player.iptv.data.CredentialRepository;
import com.player.iptv.model.ContentCache;
import com.player.iptv.model.IptvCredential;
import com.player.iptv.network.IptvApiService;
import com.player.iptv.network.RetrofitClient;

import org.json.JSONArray;
import org.json.JSONObject;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class SyncFragment extends Fragment {

    public interface SyncListener {
        void onSyncCompleted();
        void onSyncError(String message);
    }

    private SyncListener listener;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TextView tvSyncStatus;
    private TextView tvSyncDetail;
    private TextView tvSyncError;
    private ProgressBar progressSync;

    public SyncFragment() {}

    public void setSyncListener(SyncListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSyncStatus = view.findViewById(R.id.tvSyncStatus);
        tvSyncDetail = view.findViewById(R.id.tvSyncDetail);
        tvSyncError = view.findViewById(R.id.tvSyncError);
        progressSync = view.findViewById(R.id.progressSync);

        startSync();
    }

    private void startSync() {
        CredentialRepository repo = new CredentialRepository(requireContext());

        disposables.add(repo.getActiveCredential()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::doSync, throwable -> {
                if (listener != null) listener.onSyncError("Erro: " + throwable.getMessage());
            }));
    }

    private void doSync(IptvCredential credential) {
        if (credential == null) {
            if (listener != null) listener.onSyncError("Nenhuma credencial encontrada");
            return;
        }

        disposables.add(
            runSyncSteps(credential)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result) {
                        if (listener != null) listener.onSyncCompleted();
                    } else {
                        if (listener != null) listener.onSyncError("Falha na sincronização");
                    }
                }, throwable -> {
                    if (listener != null) listener.onSyncError("Erro: " + throwable.getMessage());
                })
        );
    }

    private Single<Boolean> runSyncSteps(IptvCredential credential) {
        return Single.create(emitter -> {
            try {
                IptvApiService api = RetrofitClient.getInstance(credential.getServerUrl()).getApiService();
                AppDatabase db = AppDatabase.getInstance(requireContext());
                ContentCacheDao cacheDao = db.contentCacheDao();

                updateStatus("Autenticando...", "");
                Response<ResponseBody> auth = api.authenticate(credential.getUsername(), credential.getPassword())
                    .subscribeOn(Schedulers.io()).blockingGet();
                if (!auth.isSuccessful()) {
                    emitter.onSuccess(false);
                    return;
                }

                syncStep(api, cacheDao, credential, "get_live_categories", "live_categories", "Canais ao vivo - categorias...");
                syncStep(api, cacheDao, credential, "get_live_streams", "live_streams", "Canais ao vivo - streams...");
                syncStep(api, cacheDao, credential, "get_vod_categories", "vod_categories", "Filmes - categorias...");
                syncStep(api, cacheDao, credential, "get_vod_streams", "vod_streams", "Filmes...");
                syncStep(api, cacheDao, credential, "get_series_categories", "series_categories", "Séries - categorias...");
                syncStep(api, cacheDao, credential, "get_series", "series", "Séries...");

                updateStatus("Sincronização concluída!", "");
                emitter.onSuccess(true);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }

    private void syncStep(IptvApiService api, ContentCacheDao dao, IptvCredential credential,
                          String action, String contentType, String label) {
        try {
            updateStatus("Sincronizando...", label);

            Response<ResponseBody> response = api.getData(credential.getUsername(), credential.getPassword(), action).subscribeOn(Schedulers.io()).blockingGet();

            if (!response.isSuccessful() || response.body() == null) return;

            String json = response.body().string();
            JSONArray array;
            try {
                array = new JSONArray(json);
            } catch (Exception e) {
                JSONObject obj = new JSONObject(json);
                array = obj.has("data") ? obj.getJSONArray("data") : null;
            }

            if (array == null) return;

            dao.deleteByType(contentType);

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                String itemId = item.optString("stream_id", "");
                if (itemId.isEmpty()) itemId = item.optString("series_id", "");
                if (itemId.isEmpty()) itemId = item.optString("category_id", "");
                if (itemId.isEmpty()) itemId = String.valueOf(i);

                String categoryId = item.optString("category_id", "");
                dao.insert(new ContentCache(contentType, itemId, categoryId, item.toString()));
            }
        } catch (Exception ignored) {}
    }

    private void updateStatus(String status, String detail) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvSyncStatus.setText(status);
                if (!detail.isEmpty()) {
                    tvSyncDetail.setVisibility(View.VISIBLE);
                    tvSyncDetail.setText(detail);
                } else {
                    tvSyncDetail.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}
