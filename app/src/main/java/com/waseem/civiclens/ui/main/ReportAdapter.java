package com.waseem.civiclens.ui.main;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.waseem.civiclens.R;
import com.waseem.civiclens.databinding.ItemCivicReportBinding;
import com.waseem.civiclens.models.CivicReport;
import com.waseem.civiclens.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<CivicReport> reports = new ArrayList<>();
    private final OnReportClickListener clickListener;

    public interface OnReportClickListener {
        void onReportClick(CivicReport report);
    }

    public ReportAdapter(OnReportClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setReports(List<CivicReport> newReports) {
        this.reports = newReports != null ? newReports : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCivicReportBinding binding = ItemCivicReportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reports.get(position));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ItemCivicReportBinding binding;

        public ReportViewHolder(ItemCivicReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CivicReport report) {
            binding.tvTitle.setText(report.getTitle());
            binding.tvCategory.setText(report.getIssueCategory());
            binding.tvAddress.setText(report.getAddress());
            binding.tvTicketNumber.setText("TICKET #" + report.getTicketNumber());

            // Severity Badge Color
            String severity = report.getSeverity() != null ? report.getSeverity().toUpperCase() : "MEDIUM";
            binding.tvSeverityBadge.setText(severity + " SEVERITY");
            if (severity.contains("CRITICAL")) {
                binding.tvSeverityBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.severity_critical));
            } else if (severity.contains("HIGH")) {
                binding.tvSeverityBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.severity_high));
            } else if (severity.contains("LOW")) {
                binding.tvSeverityBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.severity_low));
            } else {
                binding.tvSeverityBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.severity_medium));
            }

            // Status Badge
            String status = report.getStatus() != null ? report.getStatus() : "FILED";
            binding.tvStatusBadge.setText("STATUS: " + status);

            // Time Ago
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    report.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            );
            binding.tvDate.setText(timeAgo);

            // Image Preview
            if (report.getImageBase64() != null && !report.getImageBase64().isEmpty()) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(report.getImageBase64());
                if (bitmap != null) {
                    binding.ivReportImage.setImageBitmap(bitmap);
                } else {
                    binding.ivReportImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                binding.ivReportImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onReportClick(report);
            });
        }
    }
}
