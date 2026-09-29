package com.waseem.civiclens.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import com.waseem.civiclens.models.CivicReport;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static final String COLLECTION_REPORTS = "reports";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    // Local standard cache for immediate preview / offline mode
    private static final List<CivicReport> localMemoryCache = new ArrayList<>();

    static {
        // Seed standard sample data for vibrant first view
        localMemoryCache.add(CivicReport.createNew(
                "user_01", "citizen.john@civiclens.org",
                "Pothole & Road Surface Damage", "HIGH",
                "Dangerous Deep Pothole on Anna Salai Road",
                "Deep 6-inch asphalt pothole near crosswalk causing vehicle damage and traffic slowdowns.",
                "Greater Chennai Corporation (GCC)", null,
                13.0604, 80.2496, "Anna Salai, Mount Road, T. Nagar, Chennai, Tamil Nadu, India"
        ));
        localMemoryCache.add(CivicReport.createNew(
                "user_02", "sarah.m@civiclens.org",
                "Waste & Trash Accumulation", "MEDIUM",
                "Overflowing Community Trash Bins near Marina Promenade",
                "Uncollected garbage piling up near public beach entrance for over 3 days.",
                "GCC Solid Waste Management", null,
                13.0475, 80.2824, "Marina Beach Road, Triplicane, Chennai, Tamil Nadu, India"
        ));
        localMemoryCache.add(CivicReport.createNew(
                "user_03", "alex.k@civiclens.org",
                "Broken Streetlight & Pole", "CRITICAL",
                "Non-functional Streetlight at Guindy Junction",
                "Streetlight fixture flickering and unlit creating safety hazard at night.",
                "TANGEDCO Electrical Board", null,
                13.0067, 80.2206, "Guindy Industrial Estate, Chennai, Tamil Nadu, India"
        ));
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }

    public interface ReportCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface ReportListCallback {
        void onSuccess(List<CivicReport> reports);
        void onError(String errorMessage);
    }

    public FirebaseHelper() {
        FirebaseAuth a = null;
        FirebaseFirestore d = null;
        try {
            a = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "FirebaseAuth init fallback: " + e.getMessage());
        }
        try {
            d = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "FirebaseFirestore init fallback: " + e.getMessage());
        }
        this.auth = a;
        this.db = d;
    }

    public FirebaseUser getCurrentUser() {
        return auth != null ? auth.getCurrentUser() : null;
    }

    public boolean isUserLoggedIn() {
        return auth != null && auth.getCurrentUser() != null;
    }

    public interface ProviderCheckCallback {
        void onResult(String provider);
    }

    public void checkEmailProvider(String email, ProviderCheckCallback callback) {
        if (auth == null || email == null || email.trim().isEmpty()) {
            callback.onResult(null);
            return;
        }
        try {
            auth.fetchSignInMethodsForEmail(email.trim().toLowerCase())
                    .addOnSuccessListener(result -> {
                        List<String> methods = result.getSignInMethods();
                        if (methods != null && !methods.isEmpty()) {
                            if (methods.contains("google.com") && !methods.contains("password")) {
                                callback.onResult("google");
                            } else if (methods.contains("password")) {
                                callback.onResult("password");
                            } else {
                                callback.onResult(methods.get(0));
                            }
                        } else {
                            callback.onResult(null);
                        }
                    })
                    .addOnFailureListener(e -> callback.onResult(null));
        } catch (Exception e) {
            callback.onResult(null);
        }
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        if (auth == null) {
            callback.onError("Offline Guest Mode");
            return;
        }
        checkEmailProvider(email, provider -> {
            if ("google".equals(provider)) {
                callback.onError("An account already exists with this email address using Google Sign-In. Please sign in with Google.");
                return;
            }
            try {
                auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Firebase Auth failed: " + e.getMessage());
                            callback.onError(e.getMessage());
                        });
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void registerUser(String email, String password, AuthCallback callback) {
        if (auth == null) {
            callback.onError("Offline Guest Mode");
            return;
        }
        checkEmailProvider(email, provider -> {
            if ("google".equals(provider)) {
                callback.onError("An account already exists with this email address using Google Sign-In. Please sign in with Google.");
                return;
            }
            if ("password".equals(provider)) {
                callback.onError("An account with this email address already exists. Please log in instead.");
                return;
            }
            try {
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void signOut() {
        if (auth != null) {
            try {
                auth.signOut();
            } catch (Exception e) {
                Log.w(TAG, "Sign out exception: " + e.getMessage());
            }
        }
    }

    public void submitReport(CivicReport report, ReportCallback callback) {
        localMemoryCache.add(0, report);
        if (db == null) {
            callback.onSuccess();
            return;
        }
        try {
            db.collection(COLLECTION_REPORTS)
                    .document(report.getReportId())
                    .set(report)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore write failed, stored in local cache: " + e.getMessage());
                        callback.onSuccess(); // Graceful fallback
                    });
        } catch (Exception e) {
            Log.e(TAG, "Firestore error: " + e.getMessage());
            callback.onSuccess();
        }
    }

    public void fetchReports(ReportListCallback callback) {
        if (db == null) {
            callback.onSuccess(new ArrayList<>(localMemoryCache));
            return;
        }
        try {
            db.collection(COLLECTION_REPORTS)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<CivicReport> reports = queryDocumentSnapshots.toObjects(CivicReport.class);
                        if (reports.isEmpty()) {
                            callback.onSuccess(new ArrayList<>(localMemoryCache));
                        } else {
                            callback.onSuccess(reports);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore read failed, returning local cache: " + e.getMessage());
                        callback.onSuccess(new ArrayList<>(localMemoryCache));
                    });
        } catch (Exception e) {
            callback.onSuccess(new ArrayList<>(localMemoryCache));
        }
    }
}
