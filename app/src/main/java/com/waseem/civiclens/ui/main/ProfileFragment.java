package com.waseem.civiclens.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseUser;
import com.waseem.civiclens.databinding.FragmentProfileBinding;
import com.waseem.civiclens.ui.auth.LoginActivity;
import com.waseem.civiclens.utils.FirebaseHelper;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseHelper firebaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseHelper = new FirebaseHelper();
        FirebaseUser user = firebaseHelper.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            binding.tvUserEmail.setText(user.getEmail());
            binding.tvAccountRole.setText("Verified Firebase User");
        } else {
            binding.tvUserEmail.setText("Citizen Guest");
            binding.tvAccountRole.setText("Guest Mode");
        }

        // Load saved Gemini API Key if any
        SharedPreferences prefs = requireContext().getSharedPreferences("civiclens_prefs", Context.MODE_PRIVATE);
        String apiKey = prefs.getString("gemini_api_key", "");
        if (!TextUtils.isEmpty(apiKey)) {
            binding.etApiKey.setText(apiKey);
        }

        binding.btnSaveApiKey.setOnClickListener(v -> {
            String newKey = binding.etApiKey.getText() != null ? binding.etApiKey.getText().toString().trim() : "";
            prefs.edit().putString("gemini_api_key", newKey).apply();
            Toast.makeText(requireContext(), "Gemini API Key saved!", Toast.LENGTH_SHORT).show();
        });

        binding.btnSignOut.setOnClickListener(v -> {
            firebaseHelper.signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
