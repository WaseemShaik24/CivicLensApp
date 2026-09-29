package com.waseem.civiclens.models;

public class ClassificationResult {
    private String category;          // e.g. Pothole, Garbage, Streetlight, Water Leak, Traffic Blockade
    private String severity;          // LOW, MEDIUM, HIGH, CRITICAL
    private String summary;           // Clear citizen summary
    private String technicalDetails;  // Engineering/inspection details
    private String suggestedDepartment;// Municipal authority to route to
    private double confidenceScore;   // 0.0 - 1.0
    private String customModelClass;  // Fine-tuned dataset model detected class
    private double customModelConfidence;
    private double geminiConfidence;
    private String engineMode;        // HYBRID_ENSEMBLE

    public ClassificationResult() {
        this.engineMode = "HYBRID_ENSEMBLE";
    }

    public ClassificationResult(String category, String severity, String summary,
                                String technicalDetails, String suggestedDepartment, double confidenceScore) {
        this.category = category;
        this.severity = severity;
        this.summary = summary;
        this.technicalDetails = technicalDetails;
        this.suggestedDepartment = suggestedDepartment;
        this.confidenceScore = confidenceScore;
        this.engineMode = "HYBRID_ENSEMBLE";
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getTechnicalDetails() { return technicalDetails; }
    public void setTechnicalDetails(String technicalDetails) { this.technicalDetails = technicalDetails; }

    public String getSuggestedDepartment() { return suggestedDepartment; }
    public void setSuggestedDepartment(String suggestedDepartment) { this.suggestedDepartment = suggestedDepartment; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getCustomModelClass() { return customModelClass; }
    public void setCustomModelClass(String customModelClass) { this.customModelClass = customModelClass; }

    public double getCustomModelConfidence() { return customModelConfidence; }
    public void setCustomModelConfidence(double customModelConfidence) { this.customModelConfidence = customModelConfidence; }

    public double getGeminiConfidence() { return geminiConfidence; }
    public void setGeminiConfidence(double geminiConfidence) { this.geminiConfidence = geminiConfidence; }

    public String getEngineMode() { return engineMode; }
    public void setEngineMode(String engineMode) { this.engineMode = engineMode; }
}
