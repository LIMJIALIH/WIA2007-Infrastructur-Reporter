package com.example.infrastructureproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.infrastructurereporter.R;

import java.util.ArrayList;
import java.util.List;

public class TicketsDialogFragment extends DialogFragment {

    public static final String TAG = "TicketsDialogFragment";
    private static final String ARG_REPORT_TYPE = "report_type";

    public static TicketsDialogFragment newInstance(String reportType) {
        TicketsDialogFragment fragment = new TicketsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REPORT_TYPE, reportType);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_tickets_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String reportTypeString = getArguments() != null ? getArguments().getString(ARG_REPORT_TYPE, "Unknown") : "Unknown";

        // 1. Set the dialog title
        TextView dialogTitle = view.findViewById(R.id.dialogTitle);
        dialogTitle.setText(reportTypeString + " Reports");

        // 2. Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTickets);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get tickets from TicketManager
        List<Ticket> allTickets = TicketManager.getInstance().getAllTickets();
        List<Ticket> filteredTickets = filterTickets(allTickets, reportTypeString);

        // 3. Create the Adapter with display-only listener
        TicketAdapter.OnTicketActionListener displayOnlyListener = new TicketAdapter.OnTicketActionListener() {
            @Override
            public void onAccept(Ticket ticket, int position) { /* Do nothing */ }
            @Override
            public void onReject(Ticket ticket, int position) { /* Do nothing */ }
            @Override
            public void onSpam(Ticket ticket, int position) { /* Do nothing */ }
            @Override
            public void onDelete(Ticket ticket, int position) { /* Do nothing */ }
            @Override
            public void onView(Ticket ticket, int position) {
                // Open ticket details activity
                android.content.Intent intent = new android.content.Intent(getContext(), TicketDetailActivity.class);
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
                dismiss();
            }
        };

        TicketAdapter adapter = new TicketAdapter(getContext(), displayOnlyListener);
        adapter.setTickets(filteredTickets);

        recyclerView.setAdapter(adapter);

        // 4. Set Close Button Listener
        Button closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Filters the list of tickets based on the report type string.
     */
    private List<Ticket> filterTickets(List<Ticket> allTickets, String typeString) {
        if (typeString.equalsIgnoreCase("Total")) {
            return allTickets;
        }

        List<Ticket> filtered = new ArrayList<>();

        for (Ticket ticket : allTickets) {
            if (typeString.equalsIgnoreCase("Pending") && ticket.getStatus() == Ticket.TicketStatus.PENDING) {
                filtered.add(ticket);
            } else if (typeString.equalsIgnoreCase("Accepted") && ticket.getStatus() == Ticket.TicketStatus.ACCEPTED) {
                filtered.add(ticket);
            } else if (typeString.equalsIgnoreCase("Rejected") && ticket.getStatus() == Ticket.TicketStatus.REJECTED) {
                filtered.add(ticket);
            } else if (typeString.equalsIgnoreCase("Spam") && ticket.getStatus() == Ticket.TicketStatus.SPAM) {
                filtered.add(ticket);
            } else if (typeString.equalsIgnoreCase("Under Review") && ticket.getStatus() == Ticket.TicketStatus.UNDER_REVIEW) {
                filtered.add(ticket);
            }
        }

        return filtered;
    }

}
