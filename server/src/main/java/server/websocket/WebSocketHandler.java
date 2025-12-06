package server.websocket;

import chess.*;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.websocket.*;
import org.eclipse.jetty.websocket.api.Session;
import serialization.GameStateDTO;
import service.GameService;
import webSocketMessages.Notification;
import websocket.messages.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static websocket.messages.ServerMessage.ServerMessageType.ERROR;

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
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        System.out.println("[WS SERVER] incoming: " + ctx.message());
        Session session = ctx.session;
        try {
            var msg = gson.fromJson(ctx.message(), Map.class);
            String commandType = (String) msg.get("commandType");
            if (commandType != null) {
                handleUserGameCommand(session, msg, commandType);
                return;
            }

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

//                    sendTo(session, new Notification(Notification.Type.LOAD_GAME,
//                            (color != null ? "Player joined as " + color : "Joined as observer") + " (gameId " + gameId + ")"));

                    broadcastToGame(gameId, session,
                            new Notification(Notification.Type.NOTIFICATION, current_user + " joined game " +
                                    (color != null ? "(" + color + ")" : "")));
                    break;
                }

                case "OBSERVE": {
                    int gameId = ((Number)  msg.get("gameId")).intValue();
                    var current_user = (String) msg.get("current_user");
                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

//                    sendTo(session, new Notification(Notification.Type.JOIN, "Observing game " + gameId));
                    broadcastToGame(gameId, session, new Notification(Notification.Type.CONNECT, current_user + " joined game as observer."));
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
                    String authToken = (String) msg.get("authToken");

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

    @SuppressWarnings("unchecked")
    private void handleUserGameCommand(Session session, Map msg, String commandType) {
        switch (commandType) {

            // ======================== CONNECT ========================
            case "CONNECT" -> {
                String authToken = (String) msg.get("authToken");
                Number gameIdNum = (Number) msg.get("gameID");

                if (authToken == null || gameIdNum == null) {
                    sendErrorServerMessage(session, "Error: missing authToken or gameID");
                    return;
                }

                int gameId = gameIdNum.intValue();

                try {
                    // You should implement this in GameService:
                    //   public GameStateDTO loadGameState(String authToken, int gameId)
                    GameStateDTO gameState = gameService.loadGameState(authToken, gameId);

                    // Track this session as a watcher of this game
                    watchers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(session);

                    // 1) LOAD_GAME to the root client (the sender)
                    ServerMessage load = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME);
                    load.setGame(gameState);
                    sendServerMessage(session, load);

                    String username = gameService.getUsernameForAuth(authToken);

                    ServerMessage note = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
                    note.setMessage(username + " connected to game " + gameId);
                    broadcastToGame(gameId, session, note);

                } catch (Exception e) {
                    sendErrorServerMessage(session, "Error: " + e.getMessage());
                }
            }

            case "MAKE_MOVE" -> {
                String authToken = (String) msg.get("authToken");
                Number gameIdNum = (Number) msg.get("gameID");

                if (authToken == null || gameIdNum == null) {
                    sendErrorServerMessage(session, "Error: missing authToken or gameID");
                    return;
                }

                int gameId = gameIdNum.intValue();

                try {
                    Object moveObj = msg.get("move");
                    if (moveObj == null) {
                        sendErrorServerMessage(session, "Error: missing move");
                        return;
                    }

                    // Let Gson build a ChessMove from the nested move object
                    ChessMove move = gson.fromJson(gson.toJson(moveObj), ChessMove.class);

                    GameService.MoveResult result = gameService.makeMove(authToken, gameId, move);

                    GameStateDTO dto = result.gameState();

                    // 1) LOAD_GAME -> everybody in the game (including sender)
                    ServerMessage load = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME);
                    load.setGame(dto);
                    broadcastToGame(gameId, null, load);

                    // 2) Move notification -> everyone EXCEPT the sender
                    if (result.moveNotification() != null) {
                        ServerMessage moveMsg = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
                        moveMsg.setMessage(result.moveNotification());
                        // exclude sender so they only see LOAD_GAME for normal moves
                        broadcastToGame(gameId, session, moveMsg);
                    }

                    // 3) Status notification (check / checkmate / stalemate) -> everyone
                    if (result.statusNotification() != null) {
                        ServerMessage statusMsg = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
                        statusMsg.setMessage(result.statusNotification());
                        // include sender; tests expect an extra NOTIFICATION only in this case
                        broadcastToGame(gameId, null, statusMsg);
                    }

                } catch (Exception e) {
                    sendErrorServerMessage(session, "Error: " + e.getMessage());
                }
            }



            // ======================== RESIGN ========================
            case "RESIGN" -> {
                String authToken = (String) msg.get("authToken");
                Number gameIdNum = (Number) msg.get("gameID");

                if (authToken == null || gameIdNum == null) {
                    sendErrorServerMessage(session, "Error: missing authToken or gameID");
                    return;
                }

                int gameId = gameIdNum.intValue();

                try {
                    // Get username from auth token
                    String username = gameService.getUsernameForAuth(authToken);

                    // Your GameService.resignGame takes (gameId, username)
                    gameService.resignGame(gameId, username);

                    // Notify ALL clients in the game (including the resigner)
                    ServerMessage note = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
                    note.setMessage(username + " resigned. Game over.");
                    broadcastToGame(gameId, null, note);

                } catch (Exception e) {
                    sendErrorServerMessage(session, "Error: " + e.getMessage());
                }
            }

            // ======================== LEAVE ========================
            case "LEAVE" -> {
                String authToken = (String) msg.get("authToken");
                Number gameIdNum = (Number) msg.get("gameID");

                if (authToken == null || gameIdNum == null) {
                    sendErrorServerMessage(session, "Error: missing authToken or gameID");
                    return;
                }

                int gameId = gameIdNum.intValue();

                // Remove from watcher set first so they stop receiving messages
                removeWatcher(gameId, session);

                try {
                    String username = gameService.getUsernameForAuth(authToken);

                    // For players, this clears their seat; for observers (not seated) it does nothing.
                    gameService.leaveGame(gameId, authToken);

                    // Notify everyone else in the game that this user left
                    ServerMessage note = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
                    note.setMessage(username + " left the game.");
                    broadcastToGame(gameId, session, note);

                } catch (Exception e) {
                    sendErrorServerMessage(session, "Error: " + e.getMessage());
                }
            }

            // ======================== Unknown ========================
            default -> sendErrorServerMessage(session, "Error: Unknown commandType " + commandType);
        }
    }


    @Override
    public void handleClose(WsCloseContext ctx) {
        System.out.println("[WS] closed");
        Session session = ctx.session;
        connections.remove(session);
        watchers.values().forEach(set -> set.remove(session));
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



    private void sendServerMessage(Session s, ServerMessage m) {
        if (s == null || !s.isOpen()) return;
        try {
            String json = gson.toJson(m);
            System.out.println("[WS SERVER] sendTo(ServerMessage) -> " + json);
            s.getRemote().sendString(json);
        } catch (Exception ex) {
            System.out.println("[WS SERVER] sendTo ERROR: " + ex.getMessage());
        }
    }

    private void broadcastToGame(int gameID, Session exclude, ServerMessage m) {
        var set = watchers.get(gameID);
        if (set == null || set.isEmpty()) return;
        String json = gson.toJson(m);
        for (Session s : set) {
            if (s.isOpen() && s != exclude) {
                try {
                    s.getRemote().sendString(json);
                } catch (Exception ignored) {}
            }
        }
    }

    private void sendErrorServerMessage(Session session, String message) {
        ServerMessage error = new ServerMessage(ERROR);
        error.setErrorMessage(message);  // make sure your ServerMessage has a setter or a constructor for this
        sendServerMessage(session, error);
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