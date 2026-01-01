package com.example.infrastructureproject;

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

public class CouncilDashboardActivity extends AppCompatActivity implements TicketAdapter.OnTicketActionListener {

    // Header views
    private TextView tvDashboardTitle;
    private TextView tvWelcome;
    private android.widget.Button btnLogout;

    // Stat card views
    private TextView tvStatTotalReportsValue;
    private TextView tvStatPendingValue;
    private TextView tvStatHighPriorityValue;
    private TextView tvStatAvgResponseValue;

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
    private TextView tabTotalReports;
    private TextView tabCompleted;
    private TextView tabPending;
    private TextView tabSpam;

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
    private List<Ticket> completedTickets;
    private List<Ticket> pendingTickets;
    private List<Ticket> spamTickets;
    private List<Ticket> currentDisplayedTickets;

    // Adapter
    private TicketAdapter ticketAdapter;

    // Current tab index (0=Total, 1=Completed, 2=Pending, 3=Spam)
    private int currentTabIndex = 2; // Default to Pending

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_council_dashboard);

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
            
            // Display user's full name
            displayUserWelcome();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initializeDataLists() {
        allTickets = new ArrayList<>();
        completedTickets = new ArrayList<>();
        pendingTickets = new ArrayList<>();
        spamTickets = new ArrayList<>();
        currentDisplayedTickets = new ArrayList<>();
    }

    private void initializeViews() {
        // Header
        tvDashboardTitle = findViewById(R.id.tvDashboardTitle);
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);

        // Stat cards
        tvStatTotalReportsValue = findViewById(R.id.tvStatTotalReportsValue);
        tvStatPendingValue = findViewById(R.id.tvStatPendingValue);
        tvStatHighPriorityValue = findViewById(R.id.tvStatHighPriorityValue);
        tvStatAvgResponseValue = findViewById(R.id.tvStatAvgResponseValue);

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
        tabTotalReports = findViewById(R.id.tabTotalReports);
        tabCompleted = findViewById(R.id.tabCompleted);
        tabPending = findViewById(R.id.tabPending);
        tabSpam = findViewById(R.id.tabSpam);

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
        ticketAdapter = new TicketAdapter(this, this, false); // false = council mode
        recyclerViewTickets.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTickets.setAdapter(ticketAdapter);
        recyclerViewTickets.setNestedScrollingEnabled(false);
    }
    
    private void displayUserWelcome() {
        String fullName = SupabaseManager.getCurrentFullName();
        if (fullName != null && !fullName.isEmpty()) {
            tvWelcome.setText("Welcome, " + fullName);
        } else {
            tvWelcome.setText("Welcome, Council Member");
        }
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
            }
        });

        spinnerSeverities.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTickets();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupClickListeners() {
        // Logout button
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Tab clicks
        tabTotalReports.setOnClickListener(v -> switchTab(0));
        tabCompleted.setOnClickListener(v -> switchTab(1));
        tabPending.setOnClickListener(v -> switchTab(2));
        tabSpam.setOnClickListener(v -> switchTab(3));

        // Search functionality
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

        // Filter buttons
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

        // Refresh button
        btnRefresh.setOnClickListener(v -> {
            etSearch.setText("");
            spinnerTypes.setSelection(0);
            spinnerSeverities.setSelection(0);
            isLocationFilterActive = false;
            isDescriptionFilterActive = false;
            updateFilterButtonState(btnFilterLocation, false);
            updateFilterButtonState(btnFilterDescription, false);
            filterTickets();
            Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFilterButtonState(TextView button, boolean isActive) {
        if (isActive) {
            button.setBackgroundResource(R.drawable.filter_button_active);
            button.setTextColor(getResources().getColor(android.R.color.white, null));
        } else {
            button.setBackgroundResource(R.drawable.filter_button_inactive);
            button.setTextColor(getResources().getColor(android.R.color.black, null));
        }
    }

    private void switchTab(int tabIndex) {
        currentTabIndex = tabIndex;
        
        // Update tab styles
        resetTabStyles();
        switch (tabIndex) {
            case 0:
                setTabActive(tabTotalReports);
                break;
            case 1:
                setTabActive(tabCompleted);
                break;
            case 2:
                setTabActive(tabPending);
                break;
            case 3:
                setTabActive(tabSpam);
                break;
        }
        
        filterTickets();
    }

    private void resetTabStyles() {
        setTabInactive(tabTotalReports);
        setTabInactive(tabCompleted);
        setTabInactive(tabPending);
        setTabInactive(tabSpam);
    }

    private void setTabActive(TextView tab) {
        tab.setBackgroundResource(R.drawable.tab_active_background);
        tab.setTextColor(getResources().getColor(android.R.color.white, null));
    }

    private void setTabInactive(TextView tab) {
        tab.setBackgroundResource(R.drawable.tab_inactive_background);
        tab.setTextColor(getResources().getColor(android.R.color.black, null));
    }

    private void loadDashboardData() {
        // Show loading state
        runOnUiThread(() -> {
            tvStatTotalReportsValue.setText("...");
            tvStatPendingValue.setText("...");
            tvStatHighPriorityValue.setText("...");
            tvStatAvgResponseValue.setText("...");
        });
        
        // Fetch all tickets from Supabase
        TicketRepository.getAllTickets(new TicketRepository.FetchTicketsCallback() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                runOnUiThread(() -> {
                    // Update data lists
                    allTickets.clear();
                    completedTickets.clear();
                    pendingTickets.clear();
                    spamTickets.clear();
                    
                    allTickets.addAll(tickets);
                    
                    // Distribute tickets based on status
                    for (Ticket ticket : allTickets) {
                        switch (ticket.getStatus()) {
                            case PENDING:
                                pendingTickets.add(ticket);
                                break;
                            case ACCEPTED:
                                completedTickets.add(ticket);
                                break;
                            case REJECTED:
                            case SPAM:
                                spamTickets.add(ticket);
                                break;
                        }
                    }
                    
                    // Fetch statistics
                    loadStatistics();
                    
                    // Update tab counts
                    updateTabCounts();
                    
                    // Load current tab
                    switchTab(currentTabIndex);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(CouncilDashboardActivity.this, 
                        "Error loading tickets: " + message, Toast.LENGTH_SHORT).show();
                    tvStatTotalReportsValue.setText("0");
                    tvStatPendingValue.setText("0");
                    tvStatHighPriorityValue.setText("0");
                    tvStatAvgResponseValue.setText("N/A");
                });
            }
        });
    }
    
    private void loadStatistics() {
        TicketRepository.getCouncilStatistics(new TicketRepository.CouncilStatsCallback() {
            @Override
            public void onSuccess(int totalReports, int totalPending, int highPriorityPending, String avgResponse) {
                runOnUiThread(() -> {
                    tvStatTotalReportsValue.setText(String.valueOf(totalReports));
                    tvStatPendingValue.setText(String.valueOf(totalPending));
                    tvStatHighPriorityValue.setText(String.valueOf(highPriorityPending));
                    tvStatAvgResponseValue.setText(avgResponse);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(CouncilDashboardActivity.this, 
                        "Error loading statistics: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateTabCounts() {
        tabTotalReports.setText("Total Reports (" + allTickets.size() + ")");
        tabCompleted.setText("Completed (" + completedTickets.size() + ")");
        tabPending.setText("Pending (" + pendingTickets.size() + ")");
        tabSpam.setText("Spam (" + spamTickets.size() + ")");
    }

    private void filterTickets() {
        // Get base list based on current tab
        List<Ticket> baseList;
        switch (currentTabIndex) {
            case 0: baseList = new ArrayList<>(allTickets); break;
            case 1: baseList = new ArrayList<>(completedTickets); break;
            case 2: baseList = new ArrayList<>(pendingTickets); break;
            case 3: baseList = new ArrayList<>(spamTickets); break;
            default: baseList = new ArrayList<>(pendingTickets);
        }

        // Apply filters
        List<Ticket> filtered = baseList.stream()
                .filter(ticket -> {
                    // Search filter
                    String searchQuery = etSearch.getText().toString().toLowerCase().trim();
                    if (!searchQuery.isEmpty()) {
                        boolean matchesSearch = false;
                        if (isLocationFilterActive && ticket.getLocation().toLowerCase().contains(searchQuery)) {
                            matchesSearch = true;
                        }
                        if (isDescriptionFilterActive && ticket.getDescription().toLowerCase().contains(searchQuery)) {
                            matchesSearch = true;
                        }
                        if (!isLocationFilterActive && !isDescriptionFilterActive) {
                            matchesSearch = ticket.getLocation().toLowerCase().contains(searchQuery) ||
                                          ticket.getDescription().toLowerCase().contains(searchQuery) ||
                                          ticket.getType().toLowerCase().contains(searchQuery);
                        }
                        if (!matchesSearch) return false;
                    }

                    // Type filter
                    int typePos = spinnerTypes.getSelectedItemPosition();
                    if (typePos > 0) {
                        String selectedType = ticketTypes[typePos];
                        if (!ticket.getType().equalsIgnoreCase(selectedType)) {
                            return false;
                        }
                    }

                    // Severity filter
                    int severityPos = spinnerSeverities.getSelectedItemPosition();
                    if (severityPos > 0) {
                        String selectedSeverity = severityLevels[severityPos];
                        if (!ticket.getSeverity().equalsIgnoreCase(selectedSeverity)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Update display
        currentDisplayedTickets = filtered;
        ticketAdapter.setTickets(filtered);
        
        // Update ticket count - show just "All Tickets" without count
        tvAllTickets.setText("All Tickets");

        // Show/hide empty state
        if (filtered.isEmpty()) {
            recyclerViewTickets.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
            updateEmptyState();
        } else {
            recyclerViewTickets.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState() {
        switch (currentTabIndex) {
            case 0:
                tvEmptyStateTitle.setText("No Reports");
                tvEmptyStateMessage.setText("There are no reports yet.");
                break;
            case 1:
                tvEmptyStateTitle.setText("No Completed Tickets");
                tvEmptyStateMessage.setText("No tickets have been completed yet.");
                break;
            case 2:
                tvEmptyStateTitle.setText("No Pending Tickets");
                tvEmptyStateMessage.setText("All tickets have been processed!");
                break;
            case 3:
                tvEmptyStateTitle.setText("No Spam Tickets");
                tvEmptyStateMessage.setText("No tickets marked as spam.");
                break;
        }
    }

    @Override
    public void onAccept(Ticket ticket, int position) {
        // Not used in council mode
    }

    @Override
    public void onReject(Ticket ticket, int position) {
        // Not used in council mode
    }

    @Override
    public void onSpam(Ticket ticket, int position) {
        // Not used in council mode
    }

    @Override
    public void onView(Ticket ticket, int position) {
        Intent intent = new Intent(this, CouncilTicketDetailActivity.class);
        intent.putExtra("TICKET_ID", ticket.getId());
        intent.putExtra("TICKET_TYPE", ticket.getType());
        intent.putExtra("TICKET_SEVERITY", ticket.getSeverity());
        intent.putExtra("TICKET_LOCATION", ticket.getLocation());
        intent.putExtra("TICKET_DESCRIPTION", ticket.getDescription());
        intent.putExtra("TICKET_TIMESTAMP", ticket.getDateTime());
        intent.putExtra("TICKET_IMAGE", ticket.getImageResId(this));
        intent.putExtra("TICKET_IMAGE_URL", ticket.getImageUrl()); // Pass image URL
        intent.putExtra("TICKET_USERNAME", ticket.getUsername());
        startActivity(intent);
    }

    @Override
    public void onDelete(Ticket ticket, int position) {
        // Not used in council mode
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data from TicketManager to get updated counts
        initializeDataLists();
        // Refresh dashboard with updated data
        loadDashboardData();
        filterTickets();
    }
}
