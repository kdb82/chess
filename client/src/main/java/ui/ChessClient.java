package ui;

import com.google.gson.Gson;
import exception.ResponseException;
import jakarta.websocket.*;
import client.ServerFacade;
import requests.*;
import results.*;
import webSocketMessages.Notification;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ChessClient extends Endpoint {
    private String authToken;
    private String baseURL;
    private final ServerFacade server;
    private final Gson gson = new Gson();
    private Session ws;
    private Integer joinGameId;
    private String joinGameColor;

    public ChessClient(String serverUrl) {
        this.baseURL = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.server = new ServerFacade(this.baseURL);
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
//        this.ws = session;
//        this.ws.addMessageHandler(String.class, message -> {
//            var map = gson.fromJson(message, Map.class);
//            String type = (String) map.get("type");
//
//            switch (type) {
//                case "PLAYER_JOINED" -> {
//                    String username = (String) map.get("username");
//                    String color = (String) map.get("color");
//                    int gameID = ((Double) map.get("gameID")).intValue();
//                    System.out.printf("[WS] %s joined game %d as %s%n", username, gameID, color);
//                }
//                case "NOTIFICATION" -> {
//                    System.out.println("[WS Notice] " + map.get("message"));
//                }
//                case "GAME_STATE" -> {
//                    System.out.println("[WS] Updated board:");
//                    System.out.println(map.get("board"));
//                }
//                default -> {
//                    System.out.println("[WS Unknown] " + message);
//                }
//            }
//        });
//
//        System.out.println("[WS connected]");
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

            int gameID = Integer.parseInt(params[0]);
            String color = (params.length >= 2) ? params[1].toUpperCase() : null;

            var req = new JoinGameRequest(gameID, color);
            JoinGameResult res = server.joinGame(req, authToken);

            this.joinGameId = gameID;  //POSSIBLE EDIT
            this.joinGameColor = color;

            String wsURL = baseURL.replaceFirst("^http", "ws") + "/ws?authToken=" + authToken;
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.ws = container.connectToServer(this, URI.create(wsURL));

            return "Joined game " + gameID + (color != null ? " as " + color : "");
        } catch (Exception e) {
            return "Join failed: " + e.getMessage();
        }
    }

    public String observeGame(String[] params) {
        return null;
    }
    public void quit() {
    }

    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }



}
