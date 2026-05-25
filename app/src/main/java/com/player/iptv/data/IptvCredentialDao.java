package com.player.iptv.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.player.iptv.model.IptvCredential;

import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface IptvCredentialDao {

    @Query("SELECT * FROM iptv_credentials WHERE isActive = 1 LIMIT 1")
    Single<IptvCredential> getActiveCredential();

    @Query("SELECT * FROM iptv_credentials WHERE isActive = 1 LIMIT 1")
    Flowable<IptvCredential> observeActiveCredential();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(IptvCredential credential);

    @Query("UPDATE iptv_credentials SET isActive = 0")
    void deactivateAll();

    @Query("UPDATE iptv_credentials SET lastSync = :timestamp WHERE isActive = 1")
    void updateLastSync(long timestamp);

    @Query("DELETE FROM iptv_credentials")
    void deleteAll();
}
