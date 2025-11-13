package ui;

import com.google.gson.Gson;
import exception.ResponseException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import client.ServerFacade;
import requests.*;
import results.*;

public class ChessClient extends Endpoint {
    private String authToken;
    private String baseURL;
    private final ServerFacade server;
    private final Gson gson = new Gson();


    public ChessClient(String serverUrl) {
        this.baseURL = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.server = new ServerFacade(this.baseURL);
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {

    }

    public void quit() {
    }

    public String login(String[] params) throws ResponseException {
        if (params.length != 2) {return "USAGE: login <username> <password>";}
        var request = new LoginRequest(params[0], params[1]);
        try {
            LoginResult result = server.login(request);
            this.authToken = result.authToken();
            return "Logged as " + params[0];
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

    public String observeGame(String[] params) {
        return null;
    }

    public String joinGame(String[] params) {
        return null;
    }

    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }

}
