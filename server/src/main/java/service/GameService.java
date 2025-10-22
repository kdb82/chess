package service;

import dataaccess.AuthDao;
import dataaccess.DataAccessException;
import dataaccess.GameDao;
import dataaccess.UserDao;
import exceptions.UnauthorizedException;
import model.AuthData;
import model.GameData;
import requests.ListRequest;
import results.CreateGameResult;
import results.GameSummary;
import results.JoinGameResult;
import results.ListGamesResult;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class GameService {
    private final GameDao gameDao;
    private final AuthDao authDao;

    public GameService(GameDao gameDao, UserDao userDao, AuthDao authDao) {
        this.gameDao = gameDao;
        this.authDao = authDao;
    }

    public JoinGameResult joinGame(int gameID, String username) throws DataAccessException {
        return null;
    }

    public CreateGameResult createGame(int gameID, String username) throws DataAccessException {
        return null;
    }

    public ListGamesResult listGames(ListRequest request) throws DataAccessException {
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
}
