package dataaccess;

import model.GameData;
import java.util.List;

public interface GameDao {
    void clear();

    int createGame(String gameName) throws DataAccessException;

    GameData getGame(int gameID);

    List<GameData> listGames();

    GameData updateGamePlayer(int gameID, String color, String username) throws DataAccessException;
}
