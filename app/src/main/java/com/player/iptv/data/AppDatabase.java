package com.player.iptv.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.player.iptv.model.ContentCache;
import com.player.iptv.model.Historico;
import com.player.iptv.model.IptvCredential;

@Database(entities = {IptvCredential.class, ContentCache.class, Historico.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract IptvCredentialDao iptvCredentialDao();
    public abstract ContentCacheDao contentCacheDao();
    public abstract HistoricoDao historicoDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "iptv_db"
                    ).fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }
}
