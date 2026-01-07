package com.example.infrastructureproject;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatBotMainActivity extends AppCompatActivity {

    private static final String TAG = "ChatBotMainActivity";
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton closeButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private GenerativeModelFutures model;
    private Executor executor;
    private final String SYSTEM_PROMPT = "You are an Infrastructure Reporting Assistant. You help users with infrastructure damage questions.";

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_ai_assistant);

        recyclerView = findViewById(R.id.chatRecycleView);
        messageInput = findViewById(R.id.chatInput);
        sendButton = findViewById(R.id.sendButton);
        closeButton = findViewById(R.id.closeButton);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        GenerativeModel setup = new GenerativeModel(BuildConfig.GEMINI_MODEL, BuildConfig.GEMINI_API_KEY);
        model = GenerativeModelFutures.from(setup);
        executor = Executors.newSingleThreadExecutor();

        messageList.add(new ChatMessage("Hello! How can I help you today?", false));
        chatAdapter.notifyItemInserted(messageList.size()-1);

        sendButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupPopupStyle() {
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);
        getWindow().setAttributes(params);
    }

    public void sendMessage(){
        String message = messageInput.getText().toString().trim();

        if(message.isEmpty()){
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        messageList.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(messageList.size()-1);
        recyclerView.smoothScrollToPosition(messageList.size()-1);

        messageInput.setText("");

        messageList.add(new ChatMessage("Obtaining response...", false));
        final int loadingMessageIndex = messageList.size() - 1;
        chatAdapter.notifyItemInserted(loadingMessageIndex);
        recyclerView.smoothScrollToPosition(loadingMessageIndex);

        // Build content with conversation history (excluding the loading message)
        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.addText(SYSTEM_PROMPT + "\n\n");

        for(int i = 0; i < messageList.size() - 1; i++){
            ChatMessage chat = messageList.get(i);
            if(chat.isUser()){
                contentBuilder.addText("User: " + chat.getMessage() + "\n");
            }else if(!chat.getMessage().equals("Obtaining response...")){
                contentBuilder.addText("Assistant: " + chat.getMessage() + "\n");
            }
        }

        Content content = contentBuilder.build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            messageList.remove(messageList.size()-1);
                            chatAdapter.notifyItemRemoved(messageList.size());

                            String aiResponse = result.getText();
                            Log.d(TAG, "AI Response received: " + aiResponse);
                            messageList.add(new ChatMessage(aiResponse,false));
                            chatAdapter.notifyItemInserted(messageList.size()-1);
                            recyclerView.smoothScrollToPosition(messageList.size()-1);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing AI response", e);
                            messageList.add(new ChatMessage("Error processing response: " + e.getMessage(), false));
                            chatAdapter.notifyItemInserted(messageList.size()-1);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "API call failed", t);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageList.remove(messageList.size()-1);
                        chatAdapter.notifyItemRemoved(messageList.size());

                        String errorMsg = "Sorry, I couldn't generate a response. Error: " + t.getMessage();
                        Log.e(TAG, errorMsg);
                        messageList.add(new ChatMessage(errorMsg, false));
                        chatAdapter.notifyItemInserted(messageList.size()-1);
                        recyclerView.smoothScrollToPosition(messageList.size()-1);
                    }
                });
            }
        }, executor);
    }
}
