package com.player.iptv.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.player.iptv.data.AppDatabase;
import com.player.iptv.data.HistoricoDao;
import com.player.iptv.model.Historico;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

public class HistoricoViewModel extends AndroidViewModel {

    private final HistoricoDao dao;
    private final LiveData<List<Historico>> continueWatching;
    private final LiveData<List<Historico>> allHistorico;
    private final MediatorLiveData<List<Historico>> filteredHistorico = new MediatorLiveData<>();
    private String currentFilter = "";

    public HistoricoViewModel(@NonNull Application application) {
        super(application);
        dao = AppDatabase.getInstance(application).historicoDao();
        continueWatching = dao.getContinueWatching();
        allHistorico = dao.getAll();

        filteredHistorico.addSource(allHistorico, list -> applyFilter(list));
    }

    private void applyFilter(List<Historico> list) {
        if (currentFilter.isEmpty()) {
            filteredHistorico.setValue(list);
            return;
        }
        List<Historico> filtered = new ArrayList<>();
        for (Historico h : list) {
            if (currentFilter.equals(h.getStreamType())) {
                filtered.add(h);
            }
        }
        filteredHistorico.setValue(filtered);
    }

    public LiveData<List<Historico>> getContinueWatching() {
        return continueWatching;
    }

    public LiveData<List<Historico>> getAllHistorico() {
        return filteredHistorico;
    }

    public void setFilter(String type) {
        currentFilter = type;
        List<Historico> current = allHistorico.getValue();
        if (current != null) {
            applyFilter(current);
        }
    }

    public void delete(Historico historico) {
        Completable.fromAction(() ->
                dao.delete(historico.getStreamId(), historico.getStreamType())
        ).subscribeOn(Schedulers.io()).subscribe();
    }

    public void deleteAll() {
        Completable.fromAction(() -> dao.deleteAll())
                .subscribeOn(Schedulers.io()).subscribe();
    }
}
