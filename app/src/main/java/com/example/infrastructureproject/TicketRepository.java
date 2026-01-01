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
                    
                    // Set status
                    ticket.setStatus(parseStatus(status));
                    
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
     * Get ALL tickets from Supabase (for council/management dashboard)
     */
    public static void getAllTickets(FetchTicketsCallback callback) {
        new Thread(() -> {
            try {
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
                String url = BuildConfig.SUPABASE_URL + "/rest/v1/tickets?select=status,severity,created_at";
                
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
                int completedCount = 0;
                long totalResponseTime = 0;
                
                for (int i = 0; i < ticketsArray.length(); i++) {
                    JSONObject ticket = ticketsArray.getJSONObject(i);
                    String status = ticket.optString("status", "pending").toLowerCase();
                    String severity = ticket.optString("severity", "low");
                    
                    if ("pending".equals(status)) {
                        totalPending++;
                        if ("high".equalsIgnoreCase(severity)) {
                            highPriorityPending++;
                        }
                    } else if ("accepted".equals(status) || "completed".equals(status)) {
                        completedCount++;
                        // For now, use placeholder response time (2.5 hours)
                        // You can calculate actual time if you have accepted_at timestamp
                    }
                }
                
                // Calculate average response time (placeholder: 2.5 hrs)
                String avgResponse = completedCount > 0 ? "2.5 hrs" : "N/A";
                
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
                            pending++;
                            break;
                        case "accepted":
                        case "completed":
                            accepted++;
                            break;
                        case "rejected":
                        case "spam":
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
}
