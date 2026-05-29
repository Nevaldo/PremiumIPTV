package com.player.iptv.model;

public class Channel {
    private int id;
    private String name;
    private String logo;
    private String url;
    private String category;
    private boolean isLive;
    private int viewers;
    private String quality;

    public Channel(int id, String name, String logo, String url, String category) {
        this.id = id;
        this.name = name;
        this.logo = logo;
        this.url = url;
        this.category = category;
        this.isLive = true;
        this.quality = "HD";
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isLive() { return isLive; }
    public void setLive(boolean live) { isLive = live; }

    public int getViewers() { return viewers; }
    public void setViewers(int viewers) { this.viewers = viewers; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
}