package com.player.iptv.model;

import com.google.gson.annotations.SerializedName;

public class LiveStream {

    private int num;
    private String name;

    @SerializedName("stream_type")
    private String streamType;

    @SerializedName("stream_id")
    private int streamId;

    @SerializedName("stream_icon")
    private String streamIcon;

    @SerializedName("epg_channel_id")
    private String epgChannelId;

    @SerializedName("added")
    private String added;

    @SerializedName("category_id")
    private String categoryId;

    @SerializedName("custom_sid")
    private String customSid;

    @SerializedName("tv_archive")
    private int tvArchive;

    @SerializedName("direct_source")
    private String directSource;

    @SerializedName("tv_archive_duration")
    private int tvArchiveDuration;

    public int getNum() { return num; }
    public String getName() { return name; }
    public String getStreamType() { return streamType; }
    public int getStreamId() { return streamId; }
    public String getStreamIcon() { return streamIcon; }
    public String getEpgChannelId() { return epgChannelId; }
    public String getAdded() { return added; }
    public String getCategoryId() { return categoryId; }
    public String getCustomSid() { return customSid; }
    public int getTvArchive() { return tvArchive; }
    public String getDirectSource() { return directSource; }
    public int getTvArchiveDuration() { return tvArchiveDuration; }
}
