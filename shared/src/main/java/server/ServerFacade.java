package server;

import com.google.gson.Gson;
import exception.ResponseException;
import requests.*;
import results.*;

import java.net.URI;
import java.net.http.*;

public class ServerFacade {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String serverUrl;

    public ServerFacade(String url) {
        serverUrl = url;
    }

    public RegisterResult register(RegisterRequest registerRequest) throws ResponseException {
        assert(registerRequest != null);
        var request = buildRequest("POST", "/user", registerRequest);
        var response = sendRequest(request);
        return handleResponse(response,  RegisterResult.class);
    }

    public LoginResult login(LoginRequest loginRequest) throws ResponseException {
        assert(loginRequest != null);
        var request = buildRequest("POST", "/session", loginRequest);
        var response = sendRequest(request);
        return handleResponse(response,  LoginResult.class);
    }

    public LogoutResult logout(LogoutRequest logoutRequest) throws ResponseException {
        assert(logoutRequest != null);
        var request = buildRequest("POST", "/session", logoutRequest);
        var response = sendRequest(request);
        return handleResponse(response,  LogoutResult.class);
    }

    public CreateGameResult createGame(CreateGameRequest createGameRequest) throws ResponseException {
        assert(createGameRequest != null);
        var request = buildRequest("POST", "/session", createGameRequest);
        var response = sendRequest(request);
        return handleResponse(response,  CreateGameResult.class);
    }

    public JoinGameResult joinGame(JoinGameRequest joinGameRequest) throws ResponseException {
        assert(joinGameRequest != null);
        var request = buildRequest("POST", "/session", joinGameRequest);
        var response = sendRequest(request);
        return handleResponse(response,  JoinGameResult.class);
    }

    public ListGamesResult listGames(ListGameRequest listGameRequest) throws ResponseException {
        assert(listGameRequest != null);
        var request = buildRequest("POST", "/session", listGameRequest);
        var response = sendRequest(request);
        return handleResponse(response,  ListGamesResult.class);
    }

    public void clear() throws ResponseException {
        var request = buildRequest("DELETE", "/db", null);
        sendRequest(request);
    }

    private HttpRequest buildRequest(String method, String path, Object body) {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .method(method, makeRequestBody(body));
        if (body != null) {
            request.setHeader("Content-Type", "application/json");
        }
        return request.build();
    }

    private HttpRequest.BodyPublisher makeRequestBody(Object request) {
        if (request != null) {
            return HttpRequest.BodyPublishers.ofString(new Gson().toJson(request));
        } else {
            return HttpRequest.BodyPublishers.noBody();
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) throws ResponseException {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new ResponseException(ResponseException.Code.ServerError, ex.getMessage());
        }
    }

    private <T> T handleResponse(HttpResponse<String> response, Class<T> responseClass) throws ResponseException {
        var status = response.statusCode();
        if (!isSuccessful(status)) {
            var body = response.body();
            if (body != null) {
                throw ResponseException.fromJson(body);
            }

            throw new ResponseException(ResponseException.fromHttpStatusCode(status), "other failure: " + status);
        }

        if (responseClass != null) {
            return new Gson().fromJson(response.body(), responseClass);
        }

        return null;
    }

    private boolean isSuccessful(int status) {
        return status / 100 == 2;
    }

}