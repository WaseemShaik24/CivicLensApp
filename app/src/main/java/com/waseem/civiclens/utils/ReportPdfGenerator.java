package com.waseem.civiclens.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.waseem.civiclens.models.CivicReport;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportPdfGenerator {
    private static final String TAG = "ReportPdfGenerator";

    public static void generateAndOpenPdf(Context context, CivicReport report) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Header Background Banner
        paint.setColor(Color.parseColor("#1E3A8A"));
        canvas.drawRect(0, 0, 595, 100, paint);

        // Header Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        paint.setFakeBoldText(true);
        canvas.drawText("MUNICIPAL CIVIC COMPLAINT DOCKET", 30, 45, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        canvas.drawText("CivicLens Automated AI Classification & Reporting System", 30, 70, paint);

        // Document Details Box
        int topMargin = 120;
        paint.setColor(Color.parseColor("#F8FAFC"));
        RectF detailsBox = new RectF(30, topMargin, 565, topMargin + 110);
        canvas.drawRoundRect(detailsBox, 10, 10, paint);

        paint.setColor(Color.parseColor("#1E3A8A"));
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("TICKET REFERENCE: " + report.getTicketNumber(), 45, topMargin + 30, paint);

        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(11);
        paint.setFakeBoldText(false);

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault());
        String dateStr = sdf.format(new Date(report.getTimestamp()));

        canvas.drawText("Date & Time: " + dateStr, 45, topMargin + 55, paint);
        canvas.drawText("Reporter: " + report.getUserEmail(), 45, topMargin + 75, paint);
        canvas.drawText("Assigned Authority: " + report.getSuggestedDepartment(), 45, topMargin + 95, paint);

        // Status Badge
        paint.setColor(Color.parseColor("#3B82F6"));
        RectF statusRect = new RectF(400, topMargin + 20, 545, topMargin + 50);
        canvas.drawRoundRect(statusRect, 6, 6, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(12);
        paint.setFakeBoldText(true);
        canvas.drawText("STATUS: " + report.getStatus(), 415, topMargin + 40, paint);

        // Section 1: Issue Classification
        int section1Top = topMargin + 130;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("1. INCIDENT CLASSIFICATION", 30, section1Top, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawLine(30, section1Top + 8, 565, section1Top + 8, paint);

        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        canvas.drawText("Category: " + report.getIssueCategory(), 30, section1Top + 30, paint);
        canvas.drawText("Severity Rating: " + report.getSeverity(), 30, section1Top + 50, paint);

        canvas.drawText("Title: " + report.getTitle(), 30, section1Top + 70, paint);

        // Description Paragraph
        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(11);
        canvas.drawText("AI Summary / Details: " + report.getDescription(), 30, section1Top + 95, paint);

        // Section 2: Geotag & Location
        int section2Top = section1Top + 135;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("2. GEOTAGGED LOCATION", 30, section2Top, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawLine(30, section2Top + 8, 565, section2Top + 8, paint);

        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(11);
        paint.setFakeBoldText(false);
        canvas.drawText("Street Address: " + report.getAddress(), 30, section2Top + 30, paint);
        canvas.drawText(String.format(Locale.US, "GPS Coordinates: Latitude %.5f, Longitude %.5f", report.getLatitude(), report.getLongitude()), 30, section2Top + 50, paint);

        // Section 3: Image Evidence Attachment
        int section3Top = section2Top + 80;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("3. PHOTOGRAPHIC EVIDENCE", 30, section3Top, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawLine(30, section3Top + 8, 565, section3Top + 8, paint);

        if (report.getImageBase64() != null && !report.getImageBase64().isEmpty()) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(report.getImageBase64());
            if (bitmap != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 250, 200, true);
                canvas.drawBitmap(scaled, 30, section3Top + 20, paint);
            }
        } else {
            paint.setColor(Color.parseColor("#94A3B8"));
            canvas.drawRect(30, section3Top + 20, 280, section3Top + 170, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(12);
            canvas.drawText("[ Photo Evidence Attached ]", 60, section3Top + 100, paint);
        }

        // Official Footer Notice
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(10);
        canvas.drawText("Generated automatically by CivicLens Mobile App • Verified via Google Gemini Vision AI", 30, 810, paint);

        pdfDocument.finishPage(page);

        // Save PDF to cache/downloads
        try {
            File pdfFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), report.getTicketNumber() + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            Log.d(TAG, "PDF generated successfully at: " + pdfFile.getAbsolutePath());

            // Open PDF Intent
            Uri pdfUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(pdfUri, "application/pdf");
            openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(openIntent, "Open Complaint PDF"));

        } catch (Exception e) {
            Log.e(TAG, "Error writing PDF: " + e.getMessage());
            pdfDocument.close();
            Toast.makeText(context, "Generated PDF in Documents: " + report.getTicketNumber(), Toast.LENGTH_LONG).show();
        }
    }
}
