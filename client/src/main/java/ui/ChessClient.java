package ui;

import exception.ResponseException;
import client.ServerFacade;
import requests.*;
import results.*;
import webSocketMessages.Notification;
import websocket.NotificationHandler;
import websocket.WebSocketFacade;



public class ChessClient implements NotificationHandler {
    private final String baseURL;
    private final ServerFacade server;
    private WebSocketFacade ws;
    private Integer currentGameId;
    private boolean drawWhiteSide = true;
    private java.util.List<GameSummary> retrievedGames = java.util.List.of();

    private String authToken;
    private volatile boolean waitingForWs = false;
//    private final Gson gson = new Gson();


    public ChessClient(String serverUrl) {
        this.baseURL = serverUrl;
        this.server = new ServerFacade(this.baseURL);
    }

    public boolean isWaitingForWs() {
        return waitingForWs;
    }

    public String login(String[] params) throws ResponseException {
        if (params.length != 2) {
            throw new IllegalArgumentException("USAGE: login <username> <password>");
        }
        var request = new LoginRequest(params[0], params[1]);
        LoginResult result = server.login(request);
        this.authToken = result.authToken();
        return "Logged in as " + params[0];
    }

    public String register(String[] params) throws ResponseException {
        if (params.length != 3) throw new IllegalArgumentException("USAGE:  register <username> <password> <email>");
        var request = new RegisterRequest(params[0], params[1], params[2]);
        RegisterResult result = server.register(request);
        this.authToken = result.authToken();
        return "Registered and logged in as " + params[0];
    }

    public String logout() throws ResponseException {
        if (authToken == null) throw new IllegalArgumentException("Not logged in.");
        var req = new LogoutRequest(authToken);
        server.logout(req);
        this.authToken = null;
        return "Logged out.";
    }

    public String createGame(String[] params) {
        try {
            if (authToken == null) return "Please login first.";
            if (params.length < 1) return "Usage: create <NAME>";
            var name = String.join(" ", params);
            var req = new CreateGameRequest(name);
            CreateGameResult res = server.createGame(req, authToken);
            return "Created game " + res.gameID() + " named \"" + name + "\"";
        } catch (ResponseException e) {
            return "Create failed: " + e.getMessage();
        }
    }

    public String listGames() {
        try {
            if (authToken == null) return "Please login first.";
            var req = new ListGameRequest(authToken);
            ListGamesResult res = server.listGames(req);

            retrievedGames = (res.games() == null) ? java.util.List.of() : res.games();
            if (retrievedGames.isEmpty()) {
                return "No games found.";
            }

            var sb = getStringBuilder();
            return sb.toString();
        } catch (ResponseException e) {
            return "List failed: " + e.getMessage();
        }
    }

    private StringBuilder getStringBuilder() {
        var sb = new StringBuilder("Games:\nId:    game name:        white user:  black user:\n");
        for (int i = 0; i < retrievedGames.size(); i++) {
            var g = retrievedGames.get(i);
            sb.append(String.format(
                    " %-3d %-20s W:%s  B:%s%n",
                    (i + 1),
                    g.gameName(),
                    nullToDash(g.whiteUsername()),
                    nullToDash(g.blackUsername())
            ));
        }
        return sb;
    }

    private Integer gameIdFromNumber(String num) {
        try {
            int n = Integer.parseInt(num);
            if (n >= 1 && n <= retrievedGames.size()) {
                return retrievedGames.get(n - 1).gameID();
            }
        } catch (NumberFormatException ignore) {
        }
        try {
            return Integer.parseInt(num);
        } catch (Exception e) {
            return null;
        }
    }

    public String joinGame(String[] params) {
        try {
            if (authToken == null) return "Please login first.";
            if (params.length < 2) return "Usage: join <GAME_ID> [WHITE|BLACK]\nRun 'list' to see games";

            Integer gid = gameIdFromNumber(params[0]);
            if (gid == null) {
                return "Invalid selection. Game seat may be taken or game doesn't exist.\nRun 'list' to see games or 'create' to make your own.";
            }

            if (currentGameId != null && currentGameId.equals(gid)) {
                return "Already joined this game.";
            }

            this.currentGameId = gid;
            String color = params[1].toUpperCase();

            var req = new JoinGameRequest(gid, color);
            server.joinGame(req, authToken);

            if (ws == null) ws = new WebSocketFacade(baseURL, authToken, this);
            waitingForWs = true;
            ws.joinGame(gid, color);


            this.drawWhiteSide = !color.equals("BLACK");
            synchronized (System.out) {
                DrawBoard.drawInitial(this.drawWhiteSide);
            }

            String displayName = "#" + gid;
            for (var game : retrievedGames) {
                if (game.gameID() == gid) {
                    displayName = game.gameName();
                    break;
                }
            }

            return "Joined \"" + displayName + "\"" + " as " + color;
        } catch (Exception e) {
            return "Join failed: Game seat may be taken or game doesn't exist.\nRun 'list' to see games or 'create' to make your own.";
        }
    }

    public String observeGame(String[] params) {
        try {
            if (authToken == null) return "Please login first.";
            if (params.length < 1) return "Usage: observe <GAME_ID>";

            Integer gid = gameIdFromNumber(params[0]);
            if (gid == null) {
                return "Invalid selection. Run 'list' and use the game's number.";
            }

            if (currentGameId != null && currentGameId.equals(gid)) {
                return "Already observing this game.";
            }

            this.currentGameId = gid;

            if (ws == null) this.ws = new WebSocketFacade(baseURL, authToken, this);
            waitingForWs = true;
            ws.observeGame(gid);

            this.drawWhiteSide = true;
            synchronized (System.out) {
                DrawBoard.drawInitial(true);
            }

            String displayName = "#" + gid;
            for (var game : retrievedGames) {
                if (game.gameID() == gid) {
                    displayName = game.gameName();
                    break;
                }
            }

            return "Observing game \"" + displayName + "\"";
        } catch (Exception e) {
            return "Observe failed: " + e.getMessage();
        }
    }

    //NEEDS IMPLEMENTATION FOR GAMEPLAY
    public String move(String[] params) {
        return null;
    }

    public String leave() throws ResponseException {
        if (ws == null) return "No game connection.";
        ws.leaveGame(currentGameID());
        currentGameId = null;
        ws.close();
        return "Left the game.";
    }

    public void quit() {
        try {
            if (ws != null) {
                ws.close();
                ws = null;
            }
            authToken = null;
            currentGameId = null;
            System.out.println("Goodbye!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private int currentGameID() {
        return this.currentGameId;
    }

    private static String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }


    @Override
    public void notify(Notification notification) {
        synchronized (System.out) {
            System.out.println();
            System.out.println("[WebSocket message: " + notification.type() + "]: " + notification.message());
            System.out.printf("[%s] >>> ", ClientState.LOGGED_IN);
        }
        waitingForWs = false;
    }

}
