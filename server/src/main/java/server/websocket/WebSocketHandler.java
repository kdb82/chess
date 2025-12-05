package server.websocket;

import chess.*;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.GameDao;
import io.javalin.websocket.*;
import org.eclipse.jetty.websocket.api.Session;
import service.GameService;
import webSocketMessages.Notification;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {
    private final Gson gson = new Gson();
    private final ConnectionManager connections = new ConnectionManager();
    private static final Map<Integer, Set<Session>> watchers = new ConcurrentHashMap<>();

    private final GameService gameService;

    public WebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void handleConnect(WsConnectContext ctx) {
        System.out.println("[WS SERVER] connected");
        ctx.enableAutomaticPings();
        Session session = ctx.session;
        connections.add(session);
        sendTo(session, new Notification(Notification.Type.CONNECT, "Connected"));
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        System.out.println("[WS SERVER] incoming: " + ctx.message());
        Session session = ctx.session;
        try {
            var msg = gson.fromJson(ctx.message(), Map.class);
            String type = (String) msg.get("type");
            if (type == null) {
                sendTo(session, new Notification(Notification.Type.ERROR, "missing type"));
                return;
            }

            switch (type) {
                case "JOIN": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    String color = (String) msg.get("color");
                    String current_user = (String) msg.get("current_user");

                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

//                    sendTo(session, new Notification(Notification.Type.JOIN,
//                            (color != null ? "Player joined as " + color : "Joined as observer") + " (gameId " + gameId + ")"));

                    broadcastToGame(gameId, session,
                            new Notification(Notification.Type.JOIN, current_user + " joined game " +
                                    (color != null ? "(" + color + ")" : "")));
                    break;
                }

                case "OBSERVE": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    var current_user = (String) msg.get("current_user");
                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

//                    sendTo(session, new Notification(Notification.Type.JOIN, "Observing game " + gameId));
                    broadcastToGame(gameId, session, new Notification(Notification.Type.JOIN, current_user + " joined game as observer."));
                    break;
                }

                case "MOVE": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    String from = (String) msg.get("from");
                    String to = (String) msg.get("to");
                    boolean promotion = Boolean.TRUE.equals(msg.get("promotion"));
                    String authToken = (String) msg.get("authToken");

                    try {
                        ChessPosition start = parseSquare(from);
                        ChessPosition end = parseSquare(to);
                        ChessPiece.PieceType promoPiece = null;
                        if (promotion) {
                            promoPiece = ChessPiece.PieceType.QUEEN;
                        }
                        ChessMove move = new ChessMove(start, end, promoPiece);

                        GameService.MoveResult result = gameService.makeMove(authToken, gameId, move);

                        String json = gson.toJson(result.gameState());
                        broadcastToGame(gameId, null,
                                new Notification(Notification.Type.LOAD_GAME, json));

                        if (result.moveNotification() != null) {
                            broadcastToGame(gameId, null,
                                    new Notification(Notification.Type.NOTIFICATION, result.moveNotification()));
                        }

                        if (result.statusNotification() != null) {
                            broadcastToGame(gameId, null,
                                    new Notification(Notification.Type.NOTIFICATION, result.statusNotification()));
                        }

                    } catch (DataAccessException e) {
                        sendTo(session, new Notification(
                                Notification.Type.ERROR,
                                "Error: database problem: " + e.getMessage()));
                    } catch (Exception e) {
                        sendTo(session, new Notification(
                                Notification.Type.ERROR,
                                "Error: " + e.getMessage()));
                    }
                    break;
                }

                case "RESIGN": {
                    int gameId = ((Number) msg.get("gameId")).intValue();
                    String current_user = (String) msg.get("current_user");

                    try {
                        gameService.resignGame(gameId, current_user);

                        broadcastToGame(gameId, null,
                                new Notification(Notification.Type.NOTIFICATION,
                                        current_user + " resigned. Game over. type leave to leave game."));
                    } catch (Exception e) {
                        sendTo(session, new Notification(
                                Notification.Type.ERROR,
                                "Error: " + e.getMessage()));
                    }
                    break;
                }


                case "LEAVE": {
                    int gameId = ((Number) msg.get("gameId")).intValue();
                    boolean isPlayer = (boolean) msg.get("isPlayer");
                    String current_user = (String) msg.get("current_user");
                    String authToken = (String) msg.get("authToken");
                    removeWatcher(gameId, session);
//                    sendTo(session, new Notification(Notification.Type.LEAVE, "Left game " + gameId));
                    if (isPlayer) {
                        try {
                            gameService.leaveGame(gameId, authToken);
                        } catch (Exception e) {
                            sendTo(session, new Notification(
                                    Notification.Type.ERROR,
                                    "Error: " + e.getMessage()));
                        }
                    }
                    if(isPlayer) {
                        broadcastToGame(gameId, session,
                                new Notification(Notification.Type.NOTIFICATION, current_user + " (player) left game."));
                    } else {
                        broadcastToGame(gameId, session,
                                new Notification(Notification.Type.NOTIFICATION, current_user + " (observer) left game."));
                    }

                    break;
                }
                default: {
                    sendTo(session, new Notification(Notification.Type.ERROR, "Unknown type " + type));
                }
            }
        } catch (Exception e) {
            sendTo(ctx.session,  new Notification(Notification.Type.ERROR, e.getMessage()));
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
    private ChessPosition parseSquare(String sq) {
        if (sq == null || sq.length() != 2) {
            throw new IllegalArgumentException("invalid square: " + sq);
        }
        char letter = Character.toLowerCase(sq.charAt(0));
        char rank = sq.charAt(1);

        int col = letter - 'a' + 1;
        int row = rank - '0';

        if (col < 1 || col > 8 || row < 1 || row > 8) {
            throw new IllegalArgumentException("invalid square: " + sq);
        }

        return new ChessPosition(row, col);
    }



}