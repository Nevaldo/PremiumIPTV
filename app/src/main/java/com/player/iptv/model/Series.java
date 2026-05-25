package com.player.iptv.model;

import com.google.gson.annotations.SerializedName;

public class Series {

    private int num;
    private String name;

    @SerializedName("series_id")
    private int seriesId;

    private String cover;
    private String plot;
    private String genre;
    private String rating;

    @SerializedName("releaseDate")
    private String releaseDate;

    @SerializedName("last_modified")
    private String lastModified;

    @SerializedName("category_id")
    private String categoryId;

    private SeriesInfo info;

    public static class SeriesInfo {
        private String name;
        private String cover;
        private String plot;

        @SerializedName("release_date")
        private String releaseDate;

        private String rating;

        @SerializedName("rating_5based")
        private String rating5Based;

        private String genre;
        private String cast;
        private String director;

        public String getName() { return name; }
        public String getCover() { return cover; }
        public String getPlot() { return plot; }
        public String getReleaseDate() { return releaseDate; }
        public String getRating() { return rating; }
        public String getGenre() { return genre; }
        public String getCast() { return cast; }
        public String getDirector() { return director; }
    }

    public int getNum() { return num; }
    public String getName() { return name; }
    public int getSeriesId() { return seriesId; }
    public String getCover() { return cover; }
    public String getPlot() { return plot; }
    public String getGenre() { return genre; }
    public String getRating() { return rating; }
    public String getReleaseDate() { return releaseDate; }
    public String getCategoryId() { return categoryId; }
    public SeriesInfo getInfo() { return info; }

    public void setName(String name) { this.name = name; }
    public void setCover(String cover) { this.cover = cover; }
}
