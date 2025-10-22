package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import io.javalin.http.Context;
import requests.GameRequest;
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
            assert token != null;
            checkToken(token, ctx);

            JoinGameRequest request = serializer.fromJson(ctx.body(), JoinGameRequest.class);
            if (request.gameID() <= 0 || request.playerColor().isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
            }

            gameService.joinGame(request, token);
            ctx.status(200).json(Map.of());


        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", "Error: internal server error"));
        } catch (UnauthorizedException e) {
            ctx.status(401).json(Map.of("message", "Unauthorized"));
        } catch (BadRequestException e) {
            ctx.status(400).json(Map.of("message", "Error: bad request"));
        }


    }

    public void listGames(Context ctx) {
        try {
            String token = ctx.header("authorization");
            assert token != null;
            checkToken(token, ctx);

            ListGameRequest request = new ListGameRequest(token);
            ListGamesResult result = gameService.listGames(request);
            ctx.status(200).json(result);

        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", "Error: internal server error"));
        } catch (UnauthorizedException e) {
            ctx.status(401).json(Map.of("message", "Unauthorized"));
        } catch (BadRequestException e) {
            ctx.status(400).json(Map.of("message", "Error: bad request"));
        }

    }

    public void createGame(Context ctx) {
        try {
            String token = ctx.header("authorization");
            assert token != null;
            checkToken(token, ctx);

            GameRequest request = serializer.fromJson(ctx.body(), GameRequest.class);
            CreateGameResult result = gameService.createGame(request, token);
            ctx.status(200).json(result);

        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", "Error: internal server error"));
        } catch (BadRequestException e) {
            ctx.status(400).json(Map.of("message", "Error: bad request"));
        } catch (UnauthorizedException e) {
            ctx.status(401).json(Map.of("message", "Unauthorized"));
        }
    }
    private void checkToken(String token, Context ctx) {
        if (token.isBlank()) {
            ctx.status(400).json(Map.of("message", "Missing authorization header"));
        }
    }
}
