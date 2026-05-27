package com.player.iptv.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "historico")
public class Historico {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int streamId;
    private String titulo;
    private String info;
    private String subtitle;
    private String imageUrl;
    private String streamType;
    private String containerExt;
    private long position;
    private long duration;
    private long lastPlayedAt;

    public Historico(int streamId, String titulo, String info, String subtitle,
                     String imageUrl, String streamType, String containerExt,
                     long position, long duration, long lastPlayedAt) {
        this.streamId = streamId;
        this.titulo = titulo;
        this.info = info;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.streamType = streamType;
        this.containerExt = containerExt;
        this.position = position;
        this.duration = duration;
        this.lastPlayedAt = lastPlayedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStreamId() { return streamId; }
    public void setStreamId(int streamId) { this.streamId = streamId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStreamType() { return streamType; }
    public void setStreamType(String streamType) { this.streamType = streamType; }

    public String getContainerExt() { return containerExt; }
    public void setContainerExt(String containerExt) { this.containerExt = containerExt; }

    public long getPosition() { return position; }
    public void setPosition(long position) { this.position = position; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(long lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }
}
