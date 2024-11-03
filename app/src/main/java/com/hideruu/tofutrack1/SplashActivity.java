package com.hideruu.tofutrack1;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_LICENSE_VERIFIED = "license_verified";
    private static final String APP_LICENSE_KEY = "wow"; // Replace with your desired license key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLicenseVerified = preferences.getBoolean(KEY_LICENSE_VERIFIED, false);

        if (!isLicenseVerified) {
            promptForLicenseKey(); // Show the license key prompt
        } else {
            proceedToPasscodeActivity(); // License key is verified, proceed
        }
    }

    private void promptForLicenseKey() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("License Activation");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredLicenseKey = input.getText().toString();

                if (enteredLicenseKey.equals(APP_LICENSE_KEY)) {
                    // Save license verification status in SharedPreferences
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(KEY_LICENSE_VERIFIED, true);
                    editor.apply();

                    Toast.makeText(SplashActivity.this, "License verified successfully!", Toast.LENGTH_SHORT).show();
                    proceedToPasscodeActivity(); // Proceed to PasscodeActivity
                } else {
                    Toast.makeText(SplashActivity.this, "Invalid license key. Please try again.", Toast.LENGTH_SHORT).show();
                    promptForLicenseKey(); // Re-prompt if the license key is incorrect
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish(); // Exit the app if the license key is not entered
            }
        });

        builder.show();
    }

    private void proceedToPasscodeActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
                finish();
            }
        }, 2000);
    }
}
