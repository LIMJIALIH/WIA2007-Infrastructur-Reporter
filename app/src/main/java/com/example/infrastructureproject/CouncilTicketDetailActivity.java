package com.example.infrastructureproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.infrastructurereporter.R;

import java.util.ArrayList;
import java.util.List;

public class CouncilTicketDetailActivity extends AppCompatActivity {

    private ImageView ivTicketImage;
    private TextView tvTicketId;
    private TextView tvTicketType;
    private TextView tvSeverityBadge;
    private TextView tvLocation;
    private TextView tvTimestamp;
    private TextView tvDescription;
    private TextView tvUsername;
    private TextView tvAssignedTo;
    private Button btnMarkAsSpam;
    private Button btnAssignToEngineer;
    private LinearLayout actionButtonsLayout;

    private String ticketId;
    private String ticketType;
    private String ticketSeverity;
    private String ticketLocation;
    private String ticketDescription;
    private String ticketTimestamp;
    private String ticketUsername;
    private int ticketImageResId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_council_ticket_detail);

        // Initialize views
        initializeViews();

        // Get ticket data from intent
        getTicketDataFromIntent();

        // Display ticket data
        displayTicketData();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        ivTicketImage = findViewById(R.id.ivTicketImage);
        tvTicketId = findViewById(R.id.tvTicketId);
        tvTicketType = findViewById(R.id.tvTicketType);
        tvSeverityBadge = findViewById(R.id.tvSeverityBadge);
        tvLocation = findViewById(R.id.tvLocation);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvDescription = findViewById(R.id.tvDescription);
        tvUsername = findViewById(R.id.tvUsername);
        tvAssignedTo = findViewById(R.id.tvAssignedTo);
        btnMarkAsSpam = findViewById(R.id.btnMarkAsSpam);
        btnAssignToEngineer = findViewById(R.id.btnAssignToEngineer);
        
        // Get the action buttons container
        actionButtonsLayout = (LinearLayout) btnAssignToEngineer.getParent();
    }

    private void getTicketDataFromIntent() {
        Intent intent = getIntent();
        ticketId = intent.getStringExtra("TICKET_ID");
        ticketType = intent.getStringExtra("TICKET_TYPE");
        ticketSeverity = intent.getStringExtra("TICKET_SEVERITY");
        ticketLocation = intent.getStringExtra("TICKET_LOCATION");
        ticketDescription = intent.getStringExtra("TICKET_DESCRIPTION");
        ticketTimestamp = intent.getStringExtra("TICKET_TIMESTAMP");
        ticketUsername = intent.getStringExtra("TICKET_USERNAME");
        ticketImageResId = intent.getIntExtra("TICKET_IMAGE", R.drawable.placeholder_image);
    }

    private void displayTicketData() {
        tvTicketId.setText("Ticket ID: " + (ticketId != null ? ticketId : "N/A"));
        tvTicketType.setText(ticketType != null ? ticketType : "Unknown");
        tvLocation.setText(ticketLocation != null ? ticketLocation : "Unknown location");
        tvTimestamp.setText(ticketTimestamp != null ? ticketTimestamp : "Unknown time");
        tvDescription.setText(ticketDescription != null ? ticketDescription : "No description");
        tvUsername.setText("Reported by: " + (ticketUsername != null ? ticketUsername : "Anonymous"));

        // Set severity badge
        if (tvSeverityBadge != null && ticketSeverity != null) {
            tvSeverityBadge.setText(ticketSeverity);
            int severityColor;
            switch (ticketSeverity.toLowerCase()) {
                case "high":
                    severityColor = getResources().getColor(R.color.severity_high, null);
                    break;
                case "medium":
                    severityColor = getResources().getColor(R.color.severity_medium, null);
                    break;
                case "low":
                default:
                    severityColor = getResources().getColor(R.color.severity_low, null);
                    break;
            }
            tvSeverityBadge.setBackgroundColor(severityColor);
        }

        // Set image
        if (ticketImageResId != 0) {
            ivTicketImage.setImageResource(ticketImageResId);
        }

        // Check if ticket is completed and show assigned engineer
        TicketManager ticketManager = TicketManager.getInstance();
        Ticket ticket = ticketManager.getTicketById(ticketId);
        if (ticket != null && ticket.getStatus() == Ticket.TicketStatus.ACCEPTED && ticket.getAssignedTo() != null) {
            tvAssignedTo.setText("Assigned to: " + ticket.getAssignedTo());
            tvAssignedTo.setVisibility(View.VISIBLE);
            // Hide action buttons when ticket is already assigned
            if (actionButtonsLayout != null) {
                actionButtonsLayout.setVisibility(View.GONE);
            }
        } else if (ticket != null && ticket.getStatus() == Ticket.TicketStatus.SPAM) {
            // Hide action buttons when ticket is marked as spam
            tvAssignedTo.setVisibility(View.GONE);
            if (actionButtonsLayout != null) {
                actionButtonsLayout.setVisibility(View.GONE);
            }
        } else {
            tvAssignedTo.setVisibility(View.GONE);
            if (actionButtonsLayout != null) {
                actionButtonsLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupClickListeners() {
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Mark as Spam
        btnMarkAsSpam.setOnClickListener(v -> showMarkAsSpamDialog());

        // Assign to Engineer
        btnAssignToEngineer.setOnClickListener(v -> showEngineerSelectionDialog());
    }

    private void showMarkAsSpamDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Spam")
                .setMessage("Are you sure you want to mark this ticket as spam? This action cannot be undone.")
                .setPositiveButton("Yes, Mark as Spam", (dialog, which) -> {
                    // Update ticket status to SPAM using TicketManager
                    TicketManager ticketManager = TicketManager.getInstance();
                    Ticket ticket = ticketManager.getTicketById(ticketId);
                    if (ticket != null) {
                        ticket.setStatus(Ticket.TicketStatus.SPAM);
                        ticketManager.updateTicket(ticket);
                    }
                    // TODO: Implement Supabase spam marking
                    Toast.makeText(this, "Ticket marked as spam", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEngineerSelectionDialog() {
        // Mock engineer data with ticket counts
        List<Engineer> engineers = new ArrayList<>();
        engineers.add(new Engineer("John Doe", "john.doe@example.com", 15, 5));
        engineers.add(new Engineer("Jane Smith", "jane.smith@example.com", 8, 2));
        engineers.add(new Engineer("Mike Johnson", "mike.johnson@example.com", 12, 4));
        engineers.add(new Engineer("Sarah Williams", "sarah.williams@example.com", 6, 1));
        engineers.add(new Engineer("David Brown", "david.brown@example.com", 20, 8));

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_engineer_selection, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Setup dialog views
        ListView lvEngineers = dialogView.findViewById(R.id.lvEngineers);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAssign = dialogView.findViewById(R.id.btnAssign);
        EditText etInstructionsDialog = dialogView.findViewById(R.id.etInstructionsDialog);

        // Setup ListView adapter
        EngineerAdapter adapter = new EngineerAdapter(this, engineers);
        lvEngineers.setAdapter(adapter);

        final Engineer[] selectedEngineer = {null};

        lvEngineers.setOnItemClickListener((parent, view, position, id) -> {
            selectedEngineer[0] = engineers.get(position);
            adapter.setSelectedPosition(position);
            adapter.notifyDataSetChanged();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAssign.setOnClickListener(v -> {
            if (selectedEngineer[0] == null) {
                Toast.makeText(this, "Please select an engineer", Toast.LENGTH_SHORT).show();
                return;
            }

            String instructions = etInstructionsDialog.getText().toString().trim();
            
            // Update ticket status to ACCEPTED (Completed) using TicketManager
            TicketManager ticketManager = TicketManager.getInstance();
            Ticket ticket = ticketManager.getTicketById(ticketId);
            if (ticket != null) {
                ticket.setStatus(Ticket.TicketStatus.ACCEPTED);
                ticket.setAssignedTo(selectedEngineer[0].getName());
                ticketManager.updateTicket(ticket);
            }
            
            // TODO: Implement Supabase assignment with instructions
            Toast.makeText(this, "Ticket assigned to " + selectedEngineer[0].getName(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    // Engineer class for mock data
    public static class Engineer {
        private String name;
        private String email;
        private int totalTickets;
        private int highPriorityTickets;

        public Engineer(String name, String email, int totalTickets, int highPriorityTickets) {
            this.name = name;
            this.email = email;
            this.totalTickets = totalTickets;
            this.highPriorityTickets = highPriorityTickets;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public int getTotalTickets() {
            return totalTickets;
        }

        public int getHighPriorityTickets() {
            return highPriorityTickets;
        }
    }
}
