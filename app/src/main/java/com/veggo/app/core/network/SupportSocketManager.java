package com.veggo.app.core.network;

import androidx.annotation.Nullable;

import com.veggo.app.core.utils.Constants;
import com.veggo.app.core.utils.UrlUtils;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SupportSocketManager {

    public interface Listener {
        void onConnected();
        void onDisconnected();
        void onNewMessage(JSONObject messageJson);
        void onTypingUpdate(JSONObject typingJson);
        void onUnreadUpdate(int unreadCountUser);
        void onError(String message);
        default void onRealtimeEvent(JSONObject eventJson) {}
        default void onUserNotification(JSONObject notificationJson) {}
        default void onOrderUpdated(JSONObject orderJson) {}
        default void onPromotionChanged(JSONObject promotionJson) {}
    }

    @Nullable
    private Socket socket;

    public void connect(String accessToken, Listener listener) {
        disconnect();
        try {
            String socketBaseUrl = UrlUtils.socketBaseUrlFromApiBaseUrl(Constants.API_BASE_URL);
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionAttempts = Integer.MAX_VALUE;
            opts.reconnectionDelay = 1000;
            opts.reconnectionDelayMax = 5000;
            opts.timeout = 10000;
            opts.auth = new java.util.HashMap<>();
            opts.auth.put("token", accessToken);

            socket = IO.socket(socketBaseUrl, opts);
            socket.on(Socket.EVENT_CONNECT, args -> {
                if (listener != null) listener.onConnected();
            });
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                if (listener != null) listener.onDisconnected();
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                if (listener != null) listener.onError("Socket connect error");
            });
            socket.on("message:new", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onNewMessage((JSONObject) args[0]);
                } else {
                    listener.onError("Invalid message payload");
                }
            });
            socket.on("typing:update", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onTypingUpdate((JSONObject) args[0]);
                }
            });
            socket.on("support:unread", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    JSONObject json = (JSONObject) args[0];
                    listener.onUnreadUpdate(json.optInt("unreadCountUser", 0));
                }
            });
            socket.on("realtime:event", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onRealtimeEvent((JSONObject) args[0]);
                }
            });
            socket.on("user:notification", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onUserNotification((JSONObject) args[0]);
                }
            });
            socket.on("order:created", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onOrderUpdated((JSONObject) args[0]);
                }
            });
            socket.on("order:updated", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onOrderUpdated((JSONObject) args[0]);
                }
            });
            socket.on("order:status-updated", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onOrderUpdated((JSONObject) args[0]);
                }
            });
            socket.on("order:payment-updated", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onOrderUpdated((JSONObject) args[0]);
                }
            });
            socket.on("promotion:changed", (Emitter.Listener) args -> {
                if (listener == null || args == null || args.length == 0) return;
                if (args[0] instanceof JSONObject) {
                    listener.onPromotionChanged((JSONObject) args[0]);
                }
            });
            socket.connect();
        } catch (URISyntaxException e) {
            if (listener != null) listener.onError("Invalid socket URL");
        } catch (Exception e) {
            if (listener != null) listener.onError("Socket init failed");
        }
    }

    public void joinConversation(String conversationId, io.socket.client.Ack ack) {
        if (socket == null) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("conversationId", conversationId);
        } catch (Exception ignored) {
        }
        socket.emit("conversation:join", payload, ack);
    }

    public void sendMessage(String conversationId, String text, String clientMessageId, io.socket.client.Ack ack) {
        if (socket == null) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("conversationId", conversationId);
            payload.put("text", text);
            payload.put("clientMessageId", clientMessageId);
        } catch (Exception ignored) {
        }
        socket.emit("message:send", payload, ack);
    }

    public void emitTypingUpdate(String conversationId, boolean isTyping) {
        if (socket == null) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("conversationId", conversationId);
            payload.put("isTyping", isTyping);
        } catch (Exception ignored) {
        }
        socket.emit("typing:update", payload);
    }

    public void disconnect() {
        if (socket != null) {
            try {
                socket.disconnect();
                socket.off();
            } catch (Exception ignored) {
            }
            socket = null;
        }
    }
}
