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
    private TextView tvCouncilNotesLabel;
    private TextView tvCouncilNotes;
    private TextView tvAssignedTo;
    private Button btnMarkAsSpam;
    private Button btnAssignToEngineer;
    private Button btnDeleteCouncilTicket;
    private LinearLayout actionButtonsLayout;

    private String ticketId;
    private String ticketDbId; // Database ID for updates
    private String ticketType;
    private String ticketSeverity;
    private String ticketLocation;
    private String ticketDescription;
    private String ticketTimestamp;
    private String ticketUsername;
    private String ticketImageUrl;
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
        tvCouncilNotesLabel = findViewById(R.id.tvCouncilNotesLabel);
        tvCouncilNotes = findViewById(R.id.tvCouncilNotes);
        tvAssignedTo = findViewById(R.id.tvAssignedTo);
        btnMarkAsSpam = findViewById(R.id.btnMarkAsSpam);
        btnAssignToEngineer = findViewById(R.id.btnAssignToEngineer);
        btnDeleteCouncilTicket = findViewById(R.id.btnDeleteCouncilTicket);
        
        // Get the action buttons container
        actionButtonsLayout = (LinearLayout) btnAssignToEngineer.getParent();
    }

    private void getTicketDataFromIntent() {
        Intent intent = getIntent();
        ticketId = intent.getStringExtra("TICKET_ID");
        ticketDbId = intent.getStringExtra("TICKET_DB_ID"); // Get database ID
        ticketType = intent.getStringExtra("TICKET_TYPE");
        ticketSeverity = intent.getStringExtra("TICKET_SEVERITY");
        ticketLocation = intent.getStringExtra("TICKET_LOCATION");
        ticketDescription = intent.getStringExtra("TICKET_DESCRIPTION");
        ticketTimestamp = intent.getStringExtra("TICKET_TIMESTAMP");
        ticketUsername = intent.getStringExtra("TICKET_USERNAME");
        ticketImageUrl = intent.getStringExtra("TICKET_IMAGE_URL"); // Get image URL
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

        // Set image from URL or fallback to resource
        if (ticketImageUrl != null && !ticketImageUrl.isEmpty()) {
            loadImageFromUrl(ticketImageUrl);
        } else if (ticketImageResId != 0) {
            ivTicketImage.setImageResource(ticketImageResId);
        }

        // Fetch latest ticket from Supabase so we reflect assignment state
        if (ticketDbId != null && !ticketDbId.isEmpty()) {
            TicketRepository.getTicketByDbId(ticketDbId, new TicketRepository.FetchTicketCallback() {
                @Override
                public void onSuccess(Ticket ticket) {
                    runOnUiThread(() -> {
                        // Council notes - only show if not empty/null
                        if (ticket.getCouncilNotes() != null && !ticket.getCouncilNotes().isEmpty() && !ticket.getCouncilNotes().equalsIgnoreCase("null")) {
                            tvCouncilNotesLabel.setVisibility(View.VISIBLE);
                            tvCouncilNotes.setVisibility(View.VISIBLE);
                            tvCouncilNotes.setText(ticket.getCouncilNotes());
                        } else {
                            tvCouncilNotesLabel.setVisibility(View.GONE);
                            tvCouncilNotes.setVisibility(View.GONE);
                        }

                        // Assigned engineer - only show if assigned
                        boolean isAssigned = ticket.getAssignedTo() != null && !ticket.getAssignedTo().isEmpty() && !ticket.getAssignedTo().equalsIgnoreCase("null");
                        if (isAssigned) {
                            tvAssignedTo.setVisibility(View.VISIBLE);
                            tvAssignedTo.setText("Assigned to: " + ticket.getAssignedTo());
                        } else {
                            tvAssignedTo.setVisibility(View.GONE);
                        }

                        // Hide assign/spam buttons when assigned or spam
                        // Show delete button for completed/spam/accepted/rejected tickets
                        if (actionButtonsLayout != null) {
                            if (ticket.getStatus() == Ticket.TicketStatus.UNDER_REVIEW ||
                                ticket.getStatus() == Ticket.TicketStatus.ACCEPTED ||
                                ticket.getStatus() == Ticket.TicketStatus.REJECTED ||
                                ticket.getStatus() == Ticket.TicketStatus.SPAM) {
                                actionButtonsLayout.setVisibility(View.GONE);
                                // Show delete button for completed tickets
                                if (btnDeleteCouncilTicket != null) {
                                    btnDeleteCouncilTicket.setVisibility(View.VISIBLE);
                                }
                            } else {
                                actionButtonsLayout.setVisibility(View.VISIBLE);
                                if (btnDeleteCouncilTicket != null) {
                                    btnDeleteCouncilTicket.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        // If fetch fails, keep current UI; optionally hide buttons if we know it's assigned
                        android.widget.Toast.makeText(CouncilTicketDetailActivity.this, "Error loading ticket: " + message, android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            });
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
        
        // Delete Ticket - show only for completed/spam tickets
        if (btnDeleteCouncilTicket != null) {
            btnDeleteCouncilTicket.setOnClickListener(v -> showDeleteConfirmation());
        }
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
        // Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
            .setTitle("Loading Engineers...")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create();
        loadingDialog.show();
        
        // Fetch real engineers from Supabase
        TicketRepository.getEngineersWithStats(new TicketRepository.EngineersCallback() {
            @Override
            public void onSuccess(List<TicketRepository.Engineer> engineers) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    if (engineers.isEmpty()) {
                        Toast.makeText(CouncilTicketDetailActivity.this, "No engineers available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showEngineerDialog(engineers);
                });
            }
            
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(CouncilTicketDetailActivity.this, "Error loading engineers: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showEngineerDialog(List<TicketRepository.Engineer> engineers) {

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
        EngineerAdapter adapter = new EngineerAdapter(this, convertEngineers(engineers));
        lvEngineers.setAdapter(adapter);

        final TicketRepository.Engineer[] selectedEngineer = {null};

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
            
            // Assign ticket to engineer using Supabase
            TicketRepository.assignTicketToEngineer(
                ticketDbId,
                selectedEngineer[0].getId(),
                selectedEngineer[0].getName(),
                instructions,
                new TicketRepository.AssignTicketCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(CouncilTicketDetailActivity.this, 
                                "Ticket assigned to " + selectedEngineer[0].getName(), 
                                Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            
                            // Return to dashboard with result to trigger refresh
                            setResult(RESULT_OK);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(CouncilTicketDetailActivity.this, 
                                "Error assigning ticket: " + message, 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            );
        });

        dialog.show();
    }
    
    private void loadImageFromUrl(String imageUrl) {
        // Load image from URL in background thread
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream());
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    if (bitmap != null && ivTicketImage != null) {
                        ivTicketImage.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("CouncilTicketDetail", "Error loading image from URL: " + e.getMessage(), e);
            }
        }).start();
    }
    
    // Convert TicketRepository.Engineer to local Engineer for adapter
    private List<Engineer> convertEngineers(List<TicketRepository.Engineer> repoEngineers) {
        List<Engineer> engineers = new ArrayList<>();
        for (TicketRepository.Engineer repoEng : repoEngineers) {
            engineers.add(new Engineer(
                repoEng.getName(),
                repoEng.getEmail(),
                repoEng.getTotalReports(),
                repoEng.getHighPriority()
            ));
        }
        return engineers;
    }

    // Engineer class for adapter
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
    
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Ticket")
            .setMessage("Are you sure you want to delete this ticket from council view? Citizens and engineers can still see it.")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (ticketDbId != null && !ticketDbId.isEmpty()) {
                    TicketRepository.softDeleteTicketForCouncil(ticketDbId, new TicketRepository.AssignTicketCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(CouncilTicketDetailActivity.this, "Ticket deleted from council view", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        }
                        
                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                Toast.makeText(CouncilTicketDetailActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
