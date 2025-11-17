package com.example.infrastructureproject;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import com.example.infrastructurereporter.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private TextView myReportsButton;
    private TextView newReportsButton;
    private CardView selectionBackground;
    private PopupWindow popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize toggle buttons
        initializeToggleButtons();
    }

    private void initializeToggleButtons() {
        myReportsButton = findViewById(R.id.myReportsButton);
        newReportsButton = findViewById(R.id.newReportsButton);
        selectionBackground = findViewById(R.id.selectionBackground);

        // Initialize FAB
        FloatingActionButton fab = findViewById(R.id.floatingActionButton6);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAIAssistantPopup();
            }
        });

        // Set initial state - My Reports selected by default
        setSelectedButton(myReportsButton);

        // Set click listeners for toggle buttons
        myReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedButton(myReportsButton);
                showMyReports();
            }
        });

        newReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedButton(newReportsButton);
                showNewReports();
            }
        });
    }

    private void showAIAssistantPopup() {
        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_ai_assistant, null);

        // Calculate popup dimensions (70% of screen)
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);

        // Create popup window
        PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.setElevation(20f);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_frame));

        // Set close button listener
        ImageButton closeButton = popupView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });

        // Show popup centered on screen
        popupWindow.showAtLocation(findViewById(R.id.main), Gravity.CENTER, 0, 0);
    }
    private void setSelectedButton(TextView selectedButton) {
        // Update text colors and styles
        if (selectedButton == myReportsButton) {
            myReportsButton.setTextColor(Color.BLACK);
            myReportsButton.setTypeface(null, Typeface.BOLD);
            newReportsButton.setTextColor(Color.parseColor("#666666"));
            newReportsButton.setTypeface(null, Typeface.NORMAL);

            // Move selection background to My Reports
            selectionBackground.animate().x(0f).setDuration(200).start();

        } else {
            newReportsButton.setTextColor(Color.BLACK);
            newReportsButton.setTypeface(null, Typeface.BOLD);
            myReportsButton.setTextColor(Color.parseColor("#666666"));
            myReportsButton.setTypeface(null, Typeface.NORMAL);

            // Move selection background to New Reports
            selectionBackground.animate().x(500).setDuration(200).start();
        }
    }

    private void showMyReports() {
        // Show My Reports content
        TextView contentTitle = findViewById(R.id.contentTitle);
        TextView refreshButton = findViewById(R.id.refreshButton);
        CardView reportsContainer = findViewById(R.id.reportsContainer);

        contentTitle.setText("Your submitted reports");
        contentTitle.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        reportsContainer.setVisibility(View.VISIBLE);
    }

    private void showNewReports() {
        // Show New Reports content
        TextView contentTitle = findViewById(R.id.contentTitle);
        TextView refreshButton = findViewById(R.id.refreshButton);
        CardView reportsContainer = findViewById(R.id.reportsContainer);
        CardView reportsContainer2 = findViewById(R.id.reportsContainer2);

        contentTitle.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE); // Hide refresh button
        reportsContainer.setVisibility(View.GONE); // Hide my reports container
        reportsContainer2.setVisibility(View.VISIBLE); // Show new reports container
    }
}