package com.waseem.civiclens.ui.report;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.waseem.civiclens.R;
import com.waseem.civiclens.databinding.ActivityComplaintDetailBinding;
import com.waseem.civiclens.models.CivicReport;
import com.waseem.civiclens.utils.ImageUtils;
import com.waseem.civiclens.utils.ReportPdfGenerator;

import java.util.Date;

public class ComplaintDetailActivity extends AppCompatActivity {

    private ActivityComplaintDetailBinding binding;
    private CivicReport report;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityComplaintDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().hasExtra("report_data")) {
            report = (CivicReport) getIntent().getSerializableExtra("report_data");
        }

        if (report == null) {
            Toast.makeText(this, "Unable to load complaint report details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindReportDetails();

        binding.btnShareReport.setOnClickListener(v -> shareComplaintDetails());
        binding.btnExportPdf.setOnClickListener(v -> ReportPdfGenerator.generateAndOpenPdf(this, report));
    }

    private void bindReportDetails() {
        binding.tvTicketId.setText("TICKET #" + report.getTicketNumber());
        binding.tvDetailTitle.setText(report.getTitle());
        binding.tvDepartmentTag.setText("Routed to: " + (report.getSuggestedDepartment() != null ? report.getSuggestedDepartment() : "Public Works"));

        binding.tvDetailStatus.setText("STATUS: " + (report.getStatus() != null ? report.getStatus() : "FILED"));

        String dateStr = DateFormat.format("MMMM dd, yyyy - hh:mm a", new Date(report.getTimestamp())).toString();
        binding.tvDetailDate.setText(dateStr);

        binding.tvDetailCategory.setText("Category: " + report.getIssueCategory());
        binding.tvDetailSeverity.setText("Severity: " + report.getSeverity());
        binding.tvDetailDescription.setText(report.getDescription());
        binding.tvDetailAddress.setText(report.getAddress());

        String severity = report.getSeverity() != null ? report.getSeverity().toUpperCase() : "MEDIUM";
        if (severity.contains("CRITICAL")) {
            binding.tvDetailSeverity.setTextColor(ContextCompat.getColor(this, R.color.severity_critical));
        } else if (severity.contains("HIGH")) {
            binding.tvDetailSeverity.setTextColor(ContextCompat.getColor(this, R.color.severity_high));
        } else if (severity.contains("LOW")) {
            binding.tvDetailSeverity.setTextColor(ContextCompat.getColor(this, R.color.severity_low));
        } else {
            binding.tvDetailSeverity.setTextColor(ContextCompat.getColor(this, R.color.severity_medium));
        }

        if (report.getImageBase64() != null && !report.getImageBase64().isEmpty()) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(report.getImageBase64());
            if (bitmap != null) {
                binding.ivDetailPhoto.setImageBitmap(bitmap);
            }
        }
    }

    private void shareComplaintDetails() {
        String shareBody = "CivicLens Official Complaint Docket\n" +
                "Ticket #: " + report.getTicketNumber() + "\n" +
                "Issue: " + report.getTitle() + "\n" +
                "Category: " + report.getIssueCategory() + " (" + report.getSeverity() + " Severity)\n" +
                "Location: " + report.getAddress() + "\n" +
                "Status: " + report.getStatus() + "\n" +
                "Department: " + report.getSuggestedDepartment();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Civic Issue Complaint - " + report.getTicketNumber());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share Complaint Docket via"));
    }
}
