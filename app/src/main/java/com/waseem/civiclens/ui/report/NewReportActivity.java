package com.waseem.civiclens.ui.report;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseUser;

import com.waseem.civiclens.databinding.ActivityNewReportBinding;
import com.waseem.civiclens.models.CivicReport;
import com.waseem.civiclens.models.ClassificationResult;
import com.waseem.civiclens.network.GeminiVisionClient;
import com.waseem.civiclens.ui.location.LocationPickerActivity;
import com.waseem.civiclens.utils.FirebaseHelper;
import com.waseem.civiclens.utils.ImageUtils;
import com.waseem.civiclens.utils.LocationHelper;

public class NewReportActivity extends AppCompatActivity {

    private ActivityNewReportBinding binding;
    private GeminiVisionClient geminiVisionClient;
    private FirebaseHelper firebaseHelper;
    private FusedLocationProviderClient fusedLocationClient;

    private Bitmap capturedBitmap;
    private String imageBase64;
    private double currentLat = 13.0827;
    private double currentLng = 80.2707;
    private String currentAddress = "Anna Salai, Mount Road, T. Nagar, Chennai, Tamil Nadu, India";

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        geminiVisionClient = new GeminiVisionClient();
        firebaseHelper = new FirebaseHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupActivityLaunchers();
        fetchCurrentDeviceLocation();

        binding.btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndLaunch());
        binding.btnPickGallery.setOnClickListener(v -> launchGallery());
        binding.btnAnalyzeGemini.setOnClickListener(v -> analyzePhotoWithGemini());
        binding.btnChangeLocation.setOnClickListener(v -> launchLocationPicker());
        binding.btnSubmitReport.setOnClickListener(v -> submitComplaintReport());
    }

    private void setupActivityLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") instanceof Bitmap) {
                            capturedBitmap = (Bitmap) extras.get("data");
                            displaySelectedPhoto();
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            capturedBitmap = ImageUtils.getBitmapFromUri(this, uri);
                            displaySelectedPhoto();
                        }
                    }
                });

        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        currentLat = result.getData().getDoubleExtra("lat", currentLat);
                        currentLng = result.getData().getDoubleExtra("lng", currentLng);
                        currentAddress = result.getData().getStringExtra("address");
                        binding.tvSelectedAddress.setText(currentAddress);
                    }
                });
    }

    private void checkCameraPermissionAndLaunch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void displaySelectedPhoto() {
        if (capturedBitmap != null) {
            binding.ivReportPhoto.setImageBitmap(capturedBitmap);
            binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
            imageBase64 = ImageUtils.bitmapToBase64(capturedBitmap);
        }
    }

    private void analyzePhotoWithGemini() {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            Toast.makeText(this, "Please take or pick a photo evidence first!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.pbAnalyzing.setVisibility(View.VISIBLE);
        binding.tvAnalysisStatus.setVisibility(View.VISIBLE);
        binding.btnAnalyzeGemini.setEnabled(false);

        SharedPreferences prefs = getSharedPreferences("civiclens_prefs", Context.MODE_PRIVATE);
        String apiKey = prefs.getString("gemini_api_key", "");

        geminiVisionClient.classifyCivicIssue(imageBase64, apiKey, new GeminiVisionClient.ClassificationCallback() {
            @Override
            public void onSuccess(ClassificationResult result) {
                binding.pbAnalyzing.setVisibility(View.GONE);
                binding.tvAnalysisStatus.setVisibility(View.GONE);
                binding.btnAnalyzeGemini.setEnabled(true);

                binding.etCategory.setText(result.getCategory());
                binding.etSeverity.setText(result.getSeverity());
                binding.etReportTitle.setText(result.getCategory() + " Identified");
                binding.etDescription.setText(result.getSummary() + "\n" + result.getTechnicalDetails());
                binding.etDepartment.setText(result.getSuggestedDepartment());

                Toast.makeText(NewReportActivity.this, "AI Analysis Complete (" + Math.round(result.getConfidenceScore() * 100) + "% confidence)", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMsg) {
                binding.pbAnalyzing.setVisibility(View.GONE);
                binding.tvAnalysisStatus.setVisibility(View.GONE);
                binding.btnAnalyzeGemini.setEnabled(true);
                Toast.makeText(NewReportActivity.this, "Error running AI: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCurrentDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                LocationHelper.getAddressFromCoordinates(this, currentLat, currentLng, address -> {
                    runOnUiThread(() -> {
                        currentAddress = address;
                        binding.tvSelectedAddress.setText(address);
                    });
                });
            } else {
                binding.tvSelectedAddress.setText(currentAddress);
            }
        });
    }

    private void launchLocationPicker() {
        Intent intent = new Intent(this, LocationPickerActivity.class);
        intent.putExtra("lat", currentLat);
        intent.putExtra("lng", currentLng);
        locationPickerLauncher.launch(intent);
    }

    private void submitComplaintReport() {
        String category = binding.etCategory.getText() != null ? binding.etCategory.getText().toString().trim() : "";
        String severity = binding.etSeverity.getText() != null ? binding.etSeverity.getText().toString().trim() : "";
        String title = binding.etReportTitle.getText() != null ? binding.etReportTitle.getText().toString().trim() : "";
        String description = binding.etDescription.getText() != null ? binding.etDescription.getText().toString().trim() : "";
        String department = binding.etDepartment.getText() != null ? binding.etDepartment.getText().toString().trim() : "";

        if (TextUtils.isEmpty(title)) title = !TextUtils.isEmpty(category) ? category : "Civic Infrastructure Issue";
        if (TextUtils.isEmpty(category)) category = "General Civic Issue";
        if (TextUtils.isEmpty(severity)) severity = "MEDIUM";
        if (TextUtils.isEmpty(department)) department = "Department of Public Works";

        FirebaseUser user = firebaseHelper.getCurrentUser();
        String userId = user != null ? user.getUid() : "guest_citizen";
        String userEmail = user != null && user.getEmail() != null ? user.getEmail() : "citizen@civiclens.org";

        CivicReport report = CivicReport.createNew(
                userId, userEmail, category, severity, title, description,
                department, imageBase64, currentLat, currentLng, currentAddress
        );

        binding.btnSubmitReport.setEnabled(false);

        firebaseHelper.submitReport(report, new FirebaseHelper.ReportCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(NewReportActivity.this, "Complaint Successfully Filed!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(NewReportActivity.this, ComplaintDetailActivity.class);
                intent.putExtra("report_data", report);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                binding.btnSubmitReport.setEnabled(true);
                Toast.makeText(NewReportActivity.this, "Submission error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
