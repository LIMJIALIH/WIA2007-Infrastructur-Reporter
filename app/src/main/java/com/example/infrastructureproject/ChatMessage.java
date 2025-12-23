package com.example.infrastructureproject;

public class ChatMessage {

    protected String Message;
    protected boolean isUser;

    public String getMessage() {
        return Message;
    }

    public boolean isUser() {
        return isUser;
    }

    public ChatMessage(String Message, boolean isUser){
        this.Message = Message;
        this.isUser = isUser;
    }


}
