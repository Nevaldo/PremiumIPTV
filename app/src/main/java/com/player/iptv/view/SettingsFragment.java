package com.player.iptv.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;

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

    private CredentialPreferences preferences;

    // View references
    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvMemberSince;
    
    private ProgressBar progressStorageCircular;
    private TextView tvStoragePercent;
    private TextView tvStorageText;
    private TextView tvCacheLegend;
    private TextView tvDownloadsLegend;
    private TextView tvOthersLegend;
    
    private TextView tvLastSync;
    private TextView tvCacheCurrent;
    
    // Buttons
    private LinearLayout btnEditProfile;
    private LinearLayout btnChangePassword;
    private LinearLayout btnManageStorage;
    private Button btnSync;
    private Button btnClearCache;
    private LinearLayout btnPrefPlay;
    private LinearLayout btnPrefNotif;
    private LinearLayout btnPrefTheme;
    private LinearLayout btnPrefParental;
    private LinearLayout btnLogout;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new CredentialPreferences(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Bind views
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);
        
        progressStorageCircular = view.findViewById(R.id.progressStorageCircular);
        tvStoragePercent = view.findViewById(R.id.tvStoragePercent);
        tvStorageText = view.findViewById(R.id.tvStorageText);
        tvCacheLegend = view.findViewById(R.id.tvCacheLegend);
        tvDownloadsLegend = view.findViewById(R.id.tvDownloadsLegend);
        tvOthersLegend = view.findViewById(R.id.tvOthersLegend);
        
        tvLastSync = view.findViewById(R.id.tvLastSync);
        tvCacheCurrent = view.findViewById(R.id.tvCacheCurrent);
        
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnManageStorage = view.findViewById(R.id.btnManageStorage);
        btnSync = view.findViewById(R.id.btnSync);
        btnClearCache = view.findViewById(R.id.btnClearCache);
        
        btnPrefPlay = view.findViewById(R.id.btnPrefPlay);
        btnPrefNotif = view.findViewById(R.id.btnPrefNotif);
        btnPrefTheme = view.findViewById(R.id.btnPrefTheme);
        btnPrefParental = view.findViewById(R.id.btnPrefParental);
        
        btnLogout = view.findViewById(R.id.btnLogout);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserInfo();
        loadStorageInfo();
        loadLastSync();

        // Bind listeners
        btnSync.setOnClickListener(v -> onSyncClick());
        btnClearCache.setOnClickListener(v -> onClearCacheClick());
        btnLogout.setOnClickListener(v -> onLogoutClick());
        
        // Mock buttons
        btnEditProfile.setOnClickListener(v -> showToast("Editar perfil (Em breve)"));
        btnChangePassword.setOnClickListener(v -> showToast("Alterar senha (Em breve)"));
        btnManageStorage.setOnClickListener(v -> showToast("Gerenciar armazenamento (Em breve)"));
        btnPrefPlay.setOnClickListener(v -> showToast("Preferências de Reprodução (Em breve)"));
        btnPrefNotif.setOnClickListener(v -> showToast("Preferências de Notificações (Em breve)"));
        btnPrefTheme.setOnClickListener(v -> showToast("Aparência e Temas (Em breve)"));
        btnPrefParental.setOnClickListener(v -> showToast("Controle dos Pais (Em breve)"));
    }
    
    private void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
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
        
        tvMemberSince.setText("Membro desde: 12/03/2024");
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

            // Formatação de apresentação (simulando proporções parecidas com a imagem)
            String cacheStr = formatBytes(cacheBytes);
            String totalStr = formatBytes(totalBytes);
            String usedStr = formatBytes(usedBytes);

            int progress = totalBytes > 0 ? (int) ((usedBytes * 100) / totalBytes) : 0;
            progress = Math.min(progress, 100);

            // Atualizar views circulares
            progressStorageCircular.setProgress(progress);
            tvStoragePercent.setText(progress + "%");
            
            // Textos descritivos
            tvStorageText.setText(usedStr + " usados de " + totalStr);
            tvCacheLegend.setText(cacheStr);
            tvDownloadsLegend.setText(formatBytes((long)(usedBytes * 0.3))); // Fake downloads portion
            tvOthersLegend.setText(formatBytes((long)(usedBytes * 0.1))); // Fake others portion
            
            // Right panel cache input
            tvCacheCurrent.setText(cacheStr);

        } catch (Exception ignored) {
        }
    }

    private void loadLastSync() {
        long lastSync = preferences.getLastSync();
        if (lastSync > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault());
            String date = sdf.format(new Date(lastSync));
            tvLastSync.setText("Última sincronização: " + date);
        } else {
            tvLastSync.setText("Nenhuma sincronização recente");
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
                R.drawable.ic_trash_outline,
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
                R.drawable.ic_logout_red,
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
