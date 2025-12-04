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

//need to extend Endpoint for websocket to work properly
public class WebSocketFacade extends Endpoint {
    private final String baseUrl;
    private final String authToken;
    private final NotificationHandler notificationHandler;
    private final Gson gson = new Gson();
    private Session session;

    public WebSocketFacade(String url, String authToken, NotificationHandler notificationHandler) throws ResponseException {
        this.baseUrl = url;
        this.authToken = authToken;
        this.notificationHandler = notificationHandler;
        try {
            String encoded = URLEncoder.encode(authToken, StandardCharsets.UTF_8);
            String wsUrl = baseUrl.replaceFirst("http", "ws") + "/ws?authToken=" + encoded;
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
                e.printStackTrace();
            }
        });

    }

    @Override
    public void onError(Session session, Throwable thr) {
        System.out.println("[WS CLIENT] onError called!");
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

    public void joinGame(int gameId, String color) throws ResponseException {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "JOIN");
        message.put("gameId", gameId);
        message.put("color", color);
        sendJson(message);
    }

    public void observeGame(int gameId) throws ResponseException {
        sendJson(Map.of("type", "OBSERVE", "gameId", gameId));
    }

    public void makeMove(int gameId, String from, String to, String promotion) throws ResponseException {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MOVE");
        message.put("gameId", gameId);
        message.put("from", from);
        message.put("to", to);
        if (promotion != null && !promotion.isBlank()) {
            message.put("promotion", promotion);
        }
        sendJson(message);
    }

    public void leaveGame(int gameId) throws ResponseException {
        sendJson(Map.of("type", "LEAVE", "gameId", gameId));
    }

    public void resign(int gameId) throws ResponseException {
        sendJson(Map.of("type", "RESIGN", "gameId", gameId));
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