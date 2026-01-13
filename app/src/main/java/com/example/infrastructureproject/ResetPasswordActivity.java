package com.example.infrastructureproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;



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
            android.util.Log.d("ResetPassword", "No intent data received");
            return;
        }

        Uri data = intent.getData();
        android.util.Log.d("ResetPassword", "Received URL: " + data.toString());
        
        // Check if this is the Supabase verification URL
        if (data.getHost() != null && data.getHost().contains("supabase.co")) {
            // This is the direct email link - extract token from query parameter
            String token = data.getQueryParameter("token");
            String type = data.getQueryParameter("type");
            
            android.util.Log.d("ResetPassword", "Supabase URL - token: " + (token != null ? "found" : "null") + ", type: " + type);
            
            if (token != null && "recovery".equals(type)) {
                // Use the token directly as access token
                accessToken = token;
                android.util.Log.d("ResetPassword", "Using recovery token as access token");
                Toast.makeText(this, "Password reset link verified. Enter your new password.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Handle custom scheme redirect (infrastructurereporter://reset-password)
        // Supabase sends the token as a fragment: #access_token=...&refresh_token=...
        String fragment = data.getFragment();
        android.util.Log.d("ResetPassword", "Fragment: " + fragment);
        if (fragment != null) {
            String[] parts = fragment.split("&");
            for (String part : parts) {
                if (part.startsWith("access_token=")) {
                    accessToken = part.substring("access_token=".length());
                    android.util.Log.d("ResetPassword", "Found access token in fragment");
                    Toast.makeText(this, "Password reset ready. Enter your new password.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        
        // Check query params as fallback
        if (accessToken == null) {
            accessToken = data.getQueryParameter("access_token");
            if (accessToken != null) {
                android.util.Log.d("ResetPassword", "Found access token in query params");
                Toast.makeText(this, "Password reset ready. Enter your new password.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        if (accessToken == null) {
            android.util.Log.e("ResetPassword", "No valid token found in URL");
            Toast.makeText(this, "Invalid password reset link. Please request a new one.", Toast.LENGTH_LONG).show();
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
