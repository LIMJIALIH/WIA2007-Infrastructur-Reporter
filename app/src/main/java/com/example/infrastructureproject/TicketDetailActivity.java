package com.example.infrastructureproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.infrastructurereporter.R;

public class TicketDetailActivity extends AppCompatActivity {

    // UI Components
    private ImageView ivTicketImage;
    private TextView tvTicketId;
    private TextView tvType;
    private TextView tvSeverity;
    private TextView tvDescription;
    private TextView tvStatus;
    private TextView tvLocation;
    private TextView tvDateTime;
    private ImageView ivClose;

    private Ticket ticket;

    private boolean isEngineerView = false;
    private Button btnAccept;
    private Button btnReject;
    private Button btnSpam;
    private ImageView ivBack;
    private TextView tvReason;
    private TextView labelReason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if opened from engineer dashboard
        isEngineerView = !getIntent().getBooleanExtra("citizen_view", true);
        
        // Load appropriate layout
        if (isEngineerView) {
            setContentView(R.layout.activity_ticket_detail_engineer);
        } else {
            setContentView(R.layout.activity_ticket_detail);
        }

        initializeViews();
        setupTicketData();
        setupListeners();
    }

    private void initializeViews() {
        if (isEngineerView) {
            // Engineer view
            ivTicketImage = findViewById(R.id.ivTicketImage);
            ivBack = findViewById(R.id.ivBack);
            tvTicketId = findViewById(R.id.tvTicketId);
            tvType = findViewById(R.id.tvType);
            tvSeverity = findViewById(R.id.tvSeverity);
            tvDescription = findViewById(R.id.tvDescription);
            tvLocation = findViewById(R.id.tvLocation);
            tvDateTime = findViewById(R.id.tvDateTime);
            tvReason = findViewById(R.id.tvReason);
            labelReason = findViewById(R.id.labelReason);
            btnAccept = findViewById(R.id.btnAccept);
            btnReject = findViewById(R.id.btnReject);
            btnSpam = findViewById(R.id.btnSpam);
        } else {
            // Citizen view
            ivTicketImage = findViewById(R.id.ivTicketImageLarge);
            tvTicketId = findViewById(R.id.tvTicketId);
            tvType = findViewById(R.id.tvType);
            tvSeverity = findViewById(R.id.tvSeverity);
            tvDescription = findViewById(R.id.tvDescription);
            tvStatus = findViewById(R.id.tvStatus);
            tvLocation = findViewById(R.id.tvLocation);
            tvDateTime = findViewById(R.id.tvDateTime);
            ivClose = findViewById(R.id.ivClose);
        }
    }

    private void setupTicketData() {
        // Get ticket data from intent
        String ticketId = getIntent().getStringExtra("ticket_id");
        String type = getIntent().getStringExtra("type");
        String severity = getIntent().getStringExtra("severity");
        String location = getIntent().getStringExtra("location");
        String dateTime = getIntent().getStringExtra("date_time");
        String description = getIntent().getStringExtra("description");
        String imageName = getIntent().getStringExtra("image_name");
        String imageUrl = getIntent().getStringExtra("image_url"); // Get image URL
        String status = getIntent().getStringExtra("status");
        String reason = getIntent().getStringExtra("reason");

        // Create ticket object
        ticket = new Ticket(ticketId, type, severity, location, description, dateTime, imageName);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            ticket.setImageUrl(imageUrl);
        }
        if (status != null && !status.isEmpty()) {
            try {
                ticket.setStatus(Ticket.TicketStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                ticket.setStatus(Ticket.TicketStatus.PENDING);
            }
        }
        if (reason != null && !reason.isEmpty()) {
            ticket.setReason(reason);
        }

        // Set ticket data
        tvTicketId.setText(ticket.getId());
        tvType.setText(ticket.getType());
        tvLocation.setText(ticket.getLocation());
        tvDateTime.setText(ticket.getDateTime());
        tvDescription.setText(ticket.getDescription());
        tvSeverity.setText(ticket.getSeverity());

        // Set severity background
        setSeverityBackground(ticket.getSeverity());

        // Set status (only for citizen view)
        if (!isEngineerView && tvStatus != null && ticket.getStatus() != null) {
            String statusText = ticket.getStatus().toString();
            tvStatus.setText(statusText);
        }

        // Set ticket image
        setTicketImage();
    }

    private void setSeverityBackground(String severity) {
        int backgroundRes;
        switch (severity.toLowerCase()) {
            case "high":
                backgroundRes = R.drawable.bg_severity_high;
                break;
            case "medium":
                backgroundRes = R.drawable.bg_severity_medium;
                break;
            case "low":
                backgroundRes = R.drawable.bg_severity_low;
                break;
            default:
                backgroundRes = R.drawable.bg_severity_medium;
        }
        tvSeverity.setBackgroundResource(backgroundRes);
    }

    private void setTicketImage() {
        // First try to load from URL if available
        if (ticket.getImageUrl() != null && !ticket.getImageUrl().isEmpty()) {
            loadImageFromUrl(ticket.getImageUrl());
            return;
        }
        
        // Fallback to drawable resource (for old tickets)
        int imageResource = getResources().getIdentifier(
                ticket.getImageName(),
                "drawable",
                getPackageName()
        );
        if (imageResource != 0) {
            ivTicketImage.setImageResource(imageResource);
        }
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
                android.util.Log.e("TicketDetail", "Error loading image from URL: " + e.getMessage(), e);
                // Fallback to placeholder or default image
                runOnUiThread(() -> {
                    // Could set a placeholder image here if needed
                });
            }
        }).start();
    }

    private void setupListeners() {
        if (isEngineerView) {
            // Engineer view - setup button listeners
            if (ivBack != null) {
                ivBack.setOnClickListener(v -> finish());
            }

            if (btnAccept != null) {
                btnAccept.setOnClickListener(v -> showReasonDialog("Accept", "Accept Ticket", (reason) -> {
                    ticket.setStatus(Ticket.TicketStatus.ACCEPTED);
                    ticket.setReason(reason);
                    TicketManager.getInstance().updateTicket(ticket);
                    
                    // Show reason in UI
                    if (labelReason != null) labelReason.setVisibility(View.VISIBLE);
                    if (tvReason != null) {
                        tvReason.setVisibility(View.VISIBLE);
                        tvReason.setText(reason);
                    }
                    
                    // Hide buttons
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnSpam.setVisibility(View.GONE);
                    
                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ticket_id", ticket.getId());
                    resultIntent.putExtra("new_status", "ACCEPTED");
                    resultIntent.putExtra("reason", reason);
                    setResult(RESULT_OK, resultIntent);
                    
                    Toast.makeText(this, "Ticket Accepted", Toast.LENGTH_SHORT).show();
                }));
            }

            if (btnReject != null) {
                btnReject.setOnClickListener(v -> showReasonDialog("Reject", "Reject Ticket", (reason) -> {
                    ticket.setStatus(Ticket.TicketStatus.REJECTED);
                    ticket.setReason(reason);
                    TicketManager.getInstance().updateTicket(ticket);
                    
                    // Show reason in UI
                    if (labelReason != null) labelReason.setVisibility(View.VISIBLE);
                    if (tvReason != null) {
                        tvReason.setVisibility(View.VISIBLE);
                        tvReason.setText(reason);
                    }
                    
                    // Hide buttons
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnSpam.setVisibility(View.GONE);
                    
                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ticket_id", ticket.getId());
                    resultIntent.putExtra("new_status", "REJECTED");
                    resultIntent.putExtra("reason", reason);
                    setResult(RESULT_OK, resultIntent);
                    
                    Toast.makeText(this, "Ticket Rejected", Toast.LENGTH_SHORT).show();
                }));
            }

            if (btnSpam != null) {
                btnSpam.setOnClickListener(v -> {
                    ticket.setStatus(Ticket.TicketStatus.SPAM);
                    TicketManager.getInstance().updateTicket(ticket);
                    
                    // Hide buttons
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnSpam.setVisibility(View.GONE);
                    
                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("ticket_id", ticket.getId());
                    resultIntent.putExtra("new_status", "SPAM");
                    setResult(RESULT_OK, resultIntent);
                    
                    Toast.makeText(this, "Marked as Spam", Toast.LENGTH_SHORT).show();
                });
            }

            // Hide buttons if ticket is already processed
            if (ticket != null && ticket.getStatus() != Ticket.TicketStatus.PENDING) {
                if (btnAccept != null) btnAccept.setVisibility(View.GONE);
                if (btnReject != null) btnReject.setVisibility(View.GONE);
                if (btnSpam != null) btnSpam.setVisibility(View.GONE);
            }

            // Show reason if exists
            if (ticket != null && ticket.getReason() != null && !ticket.getReason().isEmpty()) {
                if (labelReason != null) labelReason.setVisibility(View.VISIBLE);
                if (tvReason != null) {
                    tvReason.setVisibility(View.VISIBLE);
                    tvReason.setText(ticket.getReason());
                }
            }
        } else {
            // Citizen view - just close button
            if (ivClose != null) {
                ivClose.setOnClickListener(v -> finish());
            }
        }
    }

    private void showReasonDialog(String action, String title, ReasonCallback callback) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reason, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        android.widget.EditText etReason = dialogView.findViewById(R.id.etReason);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvDialogTitle.setText(title);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                etReason.setError("Please enter a reason");
                return;
            }
            callback.onReasonEntered(reason);
            dialog.dismiss();
        });

        dialog.show();
    }

    interface ReasonCallback {
        void onReasonEntered(String reason);
    }
}
