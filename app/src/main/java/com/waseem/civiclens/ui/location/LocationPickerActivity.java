package com.waseem.civiclens.ui.location;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.waseem.civiclens.databinding.ActivityLocationPickerBinding;
import com.waseem.civiclens.utils.LocationHelper;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityLocationPickerBinding binding;
    private GoogleMap googleMap;
    private double currentLat = 13.0827;
    private double currentLng = 80.2707;
    private String currentAddress = "Anna Salai, Mount Road, T. Nagar, Chennai, Tamil Nadu, India";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().hasExtra("lat")) {
            currentLat = getIntent().getDoubleExtra("lat", 13.0827);
            currentLng = getIntent().getDoubleExtra("lng", 80.2707);
        }

        binding.mapViewPicker.onCreate(savedInstanceState);
        binding.mapViewPicker.getMapAsync(this);

        binding.btnConfirmLocation.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("lat", currentLat);
            data.putExtra("lng", currentLng);
            data.putExtra("address", currentAddress);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng initialPos = new LatLng(currentLat, currentLng);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPos, 16f));

        googleMap.setOnCameraIdleListener(() -> {
            LatLng center = googleMap.getCameraPosition().target;
            currentLat = center.latitude;
            currentLng = center.longitude;

            LocationHelper.getAddressFromCoordinates(this, currentLat, currentLng, address -> {
                runOnUiThread(() -> {
                    currentAddress = address;
                    if (binding != null) binding.tvPickerAddress.setText(address);
                });
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.mapViewPicker.onResume();
    }

    @Override
    protected void onPause() {
        binding.mapViewPicker.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        binding.mapViewPicker.onDestroy();
        super.onDestroy();
    }
}
