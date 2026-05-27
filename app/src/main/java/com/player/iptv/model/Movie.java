package com.player.iptv.model;

import com.google.gson.annotations.SerializedName;

public class Movie {

    private int num;
    private String name;
    private String title;

    @SerializedName("stream_id")
    private int streamId;

    @SerializedName("stream_icon")
    private String streamIcon;

    @SerializedName("stream_type")
    private String streamType;

    @SerializedName("added")
    private String added;

    @SerializedName("category_id")
    private String categoryId;

    @SerializedName("container_extension")
    private String containerExtension;

    private String rating;

    @SerializedName("rating_5based")
    private String rating5based;

    @SerializedName("custom_sid")
    private String customSid;

    @SerializedName("direct_source")
    private String directSource;

    private MovieInfo info;

    public static class MovieInfo {

        @SerializedName("movie_image")
        private String movieImage;

        @SerializedName("release_date")
        private String releaseDate;

        private String plot;
        private String genre;
        private String rating;

        @SerializedName("rating_5based")
        private String rating5Based;

        @SerializedName("duration_secs")
        private String durationSecs;

        private String director;
        private String cast;

        public String getMovieImage() { return movieImage; }
        public String getReleaseDate() { return releaseDate; }
        public String getPlot() { return plot; }
        public String getGenre() { return genre; }
        public String getRating() { return rating; }
        public String getDurationSecs() { return durationSecs; }
        public String getDirector() { return director; }
        public String getCast() { return cast; }
    }

    public int getNum() { return num; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public int getStreamId() { return streamId; }
    public String getStreamIcon() { return streamIcon; }
    public String getStreamType() { return streamType; }
    public String getCategoryId() { return categoryId; }
    public String getContainerExtension() { return containerExtension; }
    public String getRating() { return rating; }
    public String getRating5based() { return rating5based; }
    public MovieInfo getInfo() { return info; }
    public String getDirectSource() { return directSource; }
}
