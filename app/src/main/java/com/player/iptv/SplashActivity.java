package com.player.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.player.iptv.data.CredentialPreferences;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(this::navigate, SPLASH_DELAY_MS);
    }

    private void navigate() {
        CredentialPreferences prefs = new CredentialPreferences(this);
        Intent intent;
        if (prefs.isLoggedIn()) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
