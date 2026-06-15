package com.veggo.app.domain.model;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_PRODUCT = 2;
    public static final int TYPE_WELCOME = 3;

    private final String message;
    private final int type;
    private final long timestamp;

    public ChatMessage(String message, int type) {
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() { return message; }
    public int getType() { return type; }
    public long getTimestamp() { return timestamp; }
}
