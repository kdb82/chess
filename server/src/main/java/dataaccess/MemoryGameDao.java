package dataaccess;

import chess.ChessGame;
import model.GameData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryGameDao implements GameDao {
    private final Map<Integer, GameData> games = new HashMap<>();
    private final AtomicInteger nextID = new AtomicInteger(1);

    @Override
    public void clear() {
        games.clear();
        nextID.set(1);
    }

    @Override
    public void createGame(String gameName) throws DataAccessException {
        if (gameName.isBlank()) {
            throw new DataAccessException("gamename required");
        }
        int id = nextID.getAndIncrement();
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(id, null, null, gameName, game);
        games.put(id, gameData);
    }

    @Override
    public GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public List<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    public GameData updateGamePlayer(int gameID, String playerColor, String username) throws DataAccessException {
        GameData game = games.get(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found");
        }

        if (playerColor == null || (!playerColor.equalsIgnoreCase("WHITE") && !playerColor.equalsIgnoreCase("BLACK"))) {
            throw new DataAccessException("Invalid color");
        }

        if (playerColor.equalsIgnoreCase("WHITE")) {
            if (game.whiteUsername() != null) {
                throw new DataAccessException("White already taken");
            }

            GameData updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
            games.put(gameID, updated);
            return updated;

        } else {
            if (game.blackUsername() != null) {
                throw new DataAccessException("Black already taken");
            }
            GameData updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
            games.put(gameID, updated);
            return updated;
        }
    }

}
