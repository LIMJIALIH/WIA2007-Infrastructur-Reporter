package com.example.infrastructureproject;

import android.os.Handler;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView myReportsButton;
    private TextView newReportsButton;
    private CardView selectionBackground;
    private PopupWindow popupWindow;
    private MaterialButton logoutButton;
    private MaterialButton refreshButton;
    private TextView totalReportsNumber;
    private TextView card2Number; // Pending
    private TextView card3Number; // Rejected
    private TextView card4Number; // Accepted
    private RecyclerView reportsRecyclerView;
    private TicketAdapter ticketAdapter;
    private CardView reportsContainer;
    private NestedScrollView reportsContainer2;
    private HuggingFaceRouterService huggingFaceRouterService; // ← CHANGE THIS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize HuggingFaceRouterService
        huggingFaceRouterService = new HuggingFaceRouterService(); // ← CHANGE THIS

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        initializeViews();

        // Initialize toggle buttons
        initializeToggleButtons();

        // Initialize logout button
        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogout();
            }
        });

        // Setup RecyclerView for My Reports
        setupRecyclerView();

        // Load initial data
        updateDashboardCounts();

        // Optional: Test HuggingFace connection
        testHuggingFaceConnection();
    }

    // Add this method to test connection
    private void testHuggingFaceConnection() {
        Toast.makeText(this, "Hugging Face AI initialized", Toast.LENGTH_SHORT).show();
    }

    private void initializeViews() {
        // Card count TextViews
        totalReportsNumber = findViewById(R.id.totalReportsNumber);
        card2Number = findViewById(R.id.card2Number);
        card3Number = findViewById(R.id.card3Number);
        card4Number = findViewById(R.id.card4Number);

        // Refresh button
        refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshMyReports();
            }
        });

        // Reports containers
        reportsContainer = findViewById(R.id.reportsContainer);
        reportsContainer2 = findViewById(R.id.reportsContainer2);

        // Initialize toggle buttons (these will be set in initializeToggleButtons)
        myReportsButton = findViewById(R.id.myReportsButton);
        newReportsButton = findViewById(R.id.newReportsButton);
        selectionBackground = findViewById(R.id.selectionBackground);
    }

    private void setupRecyclerView() {
        reportsRecyclerView = new RecyclerView(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        reportsRecyclerView.setLayoutManager(layoutManager);

        // Create adapter with empty listener (read-only view for citizen)
        TicketAdapter.OnTicketActionListener readOnlyListener = new TicketAdapter.OnTicketActionListener() {
            @Override
            public void onAccept(Ticket ticket, int position) {}

            @Override
            public void onReject(Ticket ticket, int position) {}

            @Override
            public void onSpam(Ticket ticket, int position) {}

            @Override
            public boolean shouldShowActionButtons() {
                return false; // Hide buttons for citizen
            }
        };

        ticketAdapter = new TicketAdapter(this, readOnlyListener);
        reportsRecyclerView.setAdapter(ticketAdapter);

        // Add RecyclerView to reports container
        LinearLayout containerLayout = (LinearLayout) reportsContainer.getChildAt(0);
        containerLayout.removeAllViews();
        containerLayout.addView(reportsRecyclerView);
    }

    private void refreshMyReports() {
        // Get all tickets for this citizen (in real app, filter by user)
        List<Ticket> allTickets = TicketManager.getInstance().getAllTickets();
        ticketAdapter.setTickets(allTickets);
        updateDashboardCounts();
        Toast.makeText(this, "Reports refreshed", Toast.LENGTH_SHORT).show();
    }

    private void updateDashboardCounts() {
        TicketManager ticketManager = TicketManager.getInstance();

        // Update card counts
        int totalCount = ticketManager.getTotalTicketCount();
        int pendingCount = ticketManager.getTicketCountByStatus(Ticket.TicketStatus.PENDING);
        int rejectedCount = ticketManager.getTicketCountByStatus(Ticket.TicketStatus.REJECTED);
        int acceptedCount = ticketManager.getTicketCountByStatus(Ticket.TicketStatus.ACCEPTED);

        totalReportsNumber.setText(String.valueOf(totalCount));
        card2Number.setText(String.valueOf(pendingCount));
        card3Number.setText(String.valueOf(rejectedCount));
        card4Number.setText(String.valueOf(acceptedCount));

        // Refresh My Reports list
        List<Ticket> allTickets = ticketManager.getAllTickets();
        ticketAdapter.setTickets(allTickets);
    }

    // Call this method when a new report is submitted
    public void onNewReportSubmitted(Ticket newTicket) {
        updateDashboardCounts();
        Toast.makeText(this, "Report submitted successfully! ID: " + newTicket.getId(),
                Toast.LENGTH_SHORT).show();

        // Switch back to My Reports view
        setSelectedButton(myReportsButton);
        showMyReports();
    }

    private void handleLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeToggleButtons() {
        myReportsButton = findViewById(R.id.myReportsButton);
        newReportsButton = findViewById(R.id.newReportsButton);
        selectionBackground = findViewById(R.id.selectionBackground);

        // Initialize FAB
        FloatingActionButton fab = findViewById(R.id.floatingActionButton6);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAIAssistantPopup();
            }
        });

        // Set initial state - My Reports selected by default
        setSelectedButton(myReportsButton);

        // Set click listeners for toggle buttons
        myReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedButton(myReportsButton);
                showMyReports();
            }
        });

        newReportsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelectedButton(newReportsButton);
                showNewReports();
            }
        });
    }

    private void showAIAssistantPopup() {
        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_ai_assistant, null);

        // Calculate popup dimensions (90% of screen)
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);

        // Create popup window
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.setElevation(20f);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.dialog_frame));

        // Get references to chat components
        EditText chatInput = popupView.findViewById(R.id.chatInput);
        ImageButton sendButton = popupView.findViewById(R.id.sendButton);
        LinearLayout chatContainer = popupView.findViewById(R.id.chatContainer);
        NestedScrollView chatScrollView = popupView.findViewById(R.id.chatScrollView);
        ImageButton closeButton = popupView.findViewById(R.id.closeButton);

        // Clear existing messages except welcome
        for (int i = chatContainer.getChildCount() - 1; i > 0; i--) {
            View child = chatContainer.getChildAt(i);
            if (child.getId() != R.id.welcomeMessage) {
                chatContainer.removeViewAt(i);
            }
        }

        // Set up chat functionality
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = chatInput.getText().toString().trim();
                if (!message.isEmpty()) {
                    // Add user message to chat
                    addUserMessage(chatContainer, message);

                    // Clear input
                    chatInput.setText("");

                    // Scroll to bottom
                    scrollToBottom(chatScrollView);

                    // Get AI response from Hugging Face
                    getAIResponse(chatContainer, message, chatScrollView);
                }
            }
        });

        // Also send when Enter is pressed (but not for multiline - use Ctrl+Enter)
        chatInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (!event.isShiftPressed()) {
                        String message = chatInput.getText().toString().trim();
                        if (!message.isEmpty()) {
                            sendButton.performClick();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        // Set close button listener
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });

        // Show popup centered on screen
        popupWindow.showAtLocation(findViewById(R.id.main), Gravity.CENTER, 0, 0);

        // Focus on input field
        chatInput.requestFocus();

        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(chatInput, InputMethodManager.SHOW_IMPLICIT);
    }

    // Helper method to add user message to chat
    private void addUserMessage(LinearLayout chatContainer, String message) {
        // Create user message layout
        LinearLayout userMessageLayout = new LinearLayout(this);
        userMessageLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        userMessageLayout.setOrientation(LinearLayout.HORIZONTAL);
        userMessageLayout.setGravity(Gravity.END);

        // Create message text view
        TextView messageText = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(40, 8, 8, 8);
        messageText.setLayoutParams(params);
        messageText.setText(message);
        messageText.setTextSize(14);
        messageText.setTextColor(Color.WHITE);
        messageText.setPadding(12, 12, 12, 12);
        messageText.setBackgroundResource(R.drawable.chat_bubble_user);
        messageText.setMaxWidth(400);

        // Create "You" indicator
        TextView userIndicator = new TextView(this);
        userIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        userIndicator.setText("You");
        userIndicator.setTextSize(10);
        userIndicator.setTextColor(Color.parseColor("#666666"));
        userIndicator.setTypeface(null, Typeface.BOLD);
        userIndicator.setGravity(Gravity.BOTTOM);

        userMessageLayout.addView(messageText);
        userMessageLayout.addView(userIndicator);

        chatContainer.addView(userMessageLayout);
    }

    // Helper method to add AI message to chat
    private void addAIMessage(LinearLayout chatContainer, String message) {
        // Create AI message layout
        LinearLayout aiMessageLayout = new LinearLayout(this);
        aiMessageLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        aiMessageLayout.setOrientation(LinearLayout.HORIZONTAL);
        aiMessageLayout.setGravity(Gravity.START);

        // Create "AI" indicator
        TextView aiIndicator = new TextView(this);
        aiIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        aiIndicator.setText("AI");
        aiIndicator.setTextSize(10);
        aiIndicator.setTextColor(Color.parseColor("#666666"));
        aiIndicator.setTypeface(null, Typeface.BOLD);
        aiIndicator.setGravity(Gravity.BOTTOM);

        // Create message text view
        TextView messageText = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 40, 8);
        messageText.setLayoutParams(params);
        messageText.setText(message);
        messageText.setTextSize(14);
        messageText.setTextColor(Color.BLACK);
        messageText.setPadding(12, 12, 12, 12);
        messageText.setBackgroundResource(R.drawable.chat_bubble_ai);
        messageText.setMaxWidth(400);

        aiMessageLayout.addView(aiIndicator);
        aiMessageLayout.addView(messageText);

        chatContainer.addView(aiMessageLayout);
    }

    // Helper method to scroll to bottom
    private void scrollToBottom(final NestedScrollView scrollView) {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void getAIResponse(LinearLayout chatContainer, String userMessage, NestedScrollView scrollView) {
        addTypingIndicator(chatContainer);
        scrollToBottom(scrollView);

        huggingFaceRouterService.getResponse(userMessage, new HuggingFaceRouterService.AIResponseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    removeTypingIndicator(chatContainer);
                    addAIMessage(chatContainer, response);
                    scrollToBottom(scrollView);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    removeTypingIndicator(chatContainer);
                    addAIMessage(chatContainer, "⚠️ " + error +
                            "\n\nUsing local response:\n" + generateFallbackResponse(userMessage));
                    scrollToBottom(scrollView);
                });
            }
        });
    }

