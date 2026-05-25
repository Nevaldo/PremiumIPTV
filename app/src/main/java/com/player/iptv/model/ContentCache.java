package com.player.iptv.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "content_cache")
public class ContentCache {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "content_type")
    private String contentType;

    @ColumnInfo(name = "item_id")
    private String itemId;

    @ColumnInfo(name = "category_id")
    private String categoryId;

    private String json;

    @ColumnInfo(name = "last_updated")
    private long lastUpdated;

    public ContentCache(String contentType, String itemId, String categoryId, String json) {
        this.contentType = contentType;
        this.itemId = itemId;
        this.categoryId = categoryId;
        this.json = json;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getJson() { return json; }
    public void setJson(String json) { this.json = json; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
