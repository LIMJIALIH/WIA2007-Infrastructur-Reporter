package com.example.infrastructureproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.infrastructurereporter.R;

import java.util.ArrayList;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private Context context;
    private List<Ticket> tickets;
    private OnTicketActionListener listener;

    public interface OnTicketActionListener {
        void onAccept(Ticket ticket, int position);
        void onReject(Ticket ticket, int position);
        void onSpam(Ticket ticket, int position);
        void onView(Ticket ticket, int position);
    }

    public TicketAdapter(Context context, OnTicketActionListener listener) {
        this.context = context;
        this.tickets = new ArrayList<>();
        this.listener = listener;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        notifyDataSetChanged();
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_ticket, parent, false);
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
        Button btnAccept;
        Button btnReject;
        Button btnSpam;
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
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnSpam = itemView.findViewById(R.id.btnSpam);
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

            // Set ticket image
            int imageResource = context.getResources().getIdentifier(
                    ticket.getImageName(),
                    "drawable",
                    context.getPackageName()
            );
            if (imageResource != 0) {
                ivTicketImage.setImageResource(imageResource);
            }

            // Set button click listeners
            btnAccept.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAccept(ticket, position);
                }
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(ticket, position);
                }
            });

            btnSpam.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSpam(ticket, position);
                }
            });
            
            btnView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onView(ticket, position);
                }
            });
        }
    }
}