package service;

import dataaccess.AuthDao;
import dataaccess.DataAccessException;
import dataaccess.GameDao;
import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import model.AuthData;
import model.GameData;
import requests.CreateGameRequest;
import requests.JoinGameRequest;
import requests.ListGameRequest;
import results.CreateGameResult;
import results.GameSummary;
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
            throw new BadRequestException("Error: null request");
        }
        if (request.gameID() <= 0 || request.playerColor() == null) {
            throw new BadRequestException("Error: gameID and playerColor can't be null");
        }

        AuthData authData = authDao.getAuth(authToken);
        if (authData == null) {
            throw new UnauthorizedException("Error: Unauthorized");
        }
        String username = authData.username();

        GameData game =  gameDao.getGame(request.gameID());
        if (game == null) {throw new BadRequestException("Error: Game not found");}

        String playerColor = normalizeColor(request.playerColor());
        if (playerColor == null) {
            throw new BadRequestException("Error: null playerColor");
        }

        try {
            if (playerColor.equals("WHITE")) {
                String game_user = game.whiteUsername();
                if (game_user == null) {
                    gameDao.updateGamePlayer(request.gameID(), playerColor, username);
                } else if (!game_user.equals(username)) {
                    throw new DataAccessException("Error: " + playerColor + " seat already taken by " + game_user);
                }
            } else if (playerColor.equals("BLACK")) {
                String game_user = game.blackUsername();
                if (game_user == null) {
                    gameDao.updateGamePlayer(request.gameID(), playerColor, username);
                }  else if (!game_user.equals(username)) {
                    throw new DataAccessException("Error: " + playerColor + " seat already taken by " + game_user);
                }
            }
        } catch (DataAccessException ex) {
            String m = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (m.contains("already taken") || m.contains("white already taken") || m.contains("black already taken")) {
                throw new AlreadyTakenException("Error: already taken");
            }
            if (m.contains("invalid color") || m.contains("not found")) {
                throw new BadRequestException("Error: bad request");
            }
            throw ex; // 500
        }

    }

    public CreateGameResult createGame(CreateGameRequest request, String authToken) throws DataAccessException, UnauthorizedException, BadRequestException {
        AuthData authData = authDao.getAuth(authToken);
        if (authData == null) {
            throw new UnauthorizedException("Error: Unauthorized");
        }

        if (request == null || request.gameName() == null || request.gameName().isBlank()) {
            throw new BadRequestException("Error: request is null");
        }

        int id = gameDao.createGame(request.gameName());

        return new CreateGameResult(id);
    }

    public ListGamesResult listGames(ListGameRequest request) throws DataAccessException {
        String token = request.authToken();

        AuthData authData = authDao.getAuth(token);
        if (authData == null || authData.authToken() == null || !authData.authToken().equals(token)) {
            throw new UnauthorizedException("Error: Invalid token");
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
