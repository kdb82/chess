package dataaccess;

import model.GameData;

public interface GameDao {

    void clear();

    void createGame(String gameName);

    void listGames(String authToken);

    GameData getGame(GameData gameData);

    GameData updateGamePlayer(String gameID, String playerColor, String userName);
}
