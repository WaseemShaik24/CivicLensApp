package com.waseem.civiclens.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.waseem.civiclens.R;
import com.waseem.civiclens.databinding.ActivityMainBinding;
import com.waseem.civiclens.ui.report.NewReportActivity;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // Load default Feed fragment
        if (savedInstanceState == null) {
            loadFragment(new FeedFragment());
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_feed) {
                loadFragment(new FeedFragment());
                binding.fabNewReport.show();
                return true;
            } else if (itemId == R.id.nav_map) {
                loadFragment(new MapFragment());
                binding.fabNewReport.show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                binding.fabNewReport.hide();
                return true;
            }
            return false;
        });

        binding.fabNewReport.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewReportActivity.class));
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
