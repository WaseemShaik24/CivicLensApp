package com.waseem.civiclens.models;

import java.io.Serializable;
import java.util.UUID;

public class CivicReport implements Serializable {
    private String reportId;
    private String ticketNumber;      // e.g. CIVIC-2026-X892A
    private String userId;
    private String userEmail;
    private String title;
    private String issueCategory;     // Pothole, Waste, Streetlight, Water Leak, etc.
    private String severity;          // LOW, MEDIUM, HIGH, CRITICAL
    private String description;
    private String suggestedDepartment;
    private String imageBase64;
    private double latitude;
    private double longitude;
    private String address;
    private String status;            // FILED, UNDER_INVESTIGATION, ACTION_SCHEDULED, RESOLVED
    private long timestamp;

    public CivicReport() {}

    public static CivicReport createNew(String userId, String userEmail, String issueCategory,
                                        String severity, String title, String description,
                                        String suggestedDepartment, String imageBase64,
                                        double latitude, double longitude, String address) {
        CivicReport report = new CivicReport();
        report.reportId = UUID.randomUUID().toString();
        String uniqueHash = String.format("%05d", (int)(10000 + Math.random() * 90000));
        report.ticketNumber = "GCC-2026-CH-" + uniqueHash;
        report.userId = userId != null ? userId : "guest_user";
        report.userEmail = userEmail != null ? userEmail : "citizen@civiclens.org";
        report.issueCategory = issueCategory;
        report.severity = severity;
        report.title = title;
        report.description = description;
        report.suggestedDepartment = suggestedDepartment;
        report.imageBase64 = imageBase64;
        report.latitude = latitude;
        report.longitude = longitude;
        report.address = address != null ? address : "Coordinates: " + String.format("%.4f, %.4f", latitude, longitude);
        report.status = "FILED";
        report.timestamp = System.currentTimeMillis();
        return report;
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIssueCategory() { return issueCategory; }
    public void setIssueCategory(String issueCategory) { this.issueCategory = issueCategory; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSuggestedDepartment() { return suggestedDepartment; }
    public void setSuggestedDepartment(String suggestedDepartment) { this.suggestedDepartment = suggestedDepartment; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
