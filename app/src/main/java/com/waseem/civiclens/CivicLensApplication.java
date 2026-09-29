package com.waseem.civiclens;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;

public class CivicLensApplication extends Application {
    private static final String TAG = "CivicLensApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
        }
    }
}
