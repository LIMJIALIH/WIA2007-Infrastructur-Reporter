package com.example.infrastructureproject;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HuggingFaceRouterService {
    private static final String TAG = "HFRouterService";
    private static final String API_URL = "https://router.huggingface.co/v1/chat/completions";
    private static final String HF_TOKEN = "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"; // REPLACE THIS

    // List of free models to try (fallback if one fails)
    private static final String[] FREE_MODELS = {
            "deepseek-ai/DeepSeek-V3.2:novita",          // First choice
            "meta-llama/Llama-3.3-70B-Instruct:free",    // Second choice
            "Qwen/Qwen2.5-32B-Instruct:free",           // Third choice
            "mistralai/Mixtral-8x7B-Instruct-v0.1:free" // Fourth choice
    };

    private int currentModelIndex = 0;

    public HuggingFaceRouterService() {
        Log.d(TAG, "Service initialized with " + FREE_MODELS.length + " available models");
    }

    public void getResponse(String userMessage, AIResponseCallback callback) {
        // Try with current model
        callAPIWithModel(userMessage, callback, currentModelIndex);
    }

    private void callAPIWithModel(String userMessage, AIResponseCallback callback, int modelIndex) {
        if (modelIndex >= FREE_MODELS.length) {
            // All models failed, use fallback
            callback.onError("All models unavailable. Using local response.");
            return;
        }

        String model = FREE_MODELS[modelIndex];
        Log.d(TAG, "Trying model: " + model);

        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + HF_TOKEN);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                // Create messages with infrastructure context
                JSONArray messages = new JSONArray();

                // System prompt for infrastructure assistant
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", createSystemPrompt());
                messages.put(systemMsg);

                // User message
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage);
                messages.put(userMsg);

                // Request body
                JSONObject json = new JSONObject();
                json.put("model", model);
                json.put("messages", messages);
                json.put("temperature", 0.7);
                json.put("max_tokens", 250);
                json.put("stream", false);

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    // Parse and return response
                    JSONObject responseJson = new JSONObject(response.toString());
                    JSONArray choices = responseJson.getJSONArray("choices");
                    if (choices.length() > 0) {
                        String content = choices.getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        Log.d(TAG, "Success with model: " + model);
                        callback.onSuccess(content.trim());
                    } else {
                        // Try next model
                        tryNextModel(userMessage, callback, modelIndex + 1);
                    }

                } else {
                    Log.w(TAG, "Model " + model + " failed with code: " + responseCode);
                    // Try next model
                    tryNextModel(userMessage, callback, modelIndex + 1);
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error with model " + model + ": " + e.getMessage());
                // Try next model
                tryNextModel(userMessage, callback, modelIndex + 1);
            }
        }).start();
    }

    private void tryNextModel(String userMessage, AIResponseCallback callback, int nextIndex) {
        if (nextIndex < FREE_MODELS.length) {
            Log.d(TAG, "Trying next model index: " + nextIndex);
            callAPIWithModel(userMessage, callback, nextIndex);
        } else {
            callback.onError("Unable to connect to AI service. Please try again later.");
        }
    }

    private String createSystemPrompt() {
        return "You are an AI assistant for an infrastructure reporting application called 'Infrastructure Reporter'. " +
                "The app allows citizens to report infrastructure issues to municipal authorities.\n\n" +

                "App Features:\n" +
                "1. Users submit reports with photos, location, and description\n" +
                "2. Reports are categorized: Road, Utilities, Facilities, Environment\n" +
                "3. Severity levels: High, Medium, Low\n" +
                "4. Report statuses: Pending, Accepted, Rejected\n" +
                "5. Users view reports in 'My Reports' section\n\n" +

                "Your Role:\n" +
                "- Provide helpful, concise answers about infrastructure reporting\n" +
                "- Explain how to submit reports effectively\n" +
                "- Help understand report status and categories\n" +
                "- Give practical advice for documenting issues\n" +
                "- Keep responses under 150 words unless detailed explanation is needed\n" +
                "- Be friendly and professional";
    }

    public interface AIResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}