package server.websocket;

import com.google.gson.Gson;
import io.javalin.websocket.*;
import org.eclipse.jetty.websocket.api.Session;
import webSocketMessages.Notification;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {
    private final Gson gson = new Gson();
    private final ConnectionManager connections = new ConnectionManager();
    private static final Map<Integer, Set<Session>> watchers = new ConcurrentHashMap<>();


    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("[WS SERVER] connected");
        ctx.enableAutomaticPings();
        Session session = ctx.session;
        connections.add(session);
        sendTo(session, new Notification(Notification.Type.JOIN, "Connected"));
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        System.out.println("[WS SERVER] incoming: " + ctx.message());
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
                            (color != null ? "Player joined as " + color : "Joined as observer") + " (gameId " + gameId + ")"));

                    broadcastToGame(gameId, session,
                            new Notification(Notification.Type.JOIN, "Player joined game " + gameId +
                                    (color != null ? " (" + color + ")" : "")));
                    break;
                }

                case "OBSERVE": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

                    sendTo(session, new Notification(Notification.Type.JOIN, "Observing game " + gameId));
                    broadcastToGame(gameId, session, new Notification(Notification.Type.JOIN, "Observer joined game " + gameId));
                    break;
                }

                case "MOVE": {
                    //Implement
                    break;
                }

                case "RESIGN": {
                    //implement
                    break;
                }

                case "LEAVE": {
                    int gameId = ((Number) msg.get("gameId")).intValue();
                    removeWatcher(gameId, session);
                    sendTo(session, new Notification(Notification.Type.LEAVE, "Left game " + gameId));
                    broadcastToGame(gameId, session,
                            new Notification(Notification.Type.LEAVE, "A player left game " + gameId));
                    break;
                }
                default: {
                    sendTo(session, new Notification(Notification.Type.Error, "Unknown type " + type));
                }
            }
        } catch (Exception e) {
            sendTo(ctx.session,  new Notification(Notification.Type.Error, e.getMessage()));
        }
    }

    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("[WS] closed");
        Session session = ctx.session;
        connections.remove(session);
    }

    private void removeWatcher(int gameID, Session s) {
        var set = watchers.get(gameID);
        if (set != null) set.remove(s);
    }

    private void sendTo(Session s, Notification n) {
        if (s == null || !s.isOpen()) return;
        try {
            String json = gson.toJson(n);
            System.out.println("[WS SERVER] sendTo -> " + json);
            s.getRemote().sendString(gson.toJson(n));
        } catch (Exception ex) {
            System.out.println("[WS SERVER] sendTo ERROR: " + ex.getMessage());
        }
    }

    private void broadcastToGame(int gameID, Session exclude, Notification n) {
        var set = watchers.get(gameID);
        if (set == null || set.isEmpty()) return;
        String json = gson.toJson(n);
        for (Session s : set) {
            if (s.isOpen() && s != exclude) {
                try { s.getRemote().sendString(json); } catch (Exception ignored) {}
            }
        }
    }


}