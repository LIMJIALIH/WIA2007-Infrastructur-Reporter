package com.example.infrastructureproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.infrastructurereporter.R;
import com.google.android.material.textfield.TextInputEditText;

public class TicketDetailActivity extends AppCompatActivity {

    private ImageView ivBack;
    private ImageView ivTicketImage;
    private TextView tvTicketId;
    private TextView tvSeverity;
    private TextView tvType;
    private TextView tvLocation;
    private TextView tvDateTime;
    private TextView tvDescription;
    private TextView tvReason;
    private TextView labelReason;
    private Button btnAccept;
    private Button btnReject;
    private Button btnSpam;

    private Ticket ticket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        initializeViews();
        loadTicketData();
        setupListeners();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        ivTicketImage = findViewById(R.id.ivTicketImage);
        tvTicketId = findViewById(R.id.tvTicketId);
        tvSeverity = findViewById(R.id.tvSeverity);
        tvType = findViewById(R.id.tvType);
        tvLocation = findViewById(R.id.tvLocation);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvDescription = findViewById(R.id.tvDescription);
        tvReason = findViewById(R.id.tvReason);
        labelReason = findViewById(R.id.labelReason);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);
        btnSpam = findViewById(R.id.btnSpam);
    }

    private void loadTicketData() {
        // Get ticket data from intent
        String ticketId = getIntent().getStringExtra("ticket_id");
        String type = getIntent().getStringExtra("type");
        String severity = getIntent().getStringExtra("severity");
        String location = getIntent().getStringExtra("location");
        String dateTime = getIntent().getStringExtra("date_time");
        String description = getIntent().getStringExtra("description");
        String imageName = getIntent().getStringExtra("image_name");
        String status = getIntent().getStringExtra("status");
        String reason = getIntent().getStringExtra("reason");

        // Create ticket object
        ticket = new Ticket(ticketId, type, severity, location, description, dateTime, imageName);
        if (status != null && !status.isEmpty()) {
            ticket.setStatus(Ticket.TicketStatus.valueOf(status));
        }
        if (reason != null && !reason.isEmpty()) {
            ticket.setReason(reason);
        }

        // Display ticket data
        tvTicketId.setText(ticket.getId());
        tvType.setText(ticket.getType());
        tvSeverity.setText(ticket.getSeverity());
        tvLocation.setText(ticket.getLocation());
        tvDateTime.setText(ticket.getDateTime());
        tvDescription.setText(ticket.getDescription());
        
        // Display reason if available
        if (ticket.getReason() != null && !ticket.getReason().isEmpty()) {
            labelReason.setVisibility(View.VISIBLE);
            tvReason.setVisibility(View.VISIBLE);
            tvReason.setText(ticket.getReason());
        } else {
            labelReason.setVisibility(View.GONE);
            tvReason.setVisibility(View.GONE);
        }
        
        // Load image
        int imageResId = ticket.getImageResId(this);
        if (imageResId != 0) {
            ivTicketImage.setImageResource(imageResId);
        }

        // Set severity background
        setSeverityBackground(ticket.getSeverity());

        // Show/hide buttons based on status
        updateButtonVisibility();
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

    private void updateButtonVisibility() {
        // Hide buttons if ticket is already processed
        if (ticket.getStatus() != Ticket.TicketStatus.PENDING) {
            btnAccept.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnSpam.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnAccept.setOnClickListener(v -> showReasonDialog("Accept", "Accept Ticket", (reason) -> {
            ticket.setStatus(Ticket.TicketStatus.ACCEPTED);
            ticket.setReason(reason);
            // Return result with ticket data
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ticket_id", ticket.getId());
            resultIntent.putExtra("new_status", "ACCEPTED");
            resultIntent.putExtra("reason", reason);
            setResult(RESULT_OK, resultIntent);
            Toast.makeText(this, "Ticket Accepted", Toast.LENGTH_SHORT).show();
            finish();
        }));

        btnReject.setOnClickListener(v -> showReasonDialog("Reject", "Reject Ticket", (reason) -> {
            ticket.setStatus(Ticket.TicketStatus.REJECTED);
            ticket.setReason(reason);
            // Return result with ticket data
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ticket_id", ticket.getId());
            resultIntent.putExtra("new_status", "REJECTED");
            resultIntent.putExtra("reason", reason);
            setResult(RESULT_OK, resultIntent);
            Toast.makeText(this, "Ticket Rejected", Toast.LENGTH_SHORT).show();
            finish();
        }));

        btnSpam.setOnClickListener(v -> {
            // No reason needed for spam
            ticket.setStatus(Ticket.TicketStatus.SPAM);
            // Return result with ticket data
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ticket_id", ticket.getId());
            resultIntent.putExtra("new_status", "SPAM");
            setResult(RESULT_OK, resultIntent);
            Toast.makeText(this, "Marked as Spam", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showReasonDialog(String action, String title, ReasonCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reason, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputEditText etReason = dialogView.findViewById(R.id.etReason);
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
