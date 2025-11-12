package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import io.javalin.http.Context;
import requests.CreateGameRequest;
import requests.JoinGameRequest;
import requests.ListGameRequest;
import results.CreateGameResult;
import results.ListGamesResult;
import service.GameService;

import java.util.Map;

public class GameHandler{
    private final Gson serializer;
    private final GameService gameService;


    public GameHandler(Gson gson, GameService gameService) {
        this.serializer = gson;
        this.gameService = gameService;
    }

    public void joinGame(Context ctx) {
        try {
            String token = ctx.header("authorization");
            if (token == null || token.isBlank()) {
                ctx.status(400).json(Map.of("message", "Missing authorization header"));
                return;
            }
            String body = ctx.body();
            if (body.isBlank()) {
                ctx.status(400).json(Map.of("message", "Missing body header"));
                return;
            }

            JoinGameRequest request = serializer.fromJson(body, JoinGameRequest.class);
            if (request.playerColor() == null || request.gameID() <= 0 || request.playerColor().isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            gameService.joinGame(request, token);
            ctx.status(200).json(Map.of());


        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", e.getMessage()));
        } catch (UnauthorizedException e) {
            ctx.status(401).json(Map.of("message", "Error: unauthorized"));
        } catch (BadRequestException e) {
            ctx.status(400).json(Map.of("message", e.getMessage()));
        } catch (AlreadyTakenException e) {
            ctx.status(403).json(Map.of("message", "Error: already taken"));
        }


    }

    public void listGames(Context ctx) {
        try {
            String token = ctx.header("authorization");
            if (token == null || token.isBlank()) {
                ctx.status(400).json(Map.of("message", "Missing authorization header"));
                return;
            }

            ListGameRequest request = new ListGameRequest(token);
            ListGamesResult result = gameService.listGames(request);
            ctx.status(200).json(result);

        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", "Error: could not list games"));
        } catch (UnauthorizedException e) {
            ctx.status(401).json(Map.of("message", "Error: unauthorized"));
        } catch (BadRequestException e) {
            ctx.status(400).json(Map.of("message", "Error: bad request"));
        }

    }

    public void createGame(Context ctx) {
        try {
            String token = ctx.header("authorization");
            if (token == null || token.isBlank()) {
                ctx.status(400).json(Map.of("message", "Missing authorization header"));
                return;
            }

            CreateGameRequest request = serializer.fromJson(ctx.body(), CreateGameRequest.class);
            CreateGameResult result = gameService.createGame(request, token);
            ctx.status(200).json(result);

        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", "Error: could not create game"));
        } catch (BadRequestException e) {
            ctx.status(400).json(Map.of("message", "Error: bad request"));
        } catch (UnauthorizedException e) {
            ctx.status(401).json(Map.of("message", "Error: unauthorized"));
        }
    }

}
