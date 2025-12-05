package webSocketMessages;

import com.google.gson.Gson;

public record Notification(Type type, String message) {
    public enum Type {
        CONNECT,
        JOIN,
        LEAVE,
        OBSERVE,
        LOAD_GAME,
        NOTIFICATION,
        ERROR
    }

    @SuppressWarnings("NullableProblems")
    public String toString() {
        return new Gson().toJson(this);
    }
}
