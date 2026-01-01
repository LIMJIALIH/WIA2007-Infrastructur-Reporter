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
                        "" // image name - will be loaded separately if needed
                    );
                    
                    // Set status
                    ticket.setStatus(parseStatus(status));
                    
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
}
