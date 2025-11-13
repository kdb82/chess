package server.websocket;

import com.google.gson.Gson;
import io.javalin.websocket.*;
import org.eclipse.jetty.websocket.api.Session;
import webSocketMessages.Notification;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {
    private final Gson gson = new Gson();
    private final ConnectionManager connections = new ConnectionManager();
    private final Map<Integer, Set<Session>> watchers = new ConcurrentHashMap<>();


    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("[WS] connected");
        ctx.enableAutomaticPings();
        Session session = ctx.session;
        sendTo(session, new Notification(Notification.Type.JOIN, "Connected"));
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        Session session = ctx.session;
        try {
            Map<String, Object> msg = gson.fromJson(ctx.message(), Map.class);
            String type = (String) msg.get("type");
            if (type == null) {
                sendTo(session, new Notification(Notification.Type.Error, "missing type"));
                return;
            }

            switch (type) {
                case "JOIN": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    String color = (String) msg.get("color");

                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

                    sendTo(session, new Notification(Notification.Type.JOIN,
                            (color != null ? "Joined as " + color : "Joined as observer") + " (game " + gameId + ")"));

                    broadcastToGame(gameId, session,
                            new Notification(Notification.Type.JOIN, "Player joined game " + gameId +
                                    (color != null ? " (" + color + ")" : "")));
                }

                case "OBSERVE": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

                    sendTo(session, new Notification(Notification.Type.JOIN, "Observing game " + gameId));
                    broadcastToGame();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("Websocket closed");
    }

    private void sendTo(Session s, Notification n) {
        if (s == null || !s.isOpen()) return;
        try {
            s.getRemote().sendString(n.toString());
        } catch (Exception ignored) {}
    }

    private void broadcastToGame(int gameID, Session exclude, Notification n) {
        var set = watchers.get(gameID);
        if (set == null || set.isEmpty()) return;
        String json = n.toString();
        for (Session s : set) {
            if (s.isOpen() && s != exclude) {
                try { s.getRemote().sendString(json); } catch (Exception ignored) {}
            }
        }
    }
}