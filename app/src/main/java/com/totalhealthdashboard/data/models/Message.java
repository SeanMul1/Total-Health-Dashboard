package com.totalhealthdashboard.data.models;

public class Message {
    private String messageId;
    private String sender; // "user" or "assistant"
    private String text;
    private long timestamp;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String messageId, String sender, String text, long timestamp) {
        this.messageId = messageId;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
