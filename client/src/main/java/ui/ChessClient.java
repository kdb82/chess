package ui;

import exception.ResponseException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import client.ServerFacade;


public class ChessClient extends Endpoint {

    public ChessClient(String serverUrl) {
        ServerFacade serverFacade = new ServerFacade(serverUrl);

    }

    public void run() {

    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {

    }

    public void quit() {
    }

    public String login(String[] params) throws ResponseException {

        return null;
    }

    public String register(String[] params) {
        return null;
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
