package com.example.infrastructureproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.infrastructurereporter.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button updatePasswordButton;
    private ImageView backButton;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        updatePasswordButton = findViewById(R.id.update_password_button);
        backButton = findViewById(R.id.back_button);

        handleIntent(getIntent());

        updatePasswordButton.setOnClickListener(v -> handleUpdatePassword());
        backButton.setOnClickListener(v -> finish());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        Uri data = intent.getData();
        // Supabase sends the token as a fragment: #access_token=...&refresh_token=...
        // We need to parse this fragment.
        String fragment = data.getFragment();
        if (fragment != null) {
            String[] parts = fragment.split("&");
            for (String part : parts) {
                if (part.startsWith("access_token=")) {
                    accessToken = part.substring("access_token=".length());
                    break;
                }
            }
        }
        
        // If it's a type=recovery, it might come as query parameter sometimes depending on config,
        // but typically it is a fragment for implicit flows.
        // If we didn't find it in fragment, check query params just in case.
        if (accessToken == null) {
            accessToken = data.getQueryParameter("access_token");
        }
        
        if (accessToken == null) {
             Toast.makeText(this, "Invalid password reset link.", Toast.LENGTH_LONG).show();
             // potentially finish() or redirect to login
        }
    }

    private void handleUpdatePassword() {
        if (accessToken == null) {
            Toast.makeText(this, "Session expired or invalid link. Please request a new password reset.", Toast.LENGTH_LONG).show();
            return;
        }

        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseManager.updatePassword(password, accessToken, new SupabaseManager.AuthCallback() {
            @Override
            public void onSuccess(String role, String fullName) {
                Toast.makeText(ResetPasswordActivity.this, "Password updated successfully. Please login.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ResetPasswordActivity.this, "Failed to update password: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
