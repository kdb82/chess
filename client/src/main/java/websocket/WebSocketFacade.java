package websocket;

import com.google.gson.Gson;
import exception.ResponseException;
import webSocketMessages.Notification;

import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class WebSocketFacade extends Endpoint {
    private final NotificationHandler notificationHandler;
    private final Gson gson = new Gson();
    private Session session;

    public WebSocketFacade(String url, String authToken, NotificationHandler notificationHandler) throws ResponseException {
        this.notificationHandler = notificationHandler;
        try {
            String encoded = URLEncoder.encode(authToken, StandardCharsets.UTF_8);
            String wsUrl = url.replaceFirst("http", "ws") + "/ws?authToken=" + encoded;
            URI uri = new URI(wsUrl);

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, uri);
        } catch (URISyntaxException | IOException | jakarta.websocket.DeploymentException ex) {
            throw new ResponseException(ResponseException.Code.ServerError, ex.getMessage());
        }
    }


    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        this.session = session;
        System.out.println("[WS] connected");

        session.addMessageHandler(String.class, message -> {
            try {
                Notification notification = gson.fromJson(message, Notification.class);
                if (notificationHandler != null) {
                    notificationHandler.notify(notification);
                }
            } catch (Exception e) {
                System.out.println("[WS CLIENT] ERROR parsing message:");
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        });

    }

    @Override
    public void onError(Session session, Throwable thr) {
        System.out.println("[WS CLIENT] onError called!");
        //noinspection CallToPrintStackTrace
        thr.printStackTrace();
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("[WS closed]");
    }

    public void close() {
        try {
            if (session != null && session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "client closed"));
            }
        } catch (IOException ignored) {}
    }

    public void joinGame(int gameId, String color, String current_user) throws ResponseException {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "JOIN");
        message.put("gameId", gameId);
        message.put("color", color);
        message.put("current_user", current_user);
        sendJson(message);
    }

    public void observeGame(int gameId, String current_user) throws ResponseException {
        sendJson(Map.of("type", "OBSERVE", "gameId", gameId, "current_user", current_user));
    }

    public void makeMove(int gameId, String from, String to, boolean promotion, String authToken) throws ResponseException {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MOVE");
        message.put("gameId", gameId);
        message.put("from", from);
        message.put("to", to);
        if (promotion) {
            message.put("promotion", true);
        }
        message.put("authToken", authToken);
        sendJson(message);
    }

    public void leaveGame(int gameId, boolean isPlayer, String current_user, String authToken) throws ResponseException {
        sendJson(Map.of("type", "LEAVE", "gameId", gameId,  "isPlayer", isPlayer, "current_user", current_user,  "authToken", authToken));
    }

    public void resign(int gameId, String current_user) throws ResponseException {
        sendJson(Map.of("type", "RESIGN", "gameId", gameId,  "current_user", current_user));
    }


    private void sendJson(Map<String, ?> message) throws ResponseException {
        try {
            if (session == null || !session.isOpen()) {
                throw new IllegalStateException("WebSocket not connected");
            }
            session.getAsyncRemote().sendText(gson.toJson(message));
        } catch (Exception ex) {
            throw new ResponseException(ResponseException.Code.ServerError, ex.getMessage());
        }
    }

}