package service;

import dataaccess.AuthDao;
import dataaccess.DataAccessException;
import dataaccess.GameDao;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import model.AuthData;
import model.GameData;
import requests.GameRequest;
import requests.JoinGameRequest;
import requests.ListGameRequest;
import results.CreateGameResult;
import results.GameSummary;
import results.JoinGameResult;
import results.ListGamesResult;

import java.util.ArrayList;
import java.util.List;

public class GameService {
    private final GameDao gameDao;
    private final AuthDao authDao;

    public GameService(GameDao gameDao, AuthDao authDao) {
        this.gameDao = gameDao;
        this.authDao = authDao;
    }

    public void joinGame(JoinGameRequest request, String authToken) throws DataAccessException,BadRequestException, UnauthorizedException {
        if (request == null) {
            throw new BadRequestException("request is null");
        }
        if (request.gameID() <= 0 || request.playerColor() == null) {
            throw new BadRequestException("gameID and playerColor can't be null");
        }

        AuthData authData = authDao.getAuth(authToken);
        if (authData == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        String username = authData.username();

        GameData game =  gameDao.getGame(request.gameID());
        if (game == null) {throw new BadRequestException("Game not found");}

        String playerColor = normalizeColor(request.playerColor());
        if (playerColor == null) {
            throw new BadRequestException("playerColor can't be null");
        }

        try {
            gameDao.updateGamePlayer(request.gameID(), playerColor, username);
        } catch (DataAccessException ex) {
            String m = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (m.contains("already taken") || m.contains("white already taken") || m.contains("black already taken")) {
                throw new exceptions.AlreadyTakenException("already taken");
            }
            if (m.contains("invalid color") || m.contains("not found")) {
                throw new BadRequestException("bad request");
            }
            throw ex; // 500
        }

    }

    public CreateGameResult createGame(GameRequest request, String authToken) throws DataAccessException, UnauthorizedException, BadRequestException {
        AuthData authData = authDao.getAuth(authToken);
        if (authData == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        if (request == null || request.gameName() == null || request.gameName().isBlank()) {
            throw new BadRequestException("request is null");
        }

        int id = gameDao.createGame(request.gameName());

        return new CreateGameResult(id);
    }

    public ListGamesResult listGames(ListGameRequest request) throws DataAccessException {
        String token = request.authToken();

        AuthData authData = authDao.getAuth(token);
        if (authData == null || authData.authToken() == null || !authData.authToken().equals(token)) {
            throw new UnauthorizedException("Invalid token");
        }

        var games = gameDao.listGames();
        List<GameSummary> gamesList = new ArrayList<>(games.size());
        for (GameData game : games) {
            gamesList.add(new GameSummary(game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName()));
        }

        return new ListGamesResult(gamesList);
    }

    private static String normalizeColor(String c) {
        if (c == null) return null;
        String t = c.trim().toUpperCase();
        return ("WHITE".equals(t) || "BLACK".equals(t)) ? t : null;
    }
}
