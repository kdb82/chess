package ui;

import com.google.gson.Gson;
import exception.ResponseException;
import jakarta.websocket.*;
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

    private String authToken;
    private final Gson gson = new Gson();


    public ChessClient(String serverUrl) {
        this.baseURL = serverUrl;
        this.server = new ServerFacade(this.baseURL);
    }


    public String login(String[] params) throws ResponseException {
        if (params.length != 2) {return "USAGE: login <username> <password>";}
        var request = new LoginRequest(params[0], params[1]);
        try {
            LoginResult result = server.login(request);
            this.authToken = result.authToken();
            return "Logged in as " + params[0];
        } catch (ResponseException e) {
            return "Login failed: " + e.getMessage();
        }
    }

    public String register(String[] params) {
        if (params.length != 3) return "USAGE:  register <username> <password> <email>";
        var request = new RegisterRequest(params[0], params[1], params[2]);
        try {
            RegisterResult result = server.register(request);
            this.authToken = result.authToken();
            return "Registered and logged in as " + params[0];
        } catch (ResponseException e) {
            return "Register failed: " + e.getMessage();
        }
    }

    public String logout() {
        try {
            if (authToken == null) return "Not logged in.";
            var req = new LogoutRequest(authToken);
            server.logout(req);
            this.authToken = null;
            return "Logged out.";
        } catch (ResponseException e) {
            return "Logout failed: " + e.getMessage();
        }
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
            // Table of games
            var sb = new StringBuilder("Games:\n");
            var games = res.games();
            if (games == null || games.isEmpty()) return "No games found.";
            for (var g : games) {
                sb.append(String.format("  %-4d  %-20s  W:%s  B:%s%n",
                        g.gameID(), g.gameName(), nullToDash(g.whiteUsername()), nullToDash(g.blackUsername())));
            }
            return sb.toString();
        } catch (ResponseException e) {
            return "List failed: " + e.getMessage();
        }
    }

    public String joinGame(String[] params) {
        try {
            if (authToken == null) return "Please login first.";
            if (params.length < 1) return "Usage: join <GAME_ID> [WHITE|BLACK]";

            var gameId = Integer.parseInt(params[0]);
            this.currentGameId = gameId;
            String color = (params.length >= 2) ? params[1].toUpperCase() : null;

            var req = new JoinGameRequest(gameId, color);
            server.joinGame(req, authToken);

            this.ws = new WebSocketFacade(baseURL, authToken, this);
            ws.joinGame(gameId, color);
            return "Joined game " + gameId + (color != null ? " as " + color : "");
        } catch (Exception e) {
            return "Join failed: " + e.getMessage();
        }
    }

    public String observeGame(String[] params) {
        try {
            if (authToken == null) return "Please login first.";
            if (params.length < 1) return "Usage: observe <GAME_ID>";
            int gameID = Integer.parseInt(params[0]);
            this.ws = new WebSocketFacade(baseURL, authToken, this);
            ws.observeGame(gameID);
            return "Observing game " + gameID;
        } catch (Exception e) {
            return "Observe failed: " + e.getMessage();
        }
    }

    //NEEDS IMPLEMENTATION FOR GAMEPLAY
    public String move(String[] p) {
        return null;
    }

    public String leave(String[] p) {
        try {
            if (ws == null) return "No game connection.";
            ws.leaveGame(currentGameID());
            currentGameId = null;
            ws.close();
            return "Left the game.";
        } catch (Exception e) {
            return "Leave failed: " + e.getMessage();
        }
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

    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }


    @Override
    public void notify(Notification notification) {
        System.out.printf("[%s]: %s%n", notification.type(), notification);
    }
}
