package com.example.infrastructureproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.infrastructurereporter.R;

public class SignUpActivity extends AppCompatActivity {

    private TextView loginTab;
    private TextView citizenButton;
    private TextView engineerButton;
    private EditText fullNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button signUpButton;
    private String selectedRole = "citizen"; // Default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        loginTab = findViewById(R.id.login_tab);
        citizenButton = findViewById(R.id.citizen_button);
        engineerButton = findViewById(R.id.engineer_button);
        fullNameInput = findViewById(R.id.full_name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        signUpButton = findViewById(R.id.button);

        loginTab.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });

        citizenButton.setOnClickListener(v -> {
            selectRole(citizenButton);
            deselectRole(engineerButton);
            selectedRole = "citizen";
        });

        engineerButton.setOnClickListener(v -> {
            selectRole(engineerButton);
            deselectRole(citizenButton);
            selectedRole = "engineer";
        });

        signUpButton.setOnClickListener(v -> handleSignUp());
    }

    private void selectRole(TextView selectedButton) {
        selectedButton.setBackgroundResource(R.drawable.black_corner_background);
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void deselectRole(TextView deselectedButton) {
        deselectedButton.setBackgroundResource(R.drawable.white_border_corner_background);
        deselectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void handleSignUp() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Basic validation
        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@")) {
            Toast.makeText(this, "Not a proper email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use SupabaseManager to sign up
        SupabaseManager.signUp(email, password, fullName, selectedRole, new SupabaseManager.AuthCallback() {
            @Override
            public void onSuccess(String role, String fullName) {
                Toast.makeText(SignUpActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                
                Intent intent;
                if (selectedRole.equals("engineer")) {
                    intent = new Intent(SignUpActivity.this, EngineerDashboardActivity.class);
                } else {
                    intent = new Intent(SignUpActivity.this, CitizenDashboardActivity.class);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