//            @Override
//            public void onError(String error) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        removeTypingIndicator(chatContainer);
//                        addAIMessage(chatContainer, "⚠️ I'm having trouble connecting to Hugging Face.\n\n" +
//                                "Here's what I can tell you:\n" + generateFallbackResponse(userMessage));
//                        scrollToBottom(scrollView);
//                    }
//                });
//            }
//        });
//    }

    // Generate fallback response when API fails
    private String generateFallbackResponse(String userMessage) {
        userMessage = userMessage.toLowerCase();

        if (userMessage.contains("hello") || userMessage.contains("hi") || userMessage.contains("hey")) {
            return "Hello! I'm your Infrastructure Reporting Assistant. How can I help you today?";
        } else if (userMessage.contains("report") || userMessage.contains("submit") || userMessage.contains("issue")) {
            return "To submit a report:\n1. Click '+ New Report'\n2. Take or upload a photo\n3. Add location and description\n4. Click Submit";
        } else if (userMessage.contains("pending") || userMessage.contains("status") || userMessage.contains("check")) {
            return "Check your report status in 'My Reports' or by clicking the status cards above (Pending, Accepted, Rejected).";
        } else if (userMessage.contains("road") || userMessage.contains("pothole")) {
            return "For road issues, take clear photos showing the problem and mention exact location for faster resolution.";
        } else if (userMessage.contains("category") || userMessage.contains("type")) {
            return "Report categories: Road, Utilities, Facilities, Environment. Choose the one that best matches your issue.";
        } else if (userMessage.contains("severity") || userMessage.contains("urgent")) {
            return "Severity levels: High (immediate danger), Medium (significant issue), Low (minor inconvenience).";
        } else if (userMessage.contains("time") || userMessage.contains("long") || userMessage.contains("wait")) {
            return "Response times vary: High severity (24-48h), Medium (3-5 days), Low (1-2 weeks).";
        } else if (userMessage.contains("photo") || userMessage.contains("picture") || userMessage.contains("image")) {
            return "For best results: Take clear, well-lit photos from multiple angles showing the entire issue.";
        } else if (userMessage.contains("thank") || userMessage.contains("thanks")) {
            return "You're welcome! Is there anything else I can help you with?";
        } else {
            return "I understand you're asking about infrastructure reporting. I can help with submitting reports, checking status, understanding categories, or answering questions about the process.";
        }
    }

    // Add typing indicator
    private void addTypingIndicator(LinearLayout chatContainer) {
        LinearLayout typingLayout = new LinearLayout(this);
        typingLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        typingLayout.setOrientation(LinearLayout.HORIZONTAL);
        typingLayout.setGravity(Gravity.START);
        typingLayout.setTag("typing");

        TextView aiIndicator = new TextView(this);
        aiIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        aiIndicator.setText("AI");
        aiIndicator.setTextSize(10);
        aiIndicator.setTextColor(Color.parseColor("#666666"));
        aiIndicator.setTypeface(null, Typeface.BOLD);
        aiIndicator.setGravity(Gravity.BOTTOM);

        TextView typingText = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 40, 8);
        typingText.setLayoutParams(params);
        typingText.setText("AI is typing...");
        typingText.setTextSize(14);
        typingText.setTextColor(Color.BLACK);
        typingText.setPadding(12, 12, 12, 12);
        typingText.setBackgroundResource(R.drawable.chat_bubble_ai);
        typingText.setMaxWidth(400);
        typingText.setTypeface(null, Typeface.ITALIC);

        typingLayout.addView(aiIndicator);
        typingLayout.addView(typingText);

        chatContainer.addView(typingLayout);
    }

    // Remove typing indicator
    private void removeTypingIndicator(LinearLayout chatContainer) {
        for (int i = 0; i < chatContainer.getChildCount(); i++) {
            View child = chatContainer.getChildAt(i);
            if ("typing".equals(child.getTag())) {
                chatContainer.removeViewAt(i);
                break;
            }
        }
    }

    private void setSelectedButton(TextView selectedButton) {
        // Update text colors and styles
        if (selectedButton == myReportsButton) {
            myReportsButton.setTextColor(Color.BLACK);
            myReportsButton.setTypeface(null, Typeface.BOLD);
            newReportsButton.setTextColor(Color.parseColor("#666666"));
            newReportsButton.setTypeface(null, Typeface.NORMAL);

            // Move selection background to My Reports
            selectionBackground.animate().x(0f).setDuration(200).start();

        } else {
            newReportsButton.setTextColor(Color.BLACK);
            newReportsButton.setTypeface(null, Typeface.BOLD);
            myReportsButton.setTextColor(Color.parseColor("#666666"));
            myReportsButton.setTypeface(null, Typeface.NORMAL);

            // Move selection background to New Reports
            selectionBackground.animate().x(500).setDuration(200).start();
        }
    }

    private void showMyReports() {
        // Show My Reports content
        TextView contentTitle = findViewById(R.id.contentTitle);
        TextView refreshButton = findViewById(R.id.refreshButton);
        CardView reportsContainer = findViewById(R.id.reportsContainer);
        NestedScrollView reportsContainer2 = findViewById(R.id.reportsContainer2);

        contentTitle.setText("Your submitted reports");
        contentTitle.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        reportsContainer.setVisibility(View.VISIBLE);
        reportsContainer2.setVisibility(View.GONE);

        // Refresh the reports list
        refreshMyReports();
    }

    private void showNewReports() {
        // Show New Reports content
        TextView contentTitle = findViewById(R.id.contentTitle);
        MaterialButton refreshButton = findViewById(R.id.refreshButton);
        CardView reportsContainer = findViewById(R.id.reportsContainer);
        NestedScrollView reportsContainer2 = findViewById(R.id.reportsContainer2);

        contentTitle.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        reportsContainer.setVisibility(View.GONE);

        // Clear the NestedScrollView
        reportsContainer2.removeAllViews();

        // Inflate the report_issue layout directly
        LayoutInflater inflater = LayoutInflater.from(this);
        View newReportView = inflater.inflate(R.layout.report_issue, reportsContainer2, false);

        // Set up the view directly (no fragment)
        setupNewReportView(newReportView);

        reportsContainer2.addView(newReportView);
        reportsContainer2.setVisibility(View.VISIBLE);
    }

    private void setupNewReportView(View view) {
        // Initialize views
        com.google.android.material.card.MaterialCardView cardTakePhoto =
                view.findViewById(R.id.card_take_photo);
        com.google.android.material.card.MaterialCardView cardUploadPhoto =
                view.findViewById(R.id.card_upload_photo);
        TextView locationText = view.findViewById(R.id.location_text);
        EditText descriptionInput = view.findViewById(R.id.description_input);
        Button submitButton = view.findViewById(R.id.submit_button);
        TextView enterManually = view.findViewById(R.id.enter_manually);

        // Set up listeners
        cardTakePhoto.setOnClickListener(v -> {
            // For now, just show toast
            Toast.makeText(this, "Take photo (need camera permission)", Toast.LENGTH_SHORT).show();
        });

        cardUploadPhoto.setOnClickListener(v -> {
            Toast.makeText(this, "Upload photo (need gallery permission)", Toast.LENGTH_SHORT).show();
        });

        enterManually.setOnClickListener(v -> {
            showLocationDialog(locationText);
        });

        // Enable/disable submit button
        descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // For simplicity, enable if there's any text
                submitButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        submitButton.setOnClickListener(v -> {
            submitNewReport(descriptionInput.getText().toString(),
                    locationText.getText().toString());
        });
    }

    private void showLocationDialog(TextView locationText) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Location");

        final EditText input = new EditText(this);
        input.setText(locationText.getText());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            locationText.setText(input.getText().toString());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void submitNewReport(String description, String location) {
        // Create new ticket
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm a", java.util.Locale.getDefault());
        String currentTime = sdf.format(new java.util.Date());

        Ticket newTicket = new Ticket(
                "",
                "Road",
                "Medium",
                location,
                description,
                currentTime,
                "new_report_image"
        );

        // Add to TicketManager
        TicketManager.getInstance().addTicket(newTicket);

        // Update dashboard and switch back
        onNewReportSubmitted(newTicket);
    }

    public void onReportCardClicked(View view) {
        String reportType = "";

        // Get the tag from the clicked card
        if (view instanceof CardView) {
            ViewGroup card = (ViewGroup) view;
            for (int i = 0; i < card.getChildCount(); i++) {
                View child = card.getChildAt(i);
                if (child instanceof LinearLayout) {
                    ViewGroup linearLayout = (ViewGroup) child;
                    for (int j = 0; j < linearLayout.getChildCount(); j++) {
                        View grandChild = linearLayout.getChildAt(j);
                        if (grandChild instanceof TextView) {
                            String tag = (String) grandChild.getTag();
                            if (tag != null) {
                                reportType = tag;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Show dialog
        TicketsDialogFragment dialog = TicketsDialogFragment.newInstance(reportType);
        dialog.show(getSupportFragmentManager(), TicketsDialogFragment.TAG);
    }
}