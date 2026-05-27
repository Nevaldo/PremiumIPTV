package com.player.iptv.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.player.iptv.model.Historico;

import java.util.List;

@Dao
public interface HistoricoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Historico historico);

    @Query("SELECT * FROM historico WHERE streamId = :streamId AND streamType = :streamType LIMIT 1")
    Historico getByStream(int streamId, String streamType);

    @Query("SELECT * FROM historico WHERE position > 0 AND position < duration - 60000 ORDER BY lastPlayedAt DESC")
    LiveData<List<Historico>> getContinueWatching();

    @Query("SELECT * FROM historico ORDER BY lastPlayedAt DESC")
    LiveData<List<Historico>> getAll();

    @Query("DELETE FROM historico WHERE id NOT IN (SELECT id FROM historico ORDER BY lastPlayedAt DESC LIMIT 50)")
    void trimTo50();

    @Query("DELETE FROM historico WHERE streamId = :streamId AND streamType = :streamType")
    void delete(int streamId, String streamType);

    @Query("DELETE FROM historico")
    void deleteAll();
}
