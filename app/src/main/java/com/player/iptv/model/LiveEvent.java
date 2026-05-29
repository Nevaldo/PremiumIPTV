package com.player.iptv.model;
public class LiveEvent {
    private String title;
    private String subtitle;
    private String time;
    private String date;
    private String stadium;
    private String score;
    private boolean isLive;

    public LiveEvent(String title, String subtitle, String time, String date, String stadium) {
        this.title = title;
        this.subtitle = subtitle;
        this.time = time;
        this.date = date;
        this.stadium = stadium;
        this.isLive = false;
    }

    // Getters e Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStadium() { return stadium; }
    public void setStadium(String stadium) { this.stadium = stadium; }

    public String getScore() { return score; }
    public void setScore(String score) { this.score = score; }

    public boolean isLive() { return isLive; }
    public void setLive(boolean live) { isLive = live; }
}