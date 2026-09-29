package com.waseem.civiclens.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.waseem.civiclens.databinding.FragmentMapBinding;
import com.waseem.civiclens.models.CivicReport;
import com.waseem.civiclens.ui.report.ComplaintDetailActivity;
import com.waseem.civiclens.utils.FirebaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentMapBinding binding;
    private GoogleMap googleMap;
    private FirebaseHelper firebaseHelper;
    private final Map<String, CivicReport> markerReportMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firebaseHelper = new FirebaseHelper();

        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        googleMap.setOnInfoWindowClickListener(marker -> {
            CivicReport report = markerReportMap.get(marker.getId());
            if (report != null) {
                Intent intent = new Intent(requireContext(), ComplaintDetailActivity.class);
                intent.putExtra("report_data", report);
                startActivity(intent);
            }
        });

        loadMapMarkers();
    }

    private void loadMapMarkers() {
        firebaseHelper.fetchReports(new FirebaseHelper.ReportListCallback() {
            @Override
            public void onSuccess(List<CivicReport> reports) {
                if (googleMap == null || reports == null || reports.isEmpty()) return;

                googleMap.clear();
                markerReportMap.clear();

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                boolean hasValidBounds = false;

                for (CivicReport report : reports) {
                    if (report.getLatitude() != 0.0 || report.getLongitude() != 0.0) {
                        LatLng pos = new LatLng(report.getLatitude(), report.getLongitude());

                        float hue = BitmapDescriptorFactory.HUE_RED;
                        String severity = report.getSeverity() != null ? report.getSeverity().toUpperCase() : "";
                        if (severity.contains("CRITICAL")) hue = BitmapDescriptorFactory.HUE_RED;
                        else if (severity.contains("HIGH")) hue = BitmapDescriptorFactory.HUE_ORANGE;
                        else if (severity.contains("MEDIUM")) hue = BitmapDescriptorFactory.HUE_YELLOW;
                        else if (severity.contains("LOW")) hue = BitmapDescriptorFactory.HUE_GREEN;

                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(report.getTitle())
                                .snippet(report.getIssueCategory() + " • " + report.getSeverity())
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        );

                        if (marker != null) {
                            markerReportMap.put(marker.getId(), report);
                        }

                        builder.include(pos);
                        hasValidBounds = true;
                    }
                }

                if (hasValidBounds) {
                    try {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
                    } catch (Exception e) {
                        // Fallback center
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7749, -122.4194), 12f));
                    }
                } else {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7749, -122.4194), 12f));
                }

                if (binding != null) {
                    binding.tvMapStats.setText("Showing " + reports.size() + " Active Geotagged Issues");
                }
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.mapView.onResume();
    }

    @Override
    public void onPause() {
        if (binding != null) binding.mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (binding != null) binding.mapView.onDestroy();
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.mapView.onLowMemory();
    }
}
