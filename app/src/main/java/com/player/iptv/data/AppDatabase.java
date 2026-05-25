package com.player.iptv.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.player.iptv.model.ContentCache;
import com.player.iptv.model.IptvCredential;

@Database(entities = {IptvCredential.class, ContentCache.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract IptvCredentialDao iptvCredentialDao();
    public abstract ContentCacheDao contentCacheDao();

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
