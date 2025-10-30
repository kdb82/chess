package dataaccess;

import model.GameData;

import java.util.List;

public class SqlGameDao implements GameDao {
    private final DatabaseManager databaseManager;

    public SqlGameDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void clear() {

    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        return 0;
    }

    @Override
    public GameData getGame(int gameID) {
        return null;
    }

    @Override
    public List<GameData> listGames() {
        return List.of();
    }

    @Override
    public GameData updateGamePlayer(int gameID, String color, String username) throws DataAccessException {
        return null;
    }
}
