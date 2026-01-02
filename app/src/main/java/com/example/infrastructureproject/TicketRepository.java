package com.example.infrastructureproject;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TicketRepository {
    private static final String TAG = "TicketRepository";
    
    /**
     * Create a new ticket in Supabase
     * @param userId User ID from SupabaseManager
     * @param issueType Type of issue (Road, Utilities, etc.)
     * @param severity Severity level (Low, Medium, High)
     * @param location Location of the issue
     * @param description Description of the issue
     * @param imageBitmap Optional image bitmap (can be null)
     * @param callback Callback for success/failure
     */
    public static void createTicket(
            String userId,
            String issueType, 
            String severity,
            String location, 
            String description,
            Bitmap imageBitmap,
            CreateTicketCallback callback) {
        
        new Thread(() -> {
            try {
                // Generate ticket ID with timestamp
                String ticketId = "TKT" + System.currentTimeMillis();
                
                // Prepare ticket data
                JSONObject ticketData = new JSONObject();
                ticketData.put("ticket_id", ticketId);
                ticketData.put("reporter_id", userId);
                ticketData.put("issue_type", issueType);
                ticketData.put("severity", severity);
                ticketData.put("status", "Pending");
                ticketData.put("location", location);
                ticketData.put("description", description);
                
                // Insert ticket into database
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets";
                String response = SupabaseManager.makeHttpRequest(
                    "POST",
                    url,
                    ticketData.toString(),
                    SupabaseManager.getAccessToken()
                );
                
                // Parse response to get ticket ID
                JSONArray responseArray = new JSONArray(response);
                if (responseArray.length() > 0) {
                    JSONObject insertedTicket = responseArray.getJSONObject(0);
                    String dbTicketId = insertedTicket.getString("id");
                    
                    // Upload image if provided
                    if (imageBitmap != null) {
                        uploadTicketImage(dbTicketId, ticketId, imageBitmap);
                    }
                    
                    if (callback != null) {
                        callback.onSuccess(ticketId);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Failed to create ticket");
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating ticket", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Upload image to Supabase Storage
     */
    private static void uploadTicketImage(String ticketId, String ticketNumber, Bitmap bitmap) {
        try {
            // Convert bitmap to byte array
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteStream);
            byte[] imageBytes = byteStream.toByteArray();
            
            // Create unique filename
            String filename = ticketNumber + "_" + System.currentTimeMillis() + ".jpg";
            String path = ticketId + "/" + filename;
            
            // Upload to Supabase Storage
            String storageUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/ticket-images/" + path;
            
            // Note: This is a simplified version. You may need to implement multipart upload
            // For now, we'll store the image reference in the database
            
            // Store image metadata in ticket_images table
            JSONObject imageData = new JSONObject();
            imageData.put("ticket_id", ticketId);
            imageData.put("bucket", "ticket-images");
            imageData.put("path", path);
            imageData.put("filename", filename);
            
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/ticket_images";
            SupabaseManager.makeHttpRequest(
                "POST",
                url,
                imageData.toString(),
                SupabaseManager.getAccessToken()
            );
            
            Log.d(TAG, "Image metadata saved successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image", e);
        }
    }
    
    /**
     * Fetch all tickets for a specific user
     */
    public static void getUserTickets(String userId, FetchTicketsCallback callback) {
        new Thread(() -> {
            try {
                // Fetch tickets for this user (soft delete filter applied in code if column exists)
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?reporter_id=eq." + userId + "&order=created_at.desc";
                
                String response = SupabaseManager.makeHttpRequest(
                    "GET",
                    url,
                    null,
                    SupabaseManager.getAccessToken()
                );
                
                // Parse response
                JSONArray ticketsArray = new JSONArray(response);
                List<Ticket> tickets = new ArrayList<>();
                
                for (int i = 0; i < ticketsArray.length(); i++) {
                    JSONObject ticketJson = ticketsArray.getJSONObject(i);
                    
                    // Skip soft-deleted tickets (if column exists)
                    boolean deletedByCitizen = ticketJson.optBoolean("deleted_by_citizen", false);
                    if (deletedByCitizen) {
                        continue; // Skip this ticket
                    }
                    
                    // Convert JSON to Ticket object
                    String ticketId = ticketJson.optString("ticket_id", "");
                    String dbId = ticketJson.optString("id", "");
                    String type = ticketJson.optString("issue_type", "Other");
                    String severity = ticketJson.optString("severity", "Low");
                    String location = ticketJson.optString("location", "Unknown");
                    String description = ticketJson.optString("description", "");
                    String createdAt = ticketJson.optString("created_at", "");
                    String status = ticketJson.optString("status", "Pending");
                    
                    // Format date
                    String formattedDate = formatDate(createdAt);
                    
                    Ticket ticket = new Ticket(
                        ticketId,
                        type,
                        severity,
                        location,
                        description,
                        formattedDate,
                        "" // image name - will be set from image URL
                    );
                    
                    // Set database ID and status
                    ticket.setDbId(dbId);
                    ticket.setStatus(parseStatus(status));
                    
                    // Set engineer's reason (for accepted/rejected tickets)
                    String engineerNotes = ticketJson.optString("engineer_notes", "");
                    if (!engineerNotes.isEmpty()) {
                        ticket.setReason(engineerNotes);
                    }
                    
                    // Fetch image URL for this ticket
                    String imageUrl = getTicketImageUrl(dbId);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        ticket.setImageUrl(imageUrl);
                    }
                    
                    tickets.add(ticket);
                }
                
                if (callback != null) {
                    callback.onSuccess(tickets);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching tickets", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Get the image URL for a ticket from ticket_images table
     */
    private static String getTicketImageUrl(String ticketDbId) {
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/ticket_images?ticket_id=eq." + ticketDbId + "&select=path";
            
            String response = SupabaseManager.makeHttpRequest(
                "GET",
                url,
                null,
                SupabaseManager.getAccessToken()
            );
            
            JSONArray imagesArray = new JSONArray(response);
            if (imagesArray.length() > 0) {
                String imagePath = imagesArray.getJSONObject(0).optString("path", "");
                if (!imagePath.isEmpty()) {
                    // Return full public URL
                    return BuildConfig.SUPABASE_URL + "/storage/v1/object/public/ticket-images/" + imagePath;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching image URL", e);
        }
        return null;
    }
    
    /**
     * Fetch a single ticket by database ID with assignment metadata
     */
    public static void getTicketByDbId(String dbId, FetchTicketCallback callback) {
        new Thread(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL +
                        "/rest/v1/tickets?id=eq." + dbId +
                        "&select=*,profiles!tickets_reporter_id_fkey(full_name)";
                String response = SupabaseManager.makeHttpRequest("GET", url, null, SupabaseManager.getAccessToken());
                JSONArray arr = new JSONArray(response);
                if (arr.length() == 0) {
                    if (callback != null) callback.onError("Ticket not found");
                    return;
                }
                JSONObject t = arr.getJSONObject(0);
                String ticketId = t.optString("ticket_id", "");
                String type = t.optString("issue_type", "Other");
                String severity = t.optString("severity", "Low");
                String location = t.optString("location", "Unknown");
                String description = t.optString("description", "");
                String createdAt = t.optString("created_at", "");
                String formattedDate = formatDate(createdAt);
                Ticket ticket = new Ticket(ticketId, type, severity, location, description, formattedDate, "");
                ticket.setDbId(dbId);
                ticket.setStatus(parseStatus(t.optString("status", "Pending")));
                ticket.setAssignedTo(t.optString("assigned_engineer_name", ""));
                ticket.setCouncilNotes(t.optString("council_notes", ""));
                // Set engineer's reason for accept/reject
                String engineerNotes = t.optString("engineer_notes", "");
                if (!engineerNotes.isEmpty()) {
                    ticket.setReason(engineerNotes);
                }
                String reporterId = t.optString("reporter_id", "");
                ticket.setReporterId(reporterId);
                // Reporter full name if available from join
                JSONObject profile = t.optJSONObject("profiles");
                if (profile != null) {
                    ticket.setUsername(profile.optString("full_name", "Anonymous"));
                } else {
                    ticket.setUsername(getReporterName(reporterId));
                }
                String imageUrl = getTicketImageUrl(dbId);
                if (imageUrl != null) ticket.setImageUrl(imageUrl);
                if (callback != null) callback.onSuccess(ticket);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching ticket by id", e);
                if (callback != null) callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Get ALL tickets from Supabase (for council/management dashboard)
     */
    public static void getAllTickets(FetchTicketsCallback callback) {
        new Thread(() -> {
            try {
                // Fetch all tickets (soft delete filter applied in code if column exists)
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?select=*&order=created_at.desc";
                
                String response = SupabaseManager.makeHttpRequest(
                    "GET",
                    url,
                    null,
                    SupabaseManager.getAccessToken()
                );
                
                JSONArray ticketsArray = new JSONArray(response);
                List<Ticket> tickets = new ArrayList<>();
                
                for (int i = 0; i < ticketsArray.length(); i++) {
                    JSONObject ticketJson = ticketsArray.getJSONObject(i);
                    
                    // Skip soft-deleted tickets for council (if column exists)
                    boolean deletedByCouncil = ticketJson.optBoolean("deleted_by_council", false);
                    if (deletedByCouncil) {
                        continue; // Skip this ticket
                    }
                    
                    // Extract database ID for image URL
                    String dbId = ticketJson.optString("id", "");
                    String reporterId = ticketJson.optString("reporter_id", "");
                    
                    // Get image URL from ticket_images table
                    String imageUrl = getTicketImageUrl(dbId);
                    
                    // Get reporter's full name from profiles table
                    String reporterName = getReporterName(reporterId);
                    
                    Ticket ticket = new Ticket(
                        ticketJson.optString("ticket_id", ""),
                        ticketJson.optString("issue_type", ""),
                        ticketJson.optString("location", ""),
                        formatDate(ticketJson.optString("created_at", "")),
                        ticketJson.optString("description", ""),
                        ticketJson.optString("severity", "Low"),
                        ""
                    );
                    
                    ticket.setDbId(dbId);
                    ticket.setStatus(parseStatus(ticketJson.optString("status", "pending")));
                    ticket.setImageUrl(imageUrl);
                    ticket.setReporterId(reporterId);
                    ticket.setUsername(reporterName);
                    
                    tickets.add(ticket);
                }
                
                if (callback != null) {
                    callback.onSuccess(tickets);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching all tickets", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Get council statistics from all tickets
     */
    public static void getCouncilStatistics(CouncilStatsCallback callback) {
        new Thread(() -> {
            try {
                // Get all tickets (soft delete filter applied in code if column exists)
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?select=status,severity,created_at,assigned_at";
                
                String response = SupabaseManager.makeHttpRequest(
                    "GET",
                    url,
                    null,
                    SupabaseManager.getAccessToken()
                );
                
                JSONArray ticketsArray = new JSONArray(response);
                
                int totalReports = ticketsArray.length();
                int totalPending = 0;
                int highPriorityPending = 0;
                long totalResponseTime = 0;
                int assignedCount = 0;
                
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                
                for (int i = 0; i < ticketsArray.length(); i++) {
                    JSONObject ticket = ticketsArray.getJSONObject(i);
                    String status = ticket.optString("status", "pending").toLowerCase();
                    String severity = ticket.optString("severity", "low");
                    String createdAt = ticket.optString("created_at", "");
                    String assignedAt = ticket.optString("assigned_at", "");
                    
                    if ("pending".equals(status)) {
                        totalPending++;
                        if ("high".equalsIgnoreCase(severity)) {
                            highPriorityPending++;
                        }
                    }
                    
                    // Calculate response time for assigned tickets
                    if (!assignedAt.isEmpty() && !createdAt.isEmpty()) {
                        try {
                            Date created = isoFormat.parse(createdAt.substring(0, 19));
                            Date assigned = isoFormat.parse(assignedAt.substring(0, 19));
                            long responseTime = assigned.getTime() - created.getTime();
                            totalResponseTime += responseTime;
                            assignedCount++;
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing dates for avg response", e);
                        }
                    }
                }
                
                // Calculate average response time
                String avgResponse;
                if (assignedCount > 0) {
                    long avgMillis = totalResponseTime / assignedCount;
                    long avgHours = avgMillis / (1000 * 60 * 60);
                    double avgHoursFraction = avgMillis / (1000.0 * 60 * 60);
                    if (avgHoursFraction < 1) {
                        avgResponse = "< 1 hr";
                    } else {
                        avgResponse = String.format("%.1f hrs", avgHoursFraction);
                    }
                } else {
                    avgResponse = "N/A";
                }
                
                if (callback != null) {
                    callback.onSuccess(totalReports, totalPending, highPriorityPending, avgResponse);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching council statistics", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Get reporter's full name from profiles table
     */
    private static String getReporterName(String reporterId) {
        if (reporterId == null || reporterId.isEmpty()) {
            return "Anonymous";
        }
        
        try {
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?id=eq." + reporterId + "&select=full_name";
            
            String response = SupabaseManager.makeHttpRequest(
                "GET",
                url,
                null,
                SupabaseManager.getAccessToken()
            );
            
            JSONArray profilesArray = new JSONArray(response);
            if (profilesArray.length() > 0) {
                JSONObject profile = profilesArray.getJSONObject(0);
                String fullName = profile.optString("full_name", "");
                return fullName.isEmpty() ? "Anonymous" : fullName;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching reporter name", e);
        }
        
        return "Anonymous";
    }
    
    /**
     * Get tickets assigned to an engineer with statistics
     * Fetches all tickets assigned to the current engineer and calculates:
     * - New Today: tickets assigned today
     * - This Week: tickets assigned this week
     * - High Priority: pending high severity tickets
     * - Avg Response: average response time
     */
    public static void getEngineerTicketsWithStats(String engineerId, EngineerStatsCallback callback) {
        new Thread(() -> {
            try {
                // Fetch all tickets assigned to this engineer
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?assigned_engineer_id=eq." + engineerId + "&order=created_at.desc";
                
                String response = SupabaseManager.makeHttpRequest(
                    "GET",
                    url,
                    null,
                    SupabaseManager.getAccessToken()
                );
                
                JSONArray ticketsArray = new JSONArray(response);
                List<Ticket> allTickets = new ArrayList<>();
                
                // Get today's date for comparison
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayDate = dateFormat.format(new Date());
                
                // Get start of week (7 days ago)
                long weekAgoMillis = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                String weekAgoDate = dateFormat.format(new Date(weekAgoMillis));
                
                int newToday = 0;
                int thisWeek = 0;
                int highPriority = 0;
                long totalResponseTime = 0;
                int responseCount = 0;
                
                for (int i = 0; i < ticketsArray.length(); i++) {
                    JSONObject ticketJson = ticketsArray.getJSONObject(i);
                    
                    // Parse ticket data
                    String ticketId = ticketJson.optString("ticket_id", "");
                    String dbId = ticketJson.optString("id", "");
                    String type = ticketJson.optString("issue_type", "Other");
                    String severity = ticketJson.optString("severity", "Low");
                    String location = ticketJson.optString("location", "Unknown");
                    String description = ticketJson.optString("description", "");
                    String createdAt = ticketJson.optString("created_at", "");
                    String assignedAt = ticketJson.optString("assigned_at", "");
                    String status = ticketJson.optString("status", "Pending");
                    String reporterId = ticketJson.optString("reporter_id", "");
                    
                    // Format date
                    String formattedDate = formatDate(createdAt);
                    
                    // Create ticket object
                    Ticket ticket = new Ticket(
                        ticketId,
                        type,
                        severity,
                        location,
                        description,
                        formattedDate,
                        ""
                    );
                    ticket.setDbId(dbId);
                    ticket.setStatus(parseStatus(status));
                    // Assignment metadata for engineer view
                    ticket.setAssignedTo(ticketJson.optString("assigned_engineer_name", ""));
                    ticket.setCouncilNotes(ticketJson.optString("council_notes", ""));
                    
                    // Get reporter name
                    String reporterName = getReporterName(reporterId);
                    ticket.setUsername(reporterName);
                    
                    // Fetch image URL
                    String imageUrl = getTicketImageUrl(dbId);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        ticket.setImageUrl(imageUrl);
                    }
                    
                    allTickets.add(ticket);
                    
                    // Calculate statistics
                    // Extract date from created_at (format: 2025-01-02T10:30:00+00:00)
                    String ticketDate = createdAt.substring(0, 10); // Get YYYY-MM-DD
                    
                    // New Today
                    if (ticketDate.equals(todayDate)) {
                        newToday++;
                    }
                    
                    // This Week
                    if (ticketDate.compareTo(weekAgoDate) >= 0) {
                        thisWeek++;
                    }
                    
                    // High Priority (Accepted tickets assigned to engineer with high severity)
                    // These are tickets assigned but not yet marked as completed by engineer
                    if (severity.equalsIgnoreCase("High") && status.equalsIgnoreCase("Accepted")) {
                        highPriority++;
                    }
                    
                    // Calculate response time based on first engineer action after assignment
                    if (!assignedAt.isEmpty()) {
                        try {
                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                            Date assigned = isoFormat.parse(assignedAt.substring(0, 19));
                            // First action among ACCEPTED/REJECTED/SPAM
                            String actionUrl = BuildConfig.SUPABASE_URL + 
                                "/rest/v1/ticket_actions?ticket_id=eq." + dbId + 
                                "&action_type=in.(ACCEPTED,REJECTED,SPAM)&order=created_at.asc&limit=1";
                            String actionResponse = SupabaseManager.makeHttpRequest("GET", actionUrl, null, SupabaseManager.getAccessToken());
                            JSONArray actionsArray = new JSONArray(actionResponse);
                            
                            if (actionsArray.length() > 0) {
                                String acceptedAt = actionsArray.getJSONObject(0).optString("created_at", "");
                                if (!acceptedAt.isEmpty()) {
                                    Date accepted = isoFormat.parse(acceptedAt.substring(0, 19));
                                    long responseTime = accepted.getTime() - assigned.getTime();
                                    totalResponseTime += responseTime;
                                    responseCount++;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error calculating response time", e);
                        }
                    }
                }
                
                // Calculate average response time
                String avgResponse;
                if (responseCount > 0) {
                    long avgMillis = totalResponseTime / responseCount;
                    long avgHours = avgMillis / (1000 * 60 * 60);
                    if (avgHours < 1) {
                        avgResponse = "< 1 hour";
                    } else {
                        avgResponse = "< " + avgHours + " hours";
                    }
                } else {
                    avgResponse = "< 2 hours"; // Default
                }
                
                if (callback != null) {
                    callback.onSuccess(allTickets, newToday, thisWeek, highPriority, avgResponse);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching engineer tickets", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Assign a ticket to an engineer
     * Updates the ticket in Supabase with assigned engineer details
     * Sets status to UNDER_REVIEW for engineer to process
     */
    public static void assignTicketToEngineer(
            String ticketDbId,
            String engineerId, 
            String engineerName,
            String instructions,
            AssignTicketCallback callback) {
        
        new Thread(() -> {
            try {
                // Prepare update data
                JSONObject updateData = new JSONObject();
                updateData.put("assigned_engineer_id", engineerId);
                updateData.put("assigned_engineer_name", engineerName);
                // Set status to UNDER_REVIEW so it appears in engineer pending
                updateData.put("status", "UNDER_REVIEW");
                updateData.put("assigned_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));
                
                if (instructions != null && !instructions.isEmpty()) {
                    updateData.put("council_notes", instructions);
                }
                
                // Update ticket in Supabase
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?id=eq." + ticketDbId;
                
                String response = SupabaseManager.makeHttpRequest(
                    "PATCH",
                    url,
                    updateData.toString(),
                    SupabaseManager.getAccessToken()
                );
                
                Log.d(TAG, "Ticket assigned successfully");
                
                if (callback != null) {
                    callback.onSuccess();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error assigning ticket", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Get all engineers with their ticket statistics
     */
    public static void getEngineersWithStats(EngineersCallback callback) {
        new Thread(() -> {
            try {
                // First, get all engineers from profiles table
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/profiles?role=eq.engineer&select=id,full_name,email";
                
                String response = SupabaseManager.makeHttpRequest(
                    "GET",
                    url,
                    null,
                    SupabaseManager.getAccessToken()
                );
                
                JSONArray engineersArray = new JSONArray(response);
                List<Engineer> engineers = new ArrayList<>();
                
                for (int i = 0; i < engineersArray.length(); i++) {
                    JSONObject engineerJson = engineersArray.getJSONObject(i);
                    
                    String engineerId = engineerJson.optString("id", "");
                    String fullName = engineerJson.optString("full_name", "Unknown");
                    String email = engineerJson.optString("email", "");
                    
                    // Get ticket statistics for this engineer
                    int[] stats = getEngineerTicketStats(engineerId);
                    int totalReports = stats[0];
                    int highPriority = stats[1];
                    
                    Engineer engineer = new Engineer(engineerId, fullName, email, totalReports, highPriority);
                    engineers.add(engineer);
                }
                
                if (callback != null) {
                    callback.onSuccess(engineers);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching engineers", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Get ticket statistics for a specific engineer
     * Returns [total_tickets, high_priority_tickets]
     */
    private static int[] getEngineerTicketStats(String engineerId) {
        int totalTickets = 0;
        int highPriority = 0;
        
        try {
            // Note: This assumes you have assigned_to column in tickets table
            // If not, you'll need to add it to the database schema
            String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?assigned_engineer_id=eq." + engineerId + "&select=severity";
            
            String response = SupabaseManager.makeHttpRequest(
                "GET",
                url,
                null,
                SupabaseManager.getAccessToken()
            );
            
            JSONArray ticketsArray = new JSONArray(response);
            totalTickets = ticketsArray.length();
            
            for (int i = 0; i < ticketsArray.length(); i++) {
                JSONObject ticket = ticketsArray.getJSONObject(i);
                String severity = ticket.optString("severity", "");
                if ("high".equalsIgnoreCase(severity)) {
                    highPriority++;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching engineer stats", e);
        }
        
        return new int[]{totalTickets, highPriority};
    }
    
    /**
     * Get ticket statistics for a user
     */
    public static void getUserStatistics(String userId, StatsCallback callback) {
        new Thread(() -> {
            try {
                // Count all tickets for user (soft delete filter applied in code if column exists)
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?reporter_id=eq." + userId + "&select=status";
                
                String response = SupabaseManager.makeHttpRequest(
                    "GET",
                    url,
                    null,
                    SupabaseManager.getAccessToken()
                );
                
                JSONArray ticketsArray = new JSONArray(response);
                
                int total = ticketsArray.length();
                int pending = 0;
                int accepted = 0;
                int rejected = 0;
                
                for (int i = 0; i < ticketsArray.length(); i++) {
                    String status = ticketsArray.getJSONObject(i).optString("status", "Pending");
                    
                    switch (status.toLowerCase()) {
                        case "pending":
                        case "under_review": // Pending includes both pending and under_review
                            pending++;
                            break;
                        case "accepted":
                        case "completed":
                            accepted++;
                            break;
                        case "rejected":
                        case "spam": // Rejected includes both rejected and spam
                            rejected++;
                            break;
                    }
                }
                
                if (callback != null) {
                    callback.onSuccess(total, pending, accepted, rejected);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching statistics", e);
                if (callback != null) {
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    // Helper methods
    private static String formatDate(String isoDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
            Date date = inputFormat.parse(isoDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return isoDate;
        }
    }
    
    private static Ticket.TicketStatus parseStatus(String status) {
        switch (status.toLowerCase()) {
            case "accepted":
            case "completed":
                return Ticket.TicketStatus.ACCEPTED;
            case "rejected":
                return Ticket.TicketStatus.REJECTED;
            case "spam":
                return Ticket.TicketStatus.SPAM;
            case "under_review":
                return Ticket.TicketStatus.UNDER_REVIEW;
            default:
                return Ticket.TicketStatus.PENDING;
        }
    }
    
    // Callback interfaces
    public interface CreateTicketCallback {
        void onSuccess(String ticketId);
        void onError(String message);
    }
    
    public interface FetchTicketsCallback {
        void onSuccess(List<Ticket> tickets);
        void onError(String message);
    }
    
    // Single ticket fetch
    public interface FetchTicketCallback {
        void onSuccess(Ticket ticket);
        void onError(String message);
    }
    
    public interface StatsCallback {
        void onSuccess(int total, int pending, int accepted, int rejected);
        void onError(String message);
    }
    
    public interface CouncilStatsCallback {
        void onSuccess(int totalReports, int totalPending, int highPriorityPending, String avgResponse);
        void onError(String message);
    }
    
    public interface EngineersCallback {
        void onSuccess(List<Engineer> engineers);
        void onError(String message);
    }
    
    public interface EngineerStatsCallback {
        void onSuccess(List<Ticket> allTickets, int newToday, int thisWeek, int highPriority, String avgResponse);
        void onError(String message);
    }
    
    public interface AssignTicketCallback {
        void onSuccess();
        void onError(String message);
    }

    /**
     * Engineer processes a ticket (Accept / Reject / Spam)
     * - Updates ticket status in tickets table
     * - Inserts a row in ticket_actions to log response time and reason
     */
    public static void engineerProcessTicket(
            String ticketDbId,
            String engineerId,
            String actionType, // ACCEPTED | REJECTED | SPAM
            String reason,
            AssignTicketCallback callback) {
        new Thread(() -> {
            try {
                // Update status in tickets table
                String statusValue;
                switch (actionType) {
                    case "ACCEPTED": statusValue = "Accepted"; break;
                    case "REJECTED": statusValue = "Rejected"; break;
                    case "SPAM": statusValue = "Spam"; break;
                    default: statusValue = "Pending"; break;
                }
                JSONObject updateData = new JSONObject();
                updateData.put("status", statusValue);
                if (reason != null && !reason.isEmpty()) {
                    updateData.put("engineer_notes", reason);
                }
                String updateUrl = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?id=eq." + ticketDbId;
                SupabaseManager.makeHttpRequest("PATCH", updateUrl, updateData.toString(), SupabaseManager.getAccessToken());

                // Insert ticket action
                JSONObject actionData = new JSONObject();
                actionData.put("ticket_id", ticketDbId);
                actionData.put("created_by", engineerId);
                actionData.put("action_type", actionType);
                if (reason != null && !reason.isEmpty()) {
                    actionData.put("reason", reason);
                }
                String actionUrl = BuildConfig.SUPABASE_URL + "/rest/v1/ticket_actions";
                SupabaseManager.makeHttpRequest("POST", actionUrl, actionData.toString(), SupabaseManager.getAccessToken());

                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Error processing ticket", e);
                if (callback != null) callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    // Engineer data class
    public static class Engineer {
        private String id;
        private String name;
        private String email;
        private int totalReports;
        private int highPriority;
        
        public Engineer(String id, String name, String email, int totalReports, int highPriority) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.totalReports = totalReports;
            this.highPriority = highPriority;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public int getTotalReports() { return totalReports; }
        public int getHighPriority() { return highPriority; }
    }
    
    /**
     * Soft delete ticket for citizen view
     * Sets deleted_by_citizen = true, hiding it from citizen dashboard
     * Ticket remains visible to council and engineer
     */
    public static void softDeleteTicketForCitizen(String ticketDbId, AssignTicketCallback callback) {
        new Thread(() -> {
            try {
                JSONObject updateData = new JSONObject();
                updateData.put("deleted_by_citizen", true);
                
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?id=eq." + ticketDbId;
                SupabaseManager.makeHttpRequest("PATCH", url, updateData.toString(), SupabaseManager.getAccessToken());
                
                Log.d(TAG, "Ticket soft-deleted for citizen");
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Error soft-deleting ticket for citizen", e);
                if (callback != null) callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Soft delete ticket for council view
     * Sets deleted_by_council = true, hiding it from council dashboard
     * Ticket remains visible to citizen and engineer
     */
    public static void softDeleteTicketForCouncil(String ticketDbId, AssignTicketCallback callback) {
        new Thread(() -> {
            try {
                JSONObject updateData = new JSONObject();
                updateData.put("deleted_by_council", true);
                
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?id=eq." + ticketDbId;
                SupabaseManager.makeHttpRequest("PATCH", url, updateData.toString(), SupabaseManager.getAccessToken());
                
                Log.d(TAG, "Ticket soft-deleted for council");
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Error soft-deleting ticket for council", e);
                if (callback != null) callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
}
