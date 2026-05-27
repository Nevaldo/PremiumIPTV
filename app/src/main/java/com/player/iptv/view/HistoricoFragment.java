package com.player.iptv.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.PlayerVodActivity;
import com.player.iptv.R;
import com.player.iptv.adapter.AssistidosRecentementeAdapter;
import com.player.iptv.adapter.ContinuarAssistindoAdapter;
import com.player.iptv.data.CredentialPreferences;
import com.player.iptv.model.Historico;
import com.player.iptv.viewmodel.HistoricoViewModel;

public class HistoricoFragment extends Fragment {

    private RecyclerView rvContinuar;
    private RecyclerView rvRecentes;
    private ContinuarAssistindoAdapter continuarAdapter;
    private AssistidosRecentementeAdapter recentesAdapter;
    private HistoricoViewModel viewModel;
    private CredentialPreferences credentialPrefs;

    private Button btnFiltroTudo;
    private Button btnFiltroFilmes;
    private Button btnFiltroSeries;
    private Button btnCarregarMais;

    public HistoricoFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historico, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        credentialPrefs = new CredentialPreferences(requireContext());
        viewModel = new ViewModelProvider(this).get(HistoricoViewModel.class);

        rvContinuar = view.findViewById(R.id.rvContinuar);
        rvRecentes = view.findViewById(R.id.rvRecentes);
        btnFiltroTudo = view.findViewById(R.id.btnFiltroTudo);
        btnFiltroFilmes = view.findViewById(R.id.btnFiltroFilmes);
        btnFiltroSeries = view.findViewById(R.id.btnFiltroSeries);
        btnCarregarMais = view.findViewById(R.id.btnCarregarMais);

        rvContinuar.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        continuarAdapter = new ContinuarAssistindoAdapter(ContinuarAssistindoAdapter.TYPE_CARD);
        rvContinuar.setAdapter(continuarAdapter);

        rvRecentes.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recentesAdapter = new AssistidosRecentementeAdapter();
        rvRecentes.setAdapter(recentesAdapter);


        viewModel.getContinueWatching().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) return;
            int end = Math.min(list.size(), 20);
            continuarAdapter.setItems(list.subList(0, end));
        });
        viewModel.getAllHistorico().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) return;
            int end = Math.min(list.size(), 6);
            recentesAdapter.setItems(list.subList(0, end));
        });

        continuarAdapter.setOnItemClickListener((item, pos) -> openPlayer(item));
        recentesAdapter.setOnItemClickListener((item, pos) -> openPlayer(item));

        btnFiltroTudo.setOnClickListener(v -> selecionarFiltro(btnFiltroTudo, ""));
        btnFiltroFilmes.setOnClickListener(v -> selecionarFiltro(btnFiltroFilmes, "movie"));
        btnFiltroSeries.setOnClickListener(v -> selecionarFiltro(btnFiltroSeries, "series"));

        btnCarregarMais.setOnClickListener(v -> deletarTudo());
    }

    private void openPlayer(Historico item) {
        Intent intent = new Intent(getContext(), PlayerVodActivity.class);
        intent.putExtra(PlayerVodActivity.EXTRA_TITLE, item.getTitulo());
        intent.putExtra(PlayerVodActivity.EXTRA_STREAM_ID, item.getStreamId());
        intent.putExtra(PlayerVodActivity.EXTRA_CONTAINER_EXT, item.getContainerExt());
        intent.putExtra(PlayerVodActivity.EXTRA_SUBTITLE, item.getInfo());
        intent.putExtra(PlayerVodActivity.EXTRA_IMAGE_URL, item.getImageUrl());

        if ("live".equals(item.getStreamType())) {
            String server = credentialPrefs.getServerUrl();
            String user = credentialPrefs.getUsername();
            String pass = credentialPrefs.getPassword();
            if (server != null && user != null && pass != null) {
                String url = server + "live/" + user + "/" + pass + "/" + item.getStreamId() + ".m3u8";
                intent.putExtra(PlayerVodActivity.EXTRA_STREAM_URL, url);
            }
        }

        startActivity(intent);
    }

    private void selecionarFiltro(Button selected, String type) {
        btnFiltroTudo.setBackgroundResource(selected == btnFiltroTudo ? R.drawable.bg_filter_selected : R.drawable.bg_filter);
        btnFiltroTudo.setTextColor(getColor(selected == btnFiltroTudo ? R.color.white : R.color.text_secondary));

        btnFiltroFilmes.setBackgroundResource(selected == btnFiltroFilmes ? R.drawable.bg_filter_selected : R.drawable.bg_filter);
        btnFiltroFilmes.setTextColor(getColor(selected == btnFiltroFilmes ? R.color.white : R.color.text_secondary));

        btnFiltroSeries.setBackgroundResource(selected == btnFiltroSeries ? R.drawable.bg_filter_selected : R.drawable.bg_filter);
        btnFiltroSeries.setTextColor(getColor(selected == btnFiltroSeries ? R.color.white : R.color.text_secondary));

        viewModel.setFilter(type);
    }

    private void deletarTudo() {
        viewModel.deleteAll();
    }

    private int getColor(int resId) {
        return requireContext().getColor(resId);
    }
}