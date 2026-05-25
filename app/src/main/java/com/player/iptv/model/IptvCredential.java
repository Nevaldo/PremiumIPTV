package com.player.iptv.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "iptv_credentials")
public class IptvCredential {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String serverUrl;
    private String username;
    private String password;
    private boolean isActive;
    private long lastSync;

    public IptvCredential(String serverUrl, String username, String password) {
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        this.isActive = true;
        this.lastSync = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getLastSync() { return lastSync; }
    public void setLastSync(long lastSync) { this.lastSync = lastSync; }
}
