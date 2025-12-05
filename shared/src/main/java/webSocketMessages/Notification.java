package webSocketMessages;

import com.google.gson.Gson;

public record Notification(Type type, String message) {
    public enum Type {
        JOIN,
        LEAVE,
        OBSERVE,
        LOAD_GAME,
        MOVE,
        ERROR
    }

    @SuppressWarnings("NullableProblems")
    public String toString() {
        return new Gson().toJson(this);
    }
}
