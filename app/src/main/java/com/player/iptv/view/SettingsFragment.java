package com.player.iptv.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.player.iptv.LoginActivity;
import com.player.iptv.MainActivity;
import com.player.iptv.R;
import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.CredentialPreferences;
import com.player.iptv.utils.DialogUtils;
import com.player.iptv.utils.MediaCacheManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class SettingsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private CredentialPreferences preferences;
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvMemberSince;
    private TextView tvStorageText;
    private TextView tvCacheSize;
    private TextView tvDownloadsSize;
    private TextView tvLastSync;
    private ProgressBar progressStorage;
    private Button btnSync;
    private Button btnClearCache;
    private Button btnLogout;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        preferences = new CredentialPreferences(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);
        tvStorageText = view.findViewById(R.id.tvStorageText);
        tvCacheSize = view.findViewById(R.id.tvCacheSize);
        tvDownloadsSize = view.findViewById(R.id.tvDownloadsSize);
        tvLastSync = view.findViewById(R.id.tvLastSync);
        progressStorage = view.findViewById(R.id.progressStorage);
        btnSync = view.findViewById(R.id.btnSync);
        btnClearCache = view.findViewById(R.id.btnClearCache);
        btnLogout = view.findViewById(R.id.btnLogout);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserInfo();
        loadStorageInfo();
        loadLastSync();

        btnSync.setOnClickListener(v -> onSyncClick());
        btnClearCache.setOnClickListener(v -> onClearCacheClick());
        btnLogout.setOnClickListener(v -> onLogoutClick());
    }

    private void loadUserInfo() {
        String username = preferences.getUsername();
        String serverUrl = preferences.getServerUrl();

        if (!username.isEmpty()) {
            tvUserName.setText(username);
        }

        if (!serverUrl.isEmpty()) {
            String email = serverUrl.replaceFirst("^(https?://)", "").replaceAll("/.*", "");
            tvUserEmail.setText(email);
        }
    }

    private void loadStorageInfo() {
        try {
            File cacheDir = new File(requireContext().getCacheDir(), "media_cache");
            long cacheBytes = 0;
            if (cacheDir.exists()) {
                cacheBytes = getFolderSize(cacheDir);
            }

            long totalBytes = Runtime.getRuntime().totalMemory();
            long usedBytes = totalBytes - Runtime.getRuntime().freeMemory();

            String cacheSize = formatBytes(cacheBytes);
            String totalStorage = formatBytes(totalBytes);
            String usedStorage = formatBytes(usedBytes);

            tvCacheSize.setText("Cache: " + cacheSize);
            tvDownloadsSize.setText("Memória usada: " + usedStorage);
            tvStorageText.setText(usedStorage + " usados de " + totalStorage);

            int progress = totalBytes > 0 ? (int) ((usedBytes * 100) / totalBytes) : 0;
            progressStorage.setProgress(Math.min(progress, 100));
        } catch (Exception ignored) {
        }
    }

    private void loadLastSync() {
        long lastSync = preferences.getLastSync();
        if (lastSync > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            String date = sdf.format(new Date(lastSync));
            tvLastSync.setText("Última sincronização: " + date);
        }
    }

    private void onSyncClick() {
        SyncFragment syncFragment = new SyncFragment();
        syncFragment.setSyncListener(new SyncFragment.SyncListener() {
            @Override
            public void onSyncCompleted() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadLastSync();
                        Toast.makeText(getContext(), "Sincronização concluída!", Toast.LENGTH_SHORT).show();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragmentContainer, new SettingsFragment())
                                    .commit();
                        }
                    });
                }
            }

            @Override
            public void onSyncError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Erro: " + message, Toast.LENGTH_LONG).show()
                    );
                }
            }
        });

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, syncFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void onClearCacheClick() {
        DialogUtils.showPremiumDialog(requireContext(),
                R.drawable.ic_settings,
                "Limpar Cache",
                "Isso irá remover todos os arquivos temporários e dados em cache. Deseja continuar?",
                "LIMPAR",
                "CANCELAR",
                () -> {
                    clearCache();
                    Toast.makeText(getContext(), "Cache limpo com sucesso!", Toast.LENGTH_SHORT).show();
                    loadStorageInfo();
                }
        );
    }

    @OptIn(markerClass = UnstableApi.class)
    private void clearCache() {
        try {
            MediaCacheManager cacheManager = MediaCacheManager.getInstance(requireContext());
            cacheManager.release();

            File cacheDir = new File(requireContext().getCacheDir(), "media_cache");
            if (cacheDir.exists()) {
                deleteDirectory(cacheDir);
            }

            AppDatabase.getInstance(requireContext()).contentCacheDao().deleteAll();
        } catch (Exception ignored) {
        }
    }

    private void onLogoutClick() {
        DialogUtils.showPremiumDialog(requireContext(),
                R.drawable.ic_settings,
                "Sair da Conta",
                "Tem certeza que deseja sair da sua conta? Você precisará fazer login novamente.",
                "SAIR",
                "CANCELAR",
                () -> {
                    preferences.clear();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
        );
    }

    private long getFolderSize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += getFolderSize(file);
                }
            }
        } else {
            size = dir.length();
        }
        return size;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
}
