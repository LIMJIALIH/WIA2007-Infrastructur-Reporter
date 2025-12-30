package com.example.infrastructureproject.repository;

import android.net.Uri;
import android.util.Log;

import com.example.infrastructureproject.SupabaseConfig;
import com.example.infrastructureproject.models.Ticket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TicketRepository {
    private static final String TAG = "TicketRepository";
    private final OkHttpClient client;
    private final Gson gson;

    public TicketRepository() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * Create a new ticket in Supabase
     */
    public Ticket createTicket(Ticket ticket, String userToken) throws IOException {
        String json = gson.toJson(ticket);
        
        RequestBody body = RequestBody.create(
            json,
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(SupabaseConfig.TICKETS_ENDPOINT)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to create ticket: " + response.code() + " - " + responseBody);
                throw new IOException("Failed to create ticket: " + response.message());
            }

            // Parse response array and get first item
            Type listType = new TypeToken<List<Ticket>>(){}.getType();
            List<Ticket> tickets = gson.fromJson(responseBody, listType);
            
            if (tickets != null && !tickets.isEmpty()) {
                Log.d(TAG, "Ticket created successfully: " + tickets.get(0).getTicketId());
                return tickets.get(0);
            }
            
            throw new IOException("No ticket returned from server");
        }
    }

    /**
     * Upload image to Supabase Storage
     */
    public String uploadImage(Uri imageUri, String ticketId, String userToken, 
                             android.content.Context context) throws IOException {
        // Generate unique filename
        String filename = System.currentTimeMillis() + "_" + ticketId + ".jpg";
        String path = "tickets/" + ticketId + "/" + filename;

        // Read image bytes
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("Cannot open image file");
        }

        byte[] imageBytes = readBytes(inputStream);
        inputStream.close();

        // Upload to storage
        RequestBody body = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
        
        Request request = new Request.Builder()
                .url(SupabaseConfig.STORAGE_ENDPOINT + "/" + path)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "image/jpeg")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String error = response.body() != null ? response.body().string() : "";
                Log.e(TAG, "Failed to upload image: " + response.code() + " - " + error);
                throw new IOException("Failed to upload image: " + response.message());
            }
            
            Log.d(TAG, "Image uploaded successfully: " + path);
            return path;
        }
    }

    /**
     * Save image metadata to ticket_images table
     */
    public void saveImageMetadata(String ticketId, String path, String filename, 
                                  String uploadedBy, String userToken) throws IOException {
        String json = String.format(
            "{\"ticket_id\":\"%s\",\"path\":\"%s\",\"filename\":\"%s\",\"uploaded_by\":\"%s\"}",
            ticketId, path, filename, uploadedBy
        );

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(SupabaseConfig.TICKET_IMAGES_ENDPOINT)
                .post(body)
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + userToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to save image metadata: " + response.code());
                throw new IOException("Failed to save image metadata");
            }
            Log.d(TAG, "Image metadata saved");
        }
    }

    /**
     * Fetch user's tickets
     */
    public List<Ticket> getUserTickets(String userId, String userToken) throws IOException {
        String url = SupabaseConfig.TICKETS_ENDPOINT + "?reporter_id=eq." + userId + "&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + userToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch tickets: " + response.message());
            }

            String responseBody = response.body() != null ? response.body().string() : "[]";
            Type listType = new TypeToken<List<Ticket>>(){}.getType();
            return gson.fromJson(responseBody, listType);
        }
    }

    // Helper to read InputStream to bytes
    private byte[] readBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
