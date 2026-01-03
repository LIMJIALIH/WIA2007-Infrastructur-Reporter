package com.example.infrastructureproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private Context context;
    private List<Ticket> tickets;
    private OnTicketActionListener listener;
    private boolean isEngineerMode;
    private boolean isCouncilMode;

    public interface OnTicketActionListener {
        void onAccept(Ticket ticket, int position);
        void onReject(Ticket ticket, int position);
        void onSpam(Ticket ticket, int position);
        void onView(Ticket ticket, int position);
        void onDelete(Ticket ticket, int position);
    }

    public TicketAdapter(Context context, OnTicketActionListener listener) {
        this(context, listener, false, false);
    }

    public TicketAdapter(Context context, OnTicketActionListener listener, boolean isEngineerMode) {
        this(context, listener, isEngineerMode, false);
    }
    
    public TicketAdapter(Context context, OnTicketActionListener listener, boolean isEngineerMode, boolean isCouncilMode) {
        this.context = context;
        this.tickets = new ArrayList<>();
        this.listener = listener;
        this.isEngineerMode = isEngineerMode;
        this.isCouncilMode = isCouncilMode;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        notifyDataSetChanged();
    }
    
    public List<Ticket> getTickets() {
        return new ArrayList<>(tickets);
    }

    public void removeTicket(int position) {
        if (position >= 0 && position < tickets.size()) {
            tickets.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isEngineerMode ? R.layout.item_ticket_engineer : R.layout.item_ticket;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.bind(ticket, position);
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    class TicketViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTicketImage;
        TextView tvSeverity;
        TextView tvTicketId;
        TextView tvType;
        TextView tvLocation;
        TextView tvDateTime;
        TextView tvDescription;
        TextView tvStatus;
        Button btnView;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTicketImage = itemView.findViewById(R.id.ivTicketImage);
            tvSeverity = itemView.findViewById(R.id.tvSeverity);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
            tvType = itemView.findViewById(R.id.tvType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnView = itemView.findViewById(R.id.btnView);
        }

        public void bind(Ticket ticket, int position) {
            // Set ticket data
            tvTicketId.setText(ticket.getId());
            tvType.setText(ticket.getType());
            tvLocation.setText(ticket.getLocation());
            tvDateTime.setText(ticket.getDateTime());
            tvDescription.setText(ticket.getDescription());
            tvSeverity.setText(ticket.getSeverity());

            // Set severity badge color
            int severityBg;
            switch (ticket.getSeverity()) {
                case "High":
                    severityBg = R.drawable.bg_severity_high;
                    break;
                case "Medium":
                    severityBg = R.drawable.bg_severity_medium;
                    break;
                case "Low":
                    severityBg = R.drawable.bg_severity_low;
                    break;
                default:
                    severityBg = R.drawable.bg_severity_low;
            }
            tvSeverity.setBackgroundResource(severityBg);

            // Set ticket image from URL or fallback to drawable
            if (ticket.getImageUrl() != null && !ticket.getImageUrl().isEmpty()) {
                loadImageThumbnail(ticket.getImageUrl(), ivTicketImage);
            } else {
                // Fallback to drawable for old tickets
                int imageResource = context.getResources().getIdentifier(
                        ticket.getImageName(),
                        "drawable",
                        context.getPackageName()
                );
                if (imageResource != 0) {
                    ivTicketImage.setImageResource(imageResource);
                } else {
                    ivTicketImage.setImageResource(R.drawable.ic_image_placeholder);
                }
            }

            // Set status or buttons based on mode
            if (isEngineerMode) {
                // Engineer mode - show action buttons
                Button btnAccept = itemView.findViewById(R.id.btnAccept);
                Button btnReject = itemView.findViewById(R.id.btnReject);
                Button btnSpam = itemView.findViewById(R.id.btnSpam);
                Button btnDelete = itemView.findViewById(R.id.btnDelete);

                // Show/hide buttons based on ticket status
                if (ticket.getStatus() == Ticket.TicketStatus.UNDER_REVIEW) {
                    // Pending Review (UNDER_REVIEW) - show action buttons, hide delete
                    if (btnAccept != null) btnAccept.setVisibility(View.VISIBLE);
                    if (btnReject != null) btnReject.setVisibility(View.VISIBLE);
                    if (btnSpam != null) btnSpam.setVisibility(View.VISIBLE);
                    if (btnDelete != null) btnDelete.setVisibility(View.GONE);
                } else {
                    // Completed (ACCEPTED, REJECTED, SPAM) - hide all buttons in list view
                    // Delete button only shows in detail view
                    if (btnAccept != null) btnAccept.setVisibility(View.GONE);
                    if (btnReject != null) btnReject.setVisibility(View.GONE);
                    if (btnSpam != null) btnSpam.setVisibility(View.GONE);
                    if (btnDelete != null) btnDelete.setVisibility(View.GONE);
                }

                if (btnAccept != null) {
                    btnAccept.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAccept(ticket, position);
                        }
                    });
                }

                if (btnReject != null) {
                    btnReject.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onReject(ticket, position);
                        }
                    });
                }

                if (btnSpam != null) {
                    btnSpam.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onSpam(ticket, position);
                        }
                    });
                }

                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDelete(ticket, position);
                        }
                    });
                }
            } else {
                // Citizen/Council mode - show status text
                if (tvStatus != null) {
                    if (ticket.getStatus() != null) {
                        // Use council-specific status text if in council mode
                        String statusText = isCouncilMode 
                            ? "Status: " + ticket.getStatusDisplayTextForCouncil()
                            : "Status: " + ticket.getStatusDisplayText();
                        tvStatus.setText(statusText);
                    } else {
                        tvStatus.setText("Status: Pending");
                    }
                }
            }
            
            // Set view button click listener
            btnView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onView(ticket, position);
                }
            });
        }
    }

    // Load image thumbnail from Supabase URL
    private void loadImageThumbnail(String imageUrl, ImageView imageView) {
        // Set placeholder while loading
        imageView.setImageResource(R.drawable.ic_image_placeholder);
        
        // Load image in background thread
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Keep placeholder on error
            }
        }).start();
    }
}