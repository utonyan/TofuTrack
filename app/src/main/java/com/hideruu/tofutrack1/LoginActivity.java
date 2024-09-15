package com.hideruu.tofutrack1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    // Define the expiration period (1 day)
    private static final long EXPIRATION_PERIOD_MILLIS = 24 * 60 * 60 * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        // Check if the user is already logged in and if the session is still valid
        if (isSessionValid()) {
            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
            finish();
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required.");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters long.");
            return;
        }

        // Show ProgressBar
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // Hide ProgressBar
                progressBar.setVisibility(View.GONE);

                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = mAuth.getCurrentUser();

                    // Update the last login time
                    updateLastLoginTime();

                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    // Handle different error cases
                    String errorMessage = "Login Failed";
                    if (task.getException() != null) {
                        String exceptionMessage = task.getException().getMessage();
                        if (exceptionMessage != null) {
                            if (exceptionMessage.contains("password")) {
                                errorMessage = "Incorrect password. Please try again.";
                            } else if (exceptionMessage.contains("email")) {
                                errorMessage = "No account found with this email.";
                            } else {
                                errorMessage = "Login Failed: " + exceptionMessage;
                            }
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateLastLoginTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        long currentTimeMillis = Calendar.getInstance().getTimeInMillis();
        editor.putLong("lastLoginTime", currentTimeMillis);
        editor.apply();
    }

    private boolean isSessionValid() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long lastLoginTime = prefs.getLong("lastLoginTime", -1);
        long currentTimeMillis = Calendar.getInstance().getTimeInMillis();
        return (lastLoginTime != -1 && (currentTimeMillis - lastLoginTime) < EXPIRATION_PERIOD_MILLIS);
    }
}
