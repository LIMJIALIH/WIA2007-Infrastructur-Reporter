package com.example.infrastructureproject;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.infrastructurereporter.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView myReportsButton;
    private TextView newReportsButton;
    private CardView selectionBackground;
    private MaterialButton logoutButton;
    private MaterialButton refreshButton;
    private TextView totalReportsNumber;
    private TextView card2Number; // Pending
    private TextView card3Number; // Rejected/Under Review
    private TextView card4Number; // Accepted
    private RecyclerView reportsRecyclerView;
    private TicketAdapter ticketAdapter;
    private CardView reportsContainer;
    private View reportsContainer2;
    private ReportIssueFragment reportIssueFragment;

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

        // Initialize views
        initializeViews();

        // Initialize toggle buttons
        initializeToggleButtons();
        
        // Initialize logout button
        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogout();
            }
        });

        // Setup RecyclerView for My Reports
        setupRecyclerView();

        // Load initial data
        updateDashboardCounts();
    }

    private void initializeViews() {
        // Card count TextViews
        totalReportsNumber = findViewById(R.id.totalReportsNumber);
        card2Number = findViewById(R.id.card2Number);
        card3Number = findViewById(R.id.card3Number);
        card4Number = findViewById(R.id.card4Number);

        // Refresh button
        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshMyReports();
            }
        });

        // Reports containers
        reportsContainer = findViewById(R.id.reportsContainer);
        reportsContainer2 = findViewById(R.id.reportsContainer2);

        // Initialize toggle buttons (these will be set in initializeToggleButtons)
        myReportsButton = findViewById(R.id.myReportsButton);
        newReportsButton = findViewById(R.id.newReportsButton);
        selectionBackground = findViewById(R.id.selectionBackground);
    }

    private void setupRecyclerView() {
        reportsRecyclerView = new RecyclerView(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        reportsRecyclerView.setLayoutManager(layoutManager);

        // Create adapter with view-only listener (read-only view for citizen)
        TicketAdapter.OnTicketActionListener readOnlyListener = new TicketAdapter.OnTicketActionListener() {
            @Override
            public void onAccept(Ticket ticket, int position) {
                // Not used for citizen view
            }

            @Override
            public void onReject(Ticket ticket, int position) {
                // Not used for citizen view
            }

            @Override
            public void onSpam(Ticket ticket, int position) {
                // Not used for citizen view
            }

            @Override
            public void onView(Ticket ticket, int position) {
                // Open ticket details
                Intent intent = new Intent(MainActivity.this, TicketDetailActivity.class);
                intent.putExtra("ticket_id", ticket.getId());
                intent.putExtra("type", ticket.getType());
                intent.putExtra("severity", ticket.getSeverity());
                intent.putExtra("location", ticket.getLocation());
                intent.putExtra("date_time", ticket.getDateTime());
                intent.putExtra("description", ticket.getDescription());
                intent.putExtra("image_name", ticket.getImageName());
                intent.putExtra("status", ticket.getStatus().toString());
                intent.putExtra("citizen_view", true);
                if (ticket.getReason() != null) {
                    intent.putExtra("reason", ticket.getReason());
                }
                startActivity(intent);
            }

            @Override
            public void onDelete(Ticket ticket, int position) {
                // Citizens cannot delete tickets
            }
        };

        ticketAdapter = new TicketAdapter(this, readOnlyListener);
        reportsRecyclerView.setAdapter(ticketAdapter);

        // Add RecyclerView to reports container
        LinearLayout containerLayout = (LinearLayout) reportsContainer.getChildAt(0);
        containerLayout.removeAllViews();
        containerLayout.addView(reportsRecyclerView);
    }

    private void refreshMyReports() {
        // Get all tickets for this citizen (in real app, filter by user)
        List<Ticket> allTickets = TicketManager.getInstance().getAllTickets();
        ticketAdapter.setTickets(allTickets);
        updateDashboardCounts();
        Toast.makeText(this, "Reports refreshed", Toast.LENGTH_SHORT).show();
    }

    private void updateDashboardCounts() {
        TicketManager ticketManager = TicketManager.getInstance();

        // Update card counts
        int totalCount = ticketManager.getTotalTicketCount();
        int pendingCount = ticketManager.getTicketCountByStatus(Ticket.TicketStatus.PENDING);
        int rejectedCount = ticketManager.getTicketCountByStatus(Ticket.TicketStatus.REJECTED);
        int acceptedCount = ticketManager.getTicketCountByStatus(Ticket.TicketStatus.ACCEPTED);

        totalReportsNumber.setText(String.valueOf(totalCount));
        card2Number.setText(String.valueOf(pendingCount));
        card3Number.setText(String.valueOf(rejectedCount));
        card4Number.setText(String.valueOf(acceptedCount));

        // Refresh My Reports list
        List<Ticket> allTickets = ticketManager.getAllTickets();
        if (ticketAdapter != null) {
            ticketAdapter.setTickets(allTickets);
        }
    }

    // Call this method when a new report is submitted
    public void onNewReportSubmitted(Ticket newTicket) {
        updateDashboardCounts();
        Toast.makeText(this, "Report submitted successfully! ID: " + newTicket.getId(),
                Toast.LENGTH_SHORT).show();

        // Switch back to My Reports view
        setSelectedButton(myReportsButton);
        showMyReports();
    }

    private void handleLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

        // Calculate popup dimensions (90% of screen)
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);

        // Create popup window
        final PopupWindow aiPopupWindow = new PopupWindow(popupView, width, height, true);
        aiPopupWindow.setElevation(20f);
        aiPopupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_frame));

        // Set close button listener
        ImageButton closeButton = popupView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aiPopupWindow.dismiss();
            }
        });

        // Show popup centered on screen
        aiPopupWindow.showAtLocation(findViewById(R.id.main), Gravity.CENTER, 0, 0);
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
        MaterialButton refreshButton = findViewById(R.id.refreshButton);
        CardView reportsContainer = findViewById(R.id.reportsContainer);
        View reportsContainer2 = findViewById(R.id.reportsContainer2);

        contentTitle.setText("Your submitted reports");
        contentTitle.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        reportsContainer.setVisibility(View.VISIBLE);
        reportsContainer2.setVisibility(View.GONE);

        // Refresh the reports list
        refreshMyReports();
        
        // Hide ReportIssueFragment if it exists
        if (reportIssueFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.hide(reportIssueFragment);
            transaction.commit();
        }
    }

    private void showNewReports() {
        // Show New Reports content
        TextView contentTitle = findViewById(R.id.contentTitle);
        MaterialButton refreshButton = findViewById(R.id.refreshButton);
        CardView reportsContainer = findViewById(R.id.reportsContainer);
        View reportsContainer2 = findViewById(R.id.reportsContainer2);

        contentTitle.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        reportsContainer.setVisibility(View.GONE);

        // Clear the FrameLayout
        if (reportsContainer2 instanceof ViewGroup) {
            ((ViewGroup) reportsContainer2).removeAllViews();
        }

        // Inflate the report_issue layout directly
        LayoutInflater inflater = LayoutInflater.from(this);
        View newReportView = inflater.inflate(R.layout.report_issue, (ViewGroup) reportsContainer2, false);

        // Set up the view directly (using -test folder functionality)
        setupNewReportView(newReportView);

        if (reportsContainer2 instanceof ViewGroup) {
            ((ViewGroup) reportsContainer2).addView(newReportView);
        }
        reportsContainer2.setVisibility(View.VISIBLE);
        
        // Show ReportIssueFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        if (reportIssueFragment == null) {
            reportIssueFragment = ReportIssueFragment.newInstance();
            transaction.add(R.id.reportsContainer2, reportIssueFragment);
        } else {
            transaction.show(reportIssueFragment);
        }
        
        transaction.commit();
    }

    private void setupNewReportView(View view) {
        // Initialize views
        com.google.android.material.card.MaterialCardView cardTakePhoto =
                view.findViewById(R.id.card_take_photo);
        com.google.android.material.card.MaterialCardView cardUploadPhoto =
                view.findViewById(R.id.card_upload_photo);
        TextView locationText = view.findViewById(R.id.location_text);
        EditText descriptionInput = view.findViewById(R.id.description_input);
        Button submitButton = view.findViewById(R.id.submit_button);
        TextView enterManually = view.findViewById(R.id.enter_manually);

        // Set up listeners
        cardTakePhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Take photo (camera feature from -test folder)", Toast.LENGTH_SHORT).show();
        });

        cardUploadPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Upload photo (camera feature from -test folder)", Toast.LENGTH_SHORT).show();
        });

        enterManually.setOnClickListener(v -> {
            showLocationDialog(locationText);
        });

        // Enable/disable submit button
        descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                submitButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        submitButton.setOnClickListener(v -> {
            submitNewReport(descriptionInput.getText().toString(),
                    locationText.getText().toString());
        });
    }

    private void showLocationDialog(TextView locationText) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Location");

        final EditText input = new EditText(this);
        input.setText(locationText.getText());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            locationText.setText(input.getText().toString());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void submitNewReport(String description, String location) {
        // Create new ticket
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault());
        String currentTime = sdf.format(new java.util.Date());

        Ticket newTicket = new Ticket(
                "",
                "Road",
                "Medium",
                location,
                description,
                currentTime,
                "new_report_image"
        );

        // Add to TicketManager
        TicketManager.getInstance().addTicket(newTicket);

        // Update dashboard and switch back
        onNewReportSubmitted(newTicket);
    }

    // Handle card clicks to show filtered tickets dialog (from User-UI)
    public void onReportCardClicked(View view) {
        String reportType = "";

        // Get the tag from the clicked card
        if (view instanceof CardView) {
            ViewGroup card = (ViewGroup) view;
            for (int i = 0; i < card.getChildCount(); i++) {
                View child = card.getChildAt(i);
                if (child instanceof LinearLayout) {
                    ViewGroup linearLayout = (ViewGroup) child;
                    for (int j = 0; j < linearLayout.getChildCount(); j++) {
                        View grandChild = linearLayout.getChildAt(j);
                        if (grandChild instanceof TextView) {
                            String tag = (String) grandChild.getTag();
                            if (tag != null) {
                                reportType = tag;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Show dialog with filtered tickets (using -test folder ticket details view)
        TicketsDialogFragment dialog = TicketsDialogFragment.newInstance(reportType);
        dialog.show(getSupportFragmentManager(), TicketsDialogFragment.TAG);
    }
}