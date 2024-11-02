package com.hideruu.tofutrack1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;

public class PasscodeActivity extends AppCompatActivity {

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passcode);

        // Find the login button in the layout
        loginButton = findViewById(R.id.loginButton);

        // Initialize BiometricPrompt
        biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // Authentication succeeded, proceed to the dashboard
                        Intent intent = new Intent(PasscodeActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Handle error
                        Toast.makeText(PasscodeActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // Inform the user that authentication failed
                        Toast.makeText(PasscodeActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });

        // Set up prompt info with device credential fallback
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Authenticate using your biometrics or device credentials")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL) // Enables PIN/pattern fallback
                .build();

        // Start biometric authentication initially
        biometricPrompt.authenticate(promptInfo);

        // Set the button to show the prompt again when clicked
        loginButton.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
    }
}