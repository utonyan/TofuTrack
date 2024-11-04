package com.hideruu.tofutrack1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.Calendar;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_LICENSE_VERIFIED = "license_verified";
    private static final String KEY_LICENSE_KEY = "license_key";
    private static final String KEY_LAST_FETCH_TIME = "last_fetch_time";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean isLicenseVerified = preferences.getBoolean(KEY_LICENSE_VERIFIED, false);
        String storedLicenseKey = preferences.getString(KEY_LICENSE_KEY, null);
        long lastFetchTime = preferences.getLong(KEY_LAST_FETCH_TIME, 0);

        if (isOnline()) {
            // If online, verify license key from Firestore
            verifyStoredLicenseKey(storedLicenseKey);
        } else if (is24HoursPassed(lastFetchTime) && isLicenseVerified) {
            // If offline, more than 24 hours have passed, and license was previously verified
            Toast.makeText(this, "License verification expired. Please connect to the internet to re-verify.", Toast.LENGTH_LONG).show();
            finish();
        } else if (isLicenseVerified) {
            // If offline and within 24 hours, allow access without prompt
            proceedToPasscodeActivity();
        } else {
            Toast.makeText(this, "No internet connection. Please connect to verify license.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private boolean is24HoursPassed(long lastFetchTime) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        return (currentTime - lastFetchTime) > 24 * 60 * 60 * 1000;
    }

    private void verifyStoredLicenseKey(String storedLicenseKey) {
        DocumentReference docRef = db.collection("DRMConfig").document("licenses");
        docRef.get(Source.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String fetchedLicenseKey = task.getResult().getString("key");
                if (fetchedLicenseKey != null && fetchedLicenseKey.equals(storedLicenseKey)) {
                    saveLastFetchTime();
                    proceedToPasscodeActivity();
                } else {
                    Toast.makeText(this, "License key has been changed. Please re-enter the key.", Toast.LENGTH_SHORT).show();
                    promptForLicenseKey(fetchedLicenseKey); // Prompt for new key if it has changed
                }
            } else {
                Toast.makeText(this, "Error fetching license key from Firestore.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void saveLastFetchTime() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putLong(KEY_LAST_FETCH_TIME, Calendar.getInstance().getTimeInMillis());
        editor.apply();
    }

    private void promptForLicenseKey(String latestLicenseKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("License Activation");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String enteredLicenseKey = input.getText().toString();

            if (enteredLicenseKey.equals(latestLicenseKey)) {
                saveLicenseKey(latestLicenseKey); // Save new key on successful match
                proceedToPasscodeActivity();
            } else {
                Toast.makeText(SplashActivity.this, "Invalid license key. Please try again.", Toast.LENGTH_SHORT).show();
                promptForLicenseKey(latestLicenseKey); // Re-prompt if the entered key is incorrect
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            finish();
        });

        builder.show();
    }

    private void saveLicenseKey(String licenseKey) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_LICENSE_VERIFIED, true);
        editor.putString(KEY_LICENSE_KEY, licenseKey);
        editor.putLong(KEY_LAST_FETCH_TIME, Calendar.getInstance().getTimeInMillis());
        editor.apply();

        Toast.makeText(SplashActivity.this, "License verified successfully!", Toast.LENGTH_SHORT).show();
    }

    private void proceedToPasscodeActivity() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, PasscodeActivity.class));
            finish();
        }, 2000);
    }
}
