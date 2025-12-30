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

public class LoginActivity extends AppCompatActivity {

    private TextView signupTab;
    private TextView citizenButton;
    private TextView engineerButton;
    private TextView councilButton;
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView forgotPassword;
    private String selectedRole = "citizen"; // Default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // enableEdgeToEdge(); // This method is not available in the Java Activity template.
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        signupTab = findViewById(R.id.signup_tab);
        citizenButton = findViewById(R.id.citizen_button);
        engineerButton = findViewById(R.id.engineer_button);
        councilButton = findViewById(R.id.council_button);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.button);
        forgotPassword = findViewById(R.id.forgot_password);

        signupTab.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        citizenButton.setOnClickListener(v -> {
            selectRole(citizenButton);
            deselectRole(engineerButton);
            deselectRole(councilButton);
            selectedRole = "citizen";
        });

        engineerButton.setOnClickListener(v -> {
            selectRole(engineerButton);
            deselectRole(citizenButton);
            deselectRole(councilButton);
            selectedRole = "engineer";
        });

        councilButton.setOnClickListener(v -> {
            selectRole(councilButton);
            deselectRole(citizenButton);
            deselectRole(engineerButton);
            selectedRole = "council";
        });

        loginButton.setOnClickListener(v -> handleLogin());
    }

    private void selectRole(TextView selectedButton) {
        selectedButton.setBackgroundResource(R.drawable.black_corner_background);
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void deselectRole(TextView deselectedButton) {
        deselectedButton.setBackgroundResource(R.drawable.white_border_corner_background);
        deselectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Basic validation
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@")) {
            Toast.makeText(this, "Not a proper email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use SupabaseManager to login
        SupabaseManager.login(email, password, new SupabaseManager.AuthCallback() {
            @Override
            public void onSuccess(String role, String fullName) {
                if (role != null) {
                    if (role.equalsIgnoreCase(selectedRole)) {
                         Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                         Intent intent;
                         if (selectedRole.equals("engineer")) {
                             intent = new Intent(LoginActivity.this, EngineerDashboardActivity.class);
                         } else if (selectedRole.equals("council")) {
                             intent = new Intent(LoginActivity.this, CouncilDashboardActivity.class);
                         } else {
                             intent = new Intent(LoginActivity.this, CitizenDashboardActivity.class);
                         }
                         startActivity(intent);
                         finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Role mismatch. Please select the correct role.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Login successful but role not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
