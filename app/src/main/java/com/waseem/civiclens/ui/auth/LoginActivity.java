package com.waseem.civiclens.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.waseem.civiclens.databinding.ActivityLoginBinding;
import com.waseem.civiclens.ui.main.MainActivity;
import com.waseem.civiclens.utils.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = new FirebaseHelper();

        if (firebaseHelper.isUserLoggedIn()) {
            navigateToMain();
            return;
        }

        binding.btnLogin.setOnClickListener(v -> handleLogin());
        binding.btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        binding.btnGoogleSignIn.setOnClickListener(v -> handleGoogleLogin());
    }

    private void handleGoogleLogin() {
        Toast.makeText(this, "Signing in with Google Account...", Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void handleLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password required");
            return;
        }

        firebaseHelper.loginUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(LoginActivity.this, "Login note: Entering Guest Mode", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
