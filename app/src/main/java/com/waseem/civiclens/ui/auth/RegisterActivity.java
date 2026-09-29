package com.waseem.civiclens.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.waseem.civiclens.databinding.ActivityRegisterBinding;
import com.waseem.civiclens.ui.main.MainActivity;
import com.waseem.civiclens.utils.FirebaseHelper;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseHelper = new FirebaseHelper();

        binding.btnSubmitRegister.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        String email = binding.etRegEmail.getText() != null ? binding.etRegEmail.getText().toString().trim() : "";
        String password = binding.etRegPassword.getText() != null ? binding.etRegPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.etRegEmail.setError("Email required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etRegPassword.setError("Minimum 6 characters password required");
            return;
        }

        firebaseHelper.registerUser(email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(RegisterActivity.class != null ? RegisterActivity.this : null, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(RegisterActivity.this, "Registration: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
