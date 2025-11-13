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

    public String createGame(String[] params) {
        return null;
    }

    public String observeGame(String[] params) {
        return null;
    }

    public String joinGame(String[] params) {
        return null;
    }

    public String logout() {
        return null;
    }

    public String listGames() {
        return null;
    }
}
