package com.player.iptv.model;

import com.google.gson.annotations.SerializedName;

public class SeriesEpisode {

    private String id;

    @SerializedName("episode_num")
    private int episodeNum;

    private String title;

    @SerializedName("container_extension")
    private String containerExtension;

    private String info;

    @SerializedName("custom_sid")
    private String customSid;

    private int added;
    private int season;

    @SerializedName("direct_source")
    private String directSource;

    public String getId() { return id; }
    public int getEpisodeNum() { return episodeNum; }
    public String getTitle() { return title; }
    public String getContainerExtension() { return containerExtension; }
    public String getInfo() { return info; }
    public String getCustomSid() { return customSid; }
    public int getAdded() { return added; }
    public int getSeason() { return season; }
    public String getDirectSource() { return directSource; }
}
