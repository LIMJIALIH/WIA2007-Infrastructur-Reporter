package com.example.infrastructureproject;

public class SupabaseConfig {
    // TODO: Replace with your actual Supabase URL and anon key
    // Get these from: Supabase Dashboard → Project Settings → API
    public static final String SUPABASE_URL = "https://your-project.supabase.co";
    public static final String SUPABASE_ANON_KEY = "your-anon-key-here";
    
    // API endpoints
    public static final String TICKETS_ENDPOINT = SUPABASE_URL + "/rest/v1/tickets";
    public static final String TICKET_IMAGES_ENDPOINT = SUPABASE_URL + "/rest/v1/ticket_images";
    public static final String STORAGE_ENDPOINT = SUPABASE_URL + "/storage/v1/object/tickets";
}
