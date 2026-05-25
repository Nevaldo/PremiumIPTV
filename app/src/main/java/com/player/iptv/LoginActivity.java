package com.player.iptv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.player.iptv.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private EditText etServerUrl;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvError;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        etServerUrl = findViewById(R.id.etServerUrl);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvError = findViewById(R.id.tvLoginError);
        progressBar = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());

        viewModel.getIsLoading().observe(this, loading -> {
            progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
            btnLogin.setEnabled(!loading);
            btnLogin.setText(loading ? "CONECTANDO..." : "CONECTAR");
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvError.setText(error);
                tvError.setVisibility(android.view.View.VISIBLE);
            } else {
                tvError.setVisibility(android.view.View.GONE);
            }
        });

        viewModel.getLoginResult().observe(this, success -> {
            if (success != null && success) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("start_sync", true);
                startActivity(intent);
                finish();
            }
        });
    }

    private void attemptLogin() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tvError.setVisibility(android.view.View.GONE);
        viewModel.login(serverUrl, username, password);
    }
}
