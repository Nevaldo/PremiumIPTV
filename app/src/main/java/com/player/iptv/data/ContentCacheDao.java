package com.player.iptv.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.player.iptv.model.ContentCache;

import java.util.List;

@Dao
public interface ContentCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ContentCache item);

    @Query("SELECT * FROM content_cache WHERE content_type = :contentType ORDER BY item_id ASC")
    List<ContentCache> getByType(String contentType);

    @Query("SELECT * FROM content_cache WHERE content_type = :contentType AND category_id = :categoryId ORDER BY item_id ASC")
    List<ContentCache> getByTypeAndCategory(String contentType, String categoryId);

    @Query("SELECT * FROM content_cache WHERE content_type = :contentType AND item_id = :itemId LIMIT 1")
    ContentCache getByTypeAndId(String contentType, String itemId);

    @Query("DELETE FROM content_cache WHERE content_type = :contentType")
    void deleteByType(String contentType);

    @Query("DELETE FROM content_cache")
    void deleteAll();
}
