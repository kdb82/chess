package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.GameDao;
import io.javalin.websocket.*;
import org.eclipse.jetty.websocket.api.Session;
import serialization.GameStateDTO;
import serialization.GameStateMapper;
import webSocketMessages.Notification;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler implements WsConnectHandler, WsMessageHandler, WsCloseHandler {
    private final Gson gson = new Gson();
    private final ConnectionManager connections = new ConnectionManager();
    private static final Map<Integer, Set<Session>> watchers = new ConcurrentHashMap<>();

    private final GameDao gameDao;

    public WebSocketHandler(GameDao gameDao) {
        this.gameDao = gameDao;
    }

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
                            new Notification(Notification.Type.JOIN, current_user + " joined game " + gameId +
                                    (color != null ? " (" + color + ")" : "")));
                    break;
                }

                case "OBSERVE": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    var current_user = (String) msg.get("current_user");
                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

//                    sendTo(session, new Notification(Notification.Type.JOIN, "Observing game " + gameId));
                    broadcastToGame(gameId, session, new Notification(Notification.Type.JOIN, current_user + " joined game " + gameId));
                    break;
                }

                case "MOVE": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    String from = (String) msg.get("from");
                    String to = (String) msg.get("to");
                    boolean promotion = Boolean.TRUE.equals(msg.get("promotion"));

                    try {
                        ChessGame game = gameDao.loadGameState(gameId);
                        ChessPosition start = parseSquare(session, from);
                        ChessPosition end = parseSquare(session, to);

                        ChessPiece.PieceType promoPiece = null;
                        if (promotion) {
                            promoPiece = ChessPiece.PieceType.QUEEN;
                        }
                        ChessMove move = new ChessMove(start, end, promoPiece);
                        game.makeMove(move);

                        gameDao.saveGameState(gameId, game);
                        GameStateDTO dto = GameStateMapper.gameToDTO(game);

                        String json = gson.toJson(dto);
                        broadcastToGame(gameId, session, new Notification(Notification.Type.LOAD_GAME, json));

                    } catch (DataAccessException e) {
                        sendTo(session, new Notification(Notification.Type.ERROR,
                                "Database error: " + e.getMessage()));
                    } catch (Exception e) {
                        sendTo(session, new Notification(Notification.Type.ERROR,
                                "Invalid move: " + e.getMessage()));
                    }
                    break;
                }

                case "RESIGN": {
                    //implement
                    break;
                }

                case "LEAVE": {
                    int gameId = ((Number) msg.get("gameId")).intValue();
                    boolean isPlayer = (boolean) msg.get("isPlayer");
                    String current_user = (String) msg.get("current_user");
                    removeWatcher(gameId, session);
//                    sendTo(session, new Notification(Notification.Type.LEAVE, "Left game " + gameId));
                    if(isPlayer) {
                        broadcastToGame(gameId, session,
                                new Notification(Notification.Type.LEAVE, current_user + " (player) left game " + gameId));
                    } else {
                        broadcastToGame(gameId, session,
                                new Notification(Notification.Type.LEAVE, current_user + " (observer) left game " + gameId));
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
    private ChessPosition parseSquare(Session s, String sq) {
        if (sq == null || sq.length() != 2) {
            sendTo(s, new Notification(Notification.Type.ERROR, "invalid square"));
        }
        char letter = Character.toLowerCase(sq != null ? sq.charAt(0) : 0);
        char rank = sq != null ? sq.charAt(1) : 0;

        int col = letter - 'a' + 1;
        int row = rank - '0';

        if (col < 1 || col > 8 || row < 1 || row > 8) {
            sendTo(s, new Notification(Notification.Type.ERROR, "invalid square"));
        }

        return new ChessPosition(row, col);
    }



}