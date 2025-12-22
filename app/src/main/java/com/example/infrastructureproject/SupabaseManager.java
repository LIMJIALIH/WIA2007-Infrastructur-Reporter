package com.example.infrastructureproject;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.infrastructurereporter.BuildConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class SupabaseManager {
    private static final String TAG = "SupabaseManager";
    private static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    private static final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Simple in-memory session
    private static String accessToken = null;
    private static String currentFullName = null;

    // Password validation regex: 
    // (?=.*[0-9])       At least one digit
    // (?=.*[a-z])       At least one lowercase
    // (?=.*[A-Z])       At least one uppercase
    // (?=.*[a-zA-Z0-9]) (Redundant check)
    // (?=.*[^a-zA-Z0-9]) At least one symbol (anything not alphanumeric)
    // .{8,}             At least 8 characters
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$");

    public interface AuthCallback {
        void onSuccess(String role, String fullName);
        void onError(String message);
    }
    
    // Helper overload for backward compatibility if needed, but we'll update calls
    public static void signUp(String email, String password, String fullName, String role, AuthCallback callback) {
        executor.execute(() -> {
            // Client-side Password Validation
            if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
                postError(callback, "Password needs 8+ chars, 1 number, 1 symbol, 1 upper & 1 lowercase.");
                return;
            }

            try {
                // 1. Sign Up
                String authUrl = SUPABASE_URL + "/auth/v1/signup";
                JSONObject authBody = new JSONObject();
                authBody.put("email", email);
                authBody.put("password", password);
                
                JSONObject data = new JSONObject();
                data.put("full_name", fullName);
                data.put("role", role);
                authBody.put("data", data);

                String authResponse = makeHttpRequest("POST", authUrl, authBody.toString(), null);
                JSONObject authJson = new JSONObject(authResponse);
                
                String userId = null;
                if (authJson.has("id")) userId = authJson.getString("id"); 
                else if (authJson.has("user")) userId = authJson.getJSONObject("user").getString("id");
                
                if (userId == null) {
                    postError(callback, "Sign up failed: No user ID returned.");
                    return;
                }
                
                String token = null;
                if (authJson.has("access_token") && !authJson.isNull("access_token")) {
                    token = authJson.getString("access_token");
                    accessToken = token; // Store it
                    currentFullName = fullName;
                }
                
                // 2. Insert into profiles
                // We attempt this regardless of token presence. 
                // If token is null (email confirmation required), we use Anon key (assuming RLS allows it or no RLS).
                try {
                    String profilesUrl = SUPABASE_URL + "/rest/v1/profiles";
                    JSONObject profileBody = new JSONObject();
                    profileBody.put("id", userId);
                    profileBody.put("full_name", fullName);
                    profileBody.put("role", role);
                    profileBody.put("email", email);
                    
                    makeHttpRequest("POST", profilesUrl, profileBody.toString(), token);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Profile creation failed", e);
                    // If duplicate key, it means trigger created it. Ignore.
                    if (e.getMessage() == null || !e.getMessage().contains("duplicate key")) {
                         Log.w(TAG, "Profile creation warning: " + e.getMessage());
                         // We don't fail the whole process here, as the user is created.
                    }
                }

                // 3. Handle Success Flow
                if (token != null) {
                    // Auto-confirmed (Setting is OFF) -> Go to Dashboard
                    postSuccess(callback, role, fullName);
                } else {
                    // Email confirmation required (Setting is ON) -> Tell user to check email
                    postError(callback, "Account created! Please check your email to verify your account before logging in.");
                }

            } catch (Exception e) {
                Log.e(TAG, "SignUp Error", e);
                String msg = e.getMessage();
                if (msg != null && msg.contains("HTTP 500")) {
                     postError(callback, "Server Error 500. Check Supabase logs. Details: " + msg);
                } else if (msg != null && (msg.contains("User already registered") || msg.contains("already registered"))) {
                     postError(callback, "This email is already registered. Please log in.");
                } else if (msg != null && (msg.contains("Password should be at least") || msg.contains("weak_password"))) {
                     postError(callback, "Password needs 8+ chars, 1 number, 1 symbol, 1 upper & 1 lowercase.");
                } else {
                     postError(callback, msg != null ? msg : "Sign up failed");
                }
            }
        });
    }

    public static void login(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Login
                String authUrl = SUPABASE_URL + "/auth/v1/token?grant_type=password";
                JSONObject loginBody = new JSONObject();
                loginBody.put("email", email);
                loginBody.put("password", password);

                String authResponse = makeHttpRequest("POST", authUrl, loginBody.toString(), null);
                JSONObject authJson = new JSONObject(authResponse);
                
                if (!authJson.has("access_token")) {
                    postError(callback, "Login failed: No access token.");
                    return;
                }
                
                accessToken = authJson.getString("access_token");
                String userId = authJson.getJSONObject("user").getString("id");

                // 2. Fetch Role and Full Name
                String queryUrl = SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId + "&select=role,full_name";
                String profileResponse = makeHttpRequest("GET", queryUrl, null, accessToken);
                
                JSONArray profiles = new JSONArray(profileResponse);
                if (profiles.length() > 0) {
                    JSONObject profile = profiles.getJSONObject(0);
                    String role = profile.getString("role");
                    String fullName = profile.optString("full_name", "User");
                    currentFullName = fullName;
                    postSuccess(callback, role, fullName);
                } else {
                    postError(callback, "User profile not found.");
                }

            } catch (Exception e) {
                 Log.e(TAG, "Login Error", e);
                 String msg = e.getMessage();
                 
                 // Smart Error Handling
                 if (msg != null && msg.contains("HTTP 400")) {
                     if (msg.contains("Email not confirmed")) {
                         postError(callback, "Please confirm your email address before logging in.");
                     } else if (msg.contains("Invalid login credentials")) {
                         postError(callback, "Invalid email or password.");
                     } else {
                         postError(callback, "Login failed: " + msg);
                     }
                 } else {
                     postError(callback, msg != null ? msg : "Login failed");
                 }
            }
        });
    }
    
    public static void sendPasswordReset(String email, AuthCallback callback) {
        executor.execute(() -> {
            try {
                String authUrl = SUPABASE_URL + "/auth/v1/recover";
                JSONObject body = new JSONObject();
                body.put("email", email);
                
                makeHttpRequest("POST", authUrl, body.toString(), null);
                postSuccess(callback, null, null);
                
            } catch (Exception e) {
                Log.e(TAG, "Reset Password Error", e);
                postError(callback, e.getMessage());
            }
        });
    }
    
    public static void updatePassword(String newPassword, String token, AuthCallback callback) {
        executor.execute(() -> {
            if (newPassword == null || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
                postError(callback, "Password needs 8+ chars, 1 number, 1 symbol, 1 upper & 1 lowercase.");
                return;
            }
            
            try {
                String authUrl = SUPABASE_URL + "/auth/v1/user";
                JSONObject body = new JSONObject();
                body.put("password", newPassword);
                
                makeHttpRequest("PUT", authUrl, body.toString(), token);
                postSuccess(callback, null, null);
                
            } catch (Exception e) {
                Log.e(TAG, "Update Password Error", e);
                postError(callback, e.getMessage());
            }
        });
    }
    
    public static void logout() {
        accessToken = null;
        currentFullName = null;
    }
    
    public static String getCurrentFullName() {
        return currentFullName;
    }

    private static String makeHttpRequest(String method, String urlString, String jsonBody, String token) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            } else {
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
            }
            
            if (jsonBody != null) {
                conn.setDoOutput(true);
                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                 try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    throw new Exception("HTTP " + code + ": " + response.toString());
                }
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void postSuccess(AuthCallback callback, String role, String fullName) {
        mainHandler.post(() -> callback.onSuccess(role, fullName));
    }

    private static void postError(AuthCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
