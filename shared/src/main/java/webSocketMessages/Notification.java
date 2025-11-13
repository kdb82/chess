package webSocketMessages;

import com.google.gson.Gson;

public record Notification(Type type, String message) {
    public enum Type {
        JOIN,
        LEAVE,
        MOVE,
        Error
    }

    public String toString() {
        return new Gson().toJson(this);
    }
}
