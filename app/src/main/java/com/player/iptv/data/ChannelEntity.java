package com.player.iptv.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "channels")
public class ChannelEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String url;
    private String logo;
    private int number;  // Número do canal (ex: 101)
    private String category;  // Esporte, Filmes, Notícias, etc.
    private boolean isFavorite;
    private int zappingPriority;  // Prioridade para pré-carregamento
    private long lastWatched;

    public ChannelEntity(String name, String url, int number, String category) {
        this.name = name;
        this.url = url;
        this.number = number;
        this.category = category;
        this.isFavorite = false;
        this.zappingPriority = 0;
        this.lastWatched = System.currentTimeMillis();
    }

    // Getters e Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public int getZappingPriority() { return zappingPriority; }
    public void setZappingPriority(int zappingPriority) { this.zappingPriority = zappingPriority; }

    public long getLastWatched() { return lastWatched; }
    public void setLastWatched(long lastWatched) { this.lastWatched = lastWatched; }
}