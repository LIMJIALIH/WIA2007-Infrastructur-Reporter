package com.example.infrastructureproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.infrastructurereporter.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EngineerDashboardActivity extends AppCompatActivity implements TicketAdapter.OnTicketActionListener {

    // Header views
    private TextView tvDashboardTitle;
    private TextView tvWelcome;
    private android.widget.Button btnLogout;

    // Stat card views
    private TextView tvStatNewTodayValue;
    private TextView tvStatThisWeekValue;
    private TextView tvStatAvgResponseValue;
    private TextView tvStatHighPriorityValue;

    // Search and filter views
    private EditText etSearch;
    private ImageView ivSearch;
    private ImageView ivFilter;
    private Spinner spinnerTypes;
    private Spinner spinnerSeverities;
    
    // Search filter buttons
    private TextView btnFilterLocation;
    private TextView btnFilterDescription;
    private boolean isLocationFilterActive = false;
    private boolean isDescriptionFilterActive = false;

    // Tabs
    private TextView tabPendingReview;
    private TextView tabRejected;
    private TextView tabSpam;
    private TextView tabAccepted;

    // Content area
    private ConstraintLayout contentArea;
    private ConstraintLayout emptyStateContainer;
    private RecyclerView recyclerViewTickets;
    private ImageView ivEmptyState;
    private TextView tvEmptyStateTitle;
    private TextView tvEmptyStateMessage;

    // Refresh button
    private LinearLayout btnRefresh;
    private TextView tvAllTickets;

    // Data arrays
    private String[] ticketTypes;
    private String[] severityLevels;

    // Ticket data
    private List<Ticket> allTickets;
    private List<Ticket> pendingTickets;
    private List<Ticket> acceptedTickets;
    private List<Ticket> rejectedTickets;
    private List<Ticket> spamTickets;
    private List<Ticket> currentDisplayedTickets;

    // Adapter
    private TicketAdapter ticketAdapter;

    // Current tab index
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_engineer_dashboard);

            // Initialize views
            initializeViews();

            // Initialize data lists
            initializeDataLists();

            // Setup data
            setupData();

            // Setup RecyclerView
            setupRecyclerView();

            // Setup spinners
            setupSpinners();

            // Setup click listeners
            setupClickListeners();

            // Load initial data
            loadDashboardData();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Navigate back to login on error
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initializeDataLists() {
        allTickets = new ArrayList<>();
        pendingTickets = new ArrayList<>();
        acceptedTickets = new ArrayList<>();
        rejectedTickets = new ArrayList<>();
        spamTickets = new ArrayList<>();
        currentDisplayedTickets = new ArrayList<>();

        // Load mock data - COMMENTED OUT, use Supabase TicketRepository instead
        // allTickets = MockDataGenerator.generateMockTickets();

        // Initially all tickets are pending
        // TODO: Load tickets from Supabase using TicketRepository
        // for (Ticket ticket : allTickets) {
        //     pendingTickets.add(ticket);
        // }
    }

    private void initializeViews() {
        // Header
        tvDashboardTitle = findViewById(R.id.tvDashboardTitle);
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);

        // Stat cards
        tvStatNewTodayValue = findViewById(R.id.tvStatNewTodayValue);
        tvStatThisWeekValue = findViewById(R.id.tvStatThisWeekValue);
        tvStatAvgResponseValue = findViewById(R.id.tvStatAvgResponseValue);
        tvStatHighPriorityValue = findViewById(R.id.tvStatHighPriorityValue);

        // Search and filter
        etSearch = findViewById(R.id.etSearch);
        ivSearch = findViewById(R.id.ivSearch);
        ivFilter = findViewById(R.id.ivFilter);
        spinnerTypes = findViewById(R.id.spinnerTypes);
        spinnerSeverities = findViewById(R.id.spinnerSeverities);
        
        // Filter buttons
        btnFilterLocation = findViewById(R.id.btnFilterLocation);
        btnFilterDescription = findViewById(R.id.btnFilterDescription);

        // Tabs
        tabPendingReview = findViewById(R.id.tabPendingReview);
        tabRejected = findViewById(R.id.tabRejected);
        tabSpam = findViewById(R.id.tabSpam);
        tabAccepted = findViewById(R.id.tabAccepted);

        // Content area
        contentArea = findViewById(R.id.contentArea);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        recyclerViewTickets = findViewById(R.id.recyclerViewTickets);
        ivEmptyState = findViewById(R.id.ivEmptyState);
        tvEmptyStateTitle = findViewById(R.id.tvEmptyStateTitle);
        tvEmptyStateMessage = findViewById(R.id.tvEmptyStateMessage);

        // Refresh button
        btnRefresh = findViewById(R.id.btnRefresh);
        tvAllTickets = findViewById(R.id.tvAllTickets);
    }

    private void setupRecyclerView() {
        ticketAdapter = new TicketAdapter(this, this, true); // true = engineer mode
        recyclerViewTickets.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTickets.setAdapter(ticketAdapter);
        recyclerViewTickets.setNestedScrollingEnabled(false);
    }

    private void setupData() {
        // Initialize ticket types
        ticketTypes = new String[]{
                getString(R.string.all_types),
                getString(R.string.category_road),
                getString(R.string.category_utilities),
                getString(R.string.category_facilities),
                getString(R.string.category_environment),
                getString(R.string.type_other)
        };

        // Initialize severity levels
        severityLevels = new String[]{
                getString(R.string.all_severities),
                getString(R.string.severity_low),
                getString(R.string.severity_medium),
                getString(R.string.severity_high)
        };
    }

    private void setupSpinners() {
        // Setup Types Spinner
        ArrayAdapter<String> typesAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                ticketTypes
        );
        typesAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerTypes.setAdapter(typesAdapter);

        // Setup Severities Spinner
        ArrayAdapter<String> severitiesAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                severityLevels
        );
        severitiesAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSeverities.setAdapter(severitiesAdapter);

        // Set spinner listeners
        spinnerTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTickets();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        spinnerSeverities.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTickets();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupClickListeners() {
        // Logout button
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                logout();
            });
        }

        // Refresh button
        btnRefresh.setOnClickListener(v -> refreshDashboard());

        // Tab click listeners
        tabPendingReview.setOnClickListener(v -> selectTab(0));
        tabRejected.setOnClickListener(v -> selectTab(1));
        tabSpam.setOnClickListener(v -> selectTab(2));
        tabAccepted.setOnClickListener(v -> selectTab(3));

        // Filter icon click
        ivFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Advanced filter options", Toast.LENGTH_SHORT).show();
        });

        // Search text watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTickets();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Filter button click listeners
        btnFilterLocation.setOnClickListener(v -> {
            isLocationFilterActive = !isLocationFilterActive;
            updateFilterButtonState(btnFilterLocation, isLocationFilterActive);
            filterTickets();
        });
        
        btnFilterDescription.setOnClickListener(v -> {
            isDescriptionFilterActive = !isDescriptionFilterActive;
            updateFilterButtonState(btnFilterDescription, isDescriptionFilterActive);
            filterTickets();
        });
    }

    private void loadDashboardData() {
        // Update statistics
        updateStatisticsFromTickets();

        // Set welcome message with username
        String fullName = SupabaseManager.getCurrentFullName();
        if (fullName != null && !fullName.isEmpty()) {
            if (tvWelcome != null) {
                tvWelcome.setText("Welcome, " + fullName);
            }
        }

        // Update tab counts
        updateTabCounts(pendingTickets.size(), rejectedTickets.size(),
                spamTickets.size(), acceptedTickets.size());

        // Load tickets for current tab
        selectTab(currentTabIndex);
    }

    private void updateStatisticsFromTickets() {
        // Count tickets from today
        int newToday = (int) pendingTickets.stream()
                .filter(t -> t.getDateTime().contains("2025-11-16"))
                .count();

        // Count tickets from this week
        int thisWeek = pendingTickets.size();

        // Count high priority tickets
        int highPriority = (int) pendingTickets.stream()
                .filter(t -> t.getSeverity().equals("High"))
                .count();

        tvStatNewTodayValue.setText(String.valueOf(newToday));
        tvStatThisWeekValue.setText(String.valueOf(thisWeek));
        tvStatAvgResponseValue.setText("< 2 hours");
        tvStatHighPriorityValue.setText(String.valueOf(highPriority));
    }

    private void selectTab(int tabIndex) {
        currentTabIndex = tabIndex;

        // Reset all tabs to unselected state
        tabPendingReview.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabPendingReview.setTextColor(getResources().getColor(R.color.text_secondary, null));

        tabRejected.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabRejected.setTextColor(getResources().getColor(R.color.text_secondary, null));

        tabSpam.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabSpam.setTextColor(getResources().getColor(R.color.text_secondary, null));

        tabAccepted.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabAccepted.setTextColor(getResources().getColor(R.color.text_secondary, null));

        // Set selected tab
        TextView selectedTab;
        List<Ticket> ticketsToShow;

        switch (tabIndex) {
            case 0:
                selectedTab = tabPendingReview;
                ticketsToShow = pendingTickets;
                break;
            case 1:
                selectedTab = tabRejected;
                ticketsToShow = rejectedTickets;
                break;
            case 2:
                selectedTab = tabSpam;
                ticketsToShow = spamTickets;
                break;
            case 3:
                selectedTab = tabAccepted;
                ticketsToShow = acceptedTickets;
                break;
            default:
                selectedTab = tabPendingReview;
                ticketsToShow = pendingTickets;
        }

        selectedTab.setBackgroundResource(R.drawable.bg_tab_selected);
        selectedTab.setTextColor(getResources().getColor(R.color.text_primary, null));

        // Load tickets for selected tab
        loadTicketsForTab(ticketsToShow);
    }

    private void loadTicketsForTab(List<Ticket> tickets) {
        currentDisplayedTickets = new ArrayList<>(tickets);
        filterTickets();
    }

    private void updateTabCounts(int pendingCount, int rejectedCount, int spamCount, int acceptedCount) {
        tabPendingReview.setText(String.format(getString(R.string.pending_review), pendingCount));
        tabRejected.setText(String.format(getString(R.string.rejected), rejectedCount));
        tabSpam.setText(String.format(getString(R.string.spam), spamCount));
        tabAccepted.setText(String.format(getString(R.string.accepted), acceptedCount));
    }

    private void showEmptyState() {
        emptyStateContainer.setVisibility(View.VISIBLE);
        recyclerViewTickets.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateContainer.setVisibility(View.GONE);
        recyclerViewTickets.setVisibility(View.VISIBLE);
    }

    private void filterTickets() {
        String selectedType = spinnerTypes.getSelectedItem().toString();
        String selectedSeverity = spinnerSeverities.getSelectedItem().toString();
        String searchQuery = etSearch.getText().toString().toLowerCase();

        List<Ticket> filteredTickets = new ArrayList<>(currentDisplayedTickets);

        // Filter by type
        if (!selectedType.equals(getString(R.string.all_types))) {
            filteredTickets = filteredTickets.stream()
                    .filter(t -> t.getType().equals(selectedType))
                    .collect(Collectors.toList());
        }

        // Filter by severity
        if (!selectedSeverity.equals(getString(R.string.all_severities))) {
            filteredTickets = filteredTickets.stream()
                    .filter(t -> t.getSeverity().equals(selectedSeverity))
                    .collect(Collectors.toList());
        }

        // Filter by search query with location/description filter buttons
        if (!searchQuery.isEmpty()) {
            // Determine which fields to search based on button states
            boolean searchLocation = isLocationFilterActive;
            boolean searchDescription = isDescriptionFilterActive;
            
            // If neither button is active, search both (default behavior)
            if (!searchLocation && !searchDescription) {
                searchLocation = true;
                searchDescription = true;
            }
            
            final boolean finalSearchLocation = searchLocation;
            final boolean finalSearchDescription = searchDescription;
            
            filteredTickets = filteredTickets.stream()
                    .filter(t -> {
                        boolean matches = false;
                        if (finalSearchLocation) {
                            matches = matches || t.getLocation().toLowerCase().contains(searchQuery);
                        }
                        if (finalSearchDescription) {
                            matches = matches || t.getDescription().toLowerCase().contains(searchQuery);
                        }
                        return matches;
                    })
                    .collect(Collectors.toList());
        }

        // Update adapter
        if (filteredTickets.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            ticketAdapter.setTickets(filteredTickets);
        }
    }

    private void refreshDashboard() {
        // Reload data
        initializeDataLists();
        loadDashboardData();
        Toast.makeText(this, "Dashboard refreshed", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        SupabaseManager.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void updateFilterButtonState(TextView button, boolean isActive) {
        if (isActive) {
            // Selected state: green background, white text
            button.setBackgroundResource(R.drawable.bg_filter_button_selected);
            button.setTextColor(getResources().getColor(R.color.white, null));
        } else {
            // Unselected state: white background with border, gray text
            button.setBackgroundResource(R.drawable.bg_filter_button_unselected);
            button.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }
    }

    private void showReasonDialog(String title, String message, ReasonCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_reason, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        com.google.android.material.textfield.TextInputEditText etReason = dialogView.findViewById(R.id.etReason);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

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

    // Ticket action callbacks
    @Override
    public void onAccept(Ticket ticket, int position) {
        showReasonDialog("Accept Ticket", "Please provide a reason for accepting this ticket:", (reason) -> {
            ticket.setStatus(Ticket.TicketStatus.ACCEPTED);
            ticket.setReason(reason);
            pendingTickets.remove(ticket);
            acceptedTickets.add(ticket);

            ticketAdapter.removeTicket(position);
            updateTabCounts(pendingTickets.size(), rejectedTickets.size(),
                    spamTickets.size(), acceptedTickets.size());
            updateStatisticsFromTickets();

            Toast.makeText(this, "Ticket " + ticket.getId() + " accepted", Toast.LENGTH_SHORT).show();

            if (ticketAdapter.getItemCount() == 0) {
                showEmptyState();
            }
        });
    }

    @Override
    public void onReject(Ticket ticket, int position) {
        showReasonDialog("Reject Ticket", "Please provide a reason for rejecting this ticket:", (reason) -> {
            ticket.setStatus(Ticket.TicketStatus.REJECTED);
            ticket.setReason(reason);
            pendingTickets.remove(ticket);
            rejectedTickets.add(ticket);

            ticketAdapter.removeTicket(position);
            updateTabCounts(pendingTickets.size(), rejectedTickets.size(),
                    spamTickets.size(), acceptedTickets.size());
            updateStatisticsFromTickets();

            Toast.makeText(this, "Ticket " + ticket.getId() + " rejected", Toast.LENGTH_SHORT).show();

            if (ticketAdapter.getItemCount() == 0) {
                showEmptyState();
            }
        });
    }

    @Override
    public void onSpam(Ticket ticket, int position) {
        ticket.setStatus(Ticket.TicketStatus.SPAM);
        pendingTickets.remove(ticket);
        spamTickets.add(ticket);

        ticketAdapter.removeTicket(position);
        updateTabCounts(pendingTickets.size(), rejectedTickets.size(),
                spamTickets.size(), acceptedTickets.size());
        updateStatisticsFromTickets();

        Toast.makeText(this, "Ticket " + ticket.getId() + " marked as spam", Toast.LENGTH_SHORT).show();

        if (ticketAdapter.getItemCount() == 0) {
            showEmptyState();
        }
    }

    @Override
    public void onView(Ticket ticket, int position) {
        // Open ticket detail activity
        Intent intent = new Intent(this, TicketDetailActivity.class);
        intent.putExtra("ticket_id", ticket.getId());
        intent.putExtra("type", ticket.getType());
        intent.putExtra("severity", ticket.getSeverity());
        intent.putExtra("location", ticket.getLocation());
        intent.putExtra("date_time", ticket.getDateTime());
        intent.putExtra("description", ticket.getDescription());
        intent.putExtra("image_name", ticket.getImageName());
        intent.putExtra("status", ticket.getStatus().name());
        intent.putExtra("reason", ticket.getReason());
        intent.putExtra("citizen_view", false); // Engineer view!
        startActivityForResult(intent, 100);
    }

    @Override
    public void onDelete(Ticket ticket, int position) {
        // Show delete confirmation dialog
        new AlertDialog.Builder(this)
            .setTitle("Delete Ticket")
            .setMessage("Are you sure you want to delete ticket " + ticket.getId() + "?")
            .setPositiveButton("DELETE", (dialog, which) -> {
                // Remove from all lists
                pendingTickets.remove(ticket);
                acceptedTickets.remove(ticket);
                rejectedTickets.remove(ticket);
                spamTickets.remove(ticket);
                allTickets.remove(ticket);
                
                // Remove from TicketManager
                TicketManager.getInstance().deleteTicket(ticket.getId());
                
                // Refresh current tab
                selectTab(currentTabIndex);
                
                Toast.makeText(this, "Ticket " + ticket.getId() + " deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Get ticket details from result
            String ticketId = data.getStringExtra("ticket_id");
            String newStatus = data.getStringExtra("new_status");
            String reason = data.getStringExtra("reason");

            if (ticketId != null && newStatus != null) {
                // Find and update the ticket in the appropriate list
                Ticket ticketToUpdate = findTicketById(ticketId);
                if (ticketToUpdate != null) {
                    // Remove from current list
                    pendingTickets.remove(ticketToUpdate);
                    acceptedTickets.remove(ticketToUpdate);
                    rejectedTickets.remove(ticketToUpdate);
                    spamTickets.remove(ticketToUpdate);

                    // Update ticket status
                    ticketToUpdate.setStatus(Ticket.TicketStatus.valueOf(newStatus));
                    if (reason != null) {
                        ticketToUpdate.setReason(reason);
                    }

                    // Add to new list based on status
                    switch (newStatus) {
                        case "ACCEPTED":
                            acceptedTickets.add(ticketToUpdate);
                            Toast.makeText(this, "Ticket " + ticketId + " accepted", Toast.LENGTH_SHORT).show();
                            break;
                        case "REJECTED":
                            rejectedTickets.add(ticketToUpdate);
                            Toast.makeText(this, "Ticket " + ticketId + " rejected", Toast.LENGTH_SHORT).show();
                            break;
                        case "SPAM":
                            spamTickets.add(ticketToUpdate);
                            Toast.makeText(this, "Ticket " + ticketId + " marked as spam", Toast.LENGTH_SHORT).show();
                            break;
                    }

                    // Update UI
                    updateTabCounts(pendingTickets.size(), rejectedTickets.size(),
                            spamTickets.size(), acceptedTickets.size());
                    updateStatisticsFromTickets();
                    refreshTicketLists();
                }
            }
        }
    }

    private Ticket findTicketById(String ticketId) {
        // Search in all lists
        for (Ticket ticket : allTickets) {
            if (ticket.getId().equals(ticketId)) {
                return ticket;
            }
        }
        return null;
    }

    private void refreshTicketLists() {
        // Re-filter and display current tab
        switch (currentTabIndex) {
            case 0:
                selectTab(0);
                break;
            case 1:
                selectTab(1);
                break;
            case 2:
                selectTab(2);
                break;
            case 3:
                selectTab(3);
                break;
        }
    }
}
