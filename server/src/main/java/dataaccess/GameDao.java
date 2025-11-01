package dataaccess;

import model.GameData;
import java.util.List;

public interface GameDao {
    void clear() throws DataAccessException;

    int createGame(String gameName) throws DataAccessException;

    GameData getGame(int gameID) throws DataAccessException;

    List<GameData> listGames() throws DataAccessException;

    GameData updateGamePlayer(int gameID, String color, String username) throws DataAccessException;
}
