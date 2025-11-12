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

    public void login(String[] params) throws ResponseException {
        if (params.length >= 1) {
            ws.enterPetShop(visitorName);
            return String.format("You signed in as %s.", visitorName);
        }
        throw new ResponseException(ResponseException.Code.ClientError, "Expected: <yourname>");
    }

}

    public void register(String[] params) {
    }

    public void createGame(String[] params) {
    }

    public void observeGame(String[] params) {
    }

    public void joinGame(String[] params) {
    }

    public void logout() {
    }

    public void listGames() {
    }
}
