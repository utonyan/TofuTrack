package com.hideruu.tofutrack1;

import android.content.Intent; // Add this import
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PasscodeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "PasscodePrefs";
    private static final String ATTEMPTS_KEY = "attempts_remaining";
    private static final String LOCKOUT_TIME_KEY = "lockout_time";

    private EditText emailEditText;
    private Button loginButton;
    private ProgressBar progressBar;

    private int attemptsRemaining = 5;
    private boolean isLockedOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passcode);

        emailEditText = findViewById(R.id.emailEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        // Load attempts remaining from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        attemptsRemaining = prefs.getInt(ATTEMPTS_KEY, 5);

        // Disable button if locked out
        if (prefs.getBoolean(LOCKOUT_TIME_KEY, false)) {
            startLockoutTimer();
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePasscodeEntry();
            }
        });
    }

    private void handlePasscodeEntry() {
        String passcode = emailEditText.getText().toString().trim();

        // Check if the passcode length is valid
        if (passcode.length() != 6) {
            Toast.makeText(this, "Passcode must be 6 digits long", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate passcode validation (you can replace this with your actual validation logic)
        if (isValidPasscode(passcode)) {
            // Successful entry - open DashboardActivity
            Toast.makeText(this, "Passcode accepted!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(PasscodeActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish(); // Optional: Close PasscodeActivity
        } else {
            attemptsRemaining--;
            Toast.makeText(this, "Incorrect passcode! Attempts remaining: " + attemptsRemaining, Toast.LENGTH_SHORT).show();

            if (attemptsRemaining <= 0) {
                lockOutUser();
            } else {
                // Save the updated attempts remaining
                saveAttemptsRemaining();
            }
        }
    }

    private boolean isValidPasscode(String passcode) {
        // Replace this with your actual passcode check
        return "080808".equals(passcode);
    }

    private void lockOutUser() {
        isLockedOut = true;
        Toast.makeText(this, "Too many attempts! You are locked out for 5 minutes.", Toast.LENGTH_SHORT).show();
        loginButton.setEnabled(false);

        // Start lockout timer
        startLockoutTimer();
    }

    private void startLockoutTimer() {
        new CountDownTimer(5 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                // You can update UI here to show remaining time if needed
            }

            @Override
            public void onFinish() {
                isLockedOut = false;
                attemptsRemaining = 5; // Reset attempts
                saveAttemptsRemaining();
                Toast.makeText(PasscodeActivity.this, "You can try again now.", Toast.LENGTH_SHORT).show();
                loginButton.setEnabled(true);
            }
        }.start();
    }

    private void saveAttemptsRemaining() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(ATTEMPTS_KEY, attemptsRemaining);
        editor.putBoolean(LOCKOUT_TIME_KEY, isLockedOut);
        editor.apply();
    }
}
