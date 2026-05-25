package com.player.iptv.data;

import android.content.Context;
import android.content.SharedPreferences;

public class CredentialPreferences {

    private static final String PREF_NAME = "iptv_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences prefs;

    public CredentialPreferences(Context context) {
        prefs = context.getApplicationContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveCredentials(String serverUrl, String username, String password) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, "");
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "");
    }

    public long getLastSync() {
        return prefs.getLong(KEY_LAST_SYNC, 0);
    }

    public void setLastSync(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
