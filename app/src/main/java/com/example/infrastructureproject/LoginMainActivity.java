package com.example.infrastructureproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.infrastructurereporter.R;

public class LoginMainActivity extends AppCompatActivity {

    private TextView loginTab;
    private TextView signupTab;
    private TextView citizenButton;
    private TextView engineerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // enableEdgeToEdge(); // This method is not available in the Java Activity template.
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        loginTab = findViewById(R.id.login_tab);
        signupTab = findViewById(R.id.signup_tab);
        citizenButton = findViewById(R.id.citizen_button);
        engineerButton = findViewById(R.id.engineer_button);

        loginTab.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });

        citizenButton.setOnClickListener(v -> {
            selectRole(citizenButton);
            deselectRole(engineerButton);
        });

        engineerButton.setOnClickListener(v -> {
            selectRole(engineerButton);
            deselectRole(citizenButton);
        });
    }

    private void selectRole(TextView selectedButton) {
        selectedButton.setBackgroundResource(R.drawable.black_corner_background);
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void deselectRole(TextView deselectedButton) {
        deselectedButton.setBackgroundResource(R.drawable.white_border_corner_background);
        deselectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }
}