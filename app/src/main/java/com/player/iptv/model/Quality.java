package com.player.iptv.model;

public class Quality {
    private String name;
    private String label;
    private String resolution;
    private boolean isRecommended;
    private boolean isSelected;

    public Quality(String name, String label, String resolution, boolean isRecommended) {
        this.name = name;
        this.label = label;
        this.resolution = resolution;
        this.isRecommended = isRecommended;
        this.isSelected = false;
    }

    // Getters e Setters
    public String getName() { return name; }
    public String getLabel() { return label; }
    public String getResolution() { return resolution; }
    public boolean isRecommended() { return isRecommended; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
