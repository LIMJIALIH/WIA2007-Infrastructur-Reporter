package com.example.infrastructureproject;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class TicketDetailsDialog extends Dialog {

    private Ticket ticket;
    private Context context;

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

    public TicketDetailsDialog(Context context, Ticket ticket) {
        super(context);
        this.context = context;
        this.ticket = ticket;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_ticket_details);

        // Make dialog transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        initializeViews();
        setupTicketData();
        setupListeners();
    }

    private void initializeViews() {
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

    private void setupTicketData() {
        if (ticket == null) return;

        // Set ticket data
        tvTicketId.setText(ticket.getId());
        tvType.setText(ticket.getType());
        tvLocation.setText(ticket.getLocation());
        tvDateTime.setText(ticket.getDateTime());
        tvDescription.setText(ticket.getDescription());
        tvSeverity.setText(ticket.getSeverity());

        // Set status
        if (ticket.getStatus() != null) {
            String statusText = ticket.getStatus().toString();
            tvStatus.setText(statusText);
        }

        // Set ticket image
        setTicketImage();
    }

//    private void setSeverityBackground(String severity) {
//        int severityBg;
//        switch (severity) {
//            case "High":
//                severityBg = R.drawable.bg_severity_high;
//                break;
//            case "Medium":
//                severityBg = R.drawable.bg_severity_medium;
//                break;
//            case "Low":
//                severityBg = R.drawable.bg_severity_low;
//                break;
//            default:
//                severityBg = R.drawable.bg_severity_low;
//        }
//        tvSeverity.setBackgroundResource(severityBg);
//    }
//
//    private void setStatusBackground(Ticket.TicketStatus status) {
//        int statusBg;
//        switch (status) {
//            case PENDING:
//                statusBg = R.drawable.bg_status_pending;
//                break;
//            case ACCEPTED:
//                statusBg = R.drawable.bg_status_accepted;
//                break;
//            case REJECTED:
//                statusBg = R.drawable.bg_status_rejected;
//                break;
//            case SPAM:
//                statusBg = R.drawable.bg_status_spam;
//                break;
//            default:
//                statusBg = R.drawable.bg_status_pending;
//        }
//        tvStatus.setBackgroundResource(statusBg);
//    }

    private void setTicketImage() {
        int imageResource = context.getResources().getIdentifier(
                ticket.getImageName(),
                "drawable",
                context.getPackageName()
        );
        if (imageResource != 0) {
            ivTicketImage.setImageResource(imageResource);
        }
    }

    private void setupListeners() {
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}