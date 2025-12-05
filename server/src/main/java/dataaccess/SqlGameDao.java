package dataaccess;

import chess.ChessGame;
import model.GameData;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static dataaccess.DatabaseManager.getConnection;

public class SqlGameDao implements GameDao {
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    @Override
    public void clear() throws DataAccessException {
        try (var conn = getConnection(); var stmnt = conn.createStatement()) {
            stmnt.executeUpdate("DELETE FROM game_moves");
            stmnt.executeUpdate("DELETE FROM game_players");
            stmnt.executeUpdate("DELETE FROM games");
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage(), e);
        }
    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        final String insertGame = """
                INSERT INTO games (creator_id, game_name, turn_color, black_king_location, white_king_location, status, result, state_json)
                VALUES (NULL, ?, 'WHITE', 'e8', 'e1', 'OPEN', 'UNDECIDED', ?)
                """;
        try (var conn = getConnection(); var stmt = conn.prepareStatement(insertGame, Statement.RETURN_GENERATED_KEYS)) {
            var initial_game = new ChessGame();
            var json = GSON.toJson(serialization.GameStateMapper.gameToDTO(initial_game));

            stmt.setString(1, gameName);
            stmt.setString(2, json);
            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DataAccessException("Error: Couldn't create game (no ID returned)");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }

    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        final String query = """
                SELECT g.id AS gameId, game_name, u.username AS white_username, u2.username AS black_username FROM games g
                LEFT JOIN game_players gp ON g.id = gp.game_id AND gp.color = 'WHITE'
                LEFT JOIN users u ON u.id = gp.user_id
                LEFT JOIN game_players gpb ON g.id = gpb.game_id AND gpb.color = 'BLACK'
                LEFT JOIN users u2 ON u2.id = gpb.user_id
                WHERE g.id = ?
                LIMIT 1
                """;
        try (var conn = getConnection(); var stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    var game = loadGameState(gameID);
                    GameData out = new GameData(
                            rs.getInt("gameId"),
                            rs.getString("white_username"),
                            rs.getString("black_username"),
                            rs.getString("game_name"),
                            game
                    );
                    return out;

                } else return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: couldn't get game",e);
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        final String sql = """
            SELECT g.id AS gameID,
                   g.game_name,
                   u.username AS whiteUsername,
                   u2.username AS blackUsername
            FROM games g
            LEFT JOIN game_players gpw ON gpw.game_id = g.id AND gpw.color = 'WHITE'
            LEFT JOIN users u         ON u.id       = gpw.user_id
            LEFT JOIN game_players gpb ON gpb.game_id = g.id AND gpb.color = 'BLACK'
            LEFT JOIN users u2         ON u2.id       = gpb.user_id
            ORDER BY g.id DESC
        """;
        var out = new ArrayList<GameData>();
        try (var conn = getConnection(); var stmt = conn.prepareStatement(sql); var rs = stmt.executeQuery()) {
            while (rs.next()) {
                out.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("game_name"),
                        null
                ));
            }
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("Error: listing games failed", e);
        }
    }

    @Override
    public GameData updateGamePlayer(int gameID, String color, String username) throws DataAccessException {
        if (color == null || (!color.equalsIgnoreCase("WHITE") && !color.equalsIgnoreCase("BLACK"))) {
            throw new DataAccessException("Error: Invalid color (use WHITE or BLACK)");
        }
        String seat = color.toUpperCase();

        if (!gameExists(gameID)) {
            throw new DataAccessException("Error: game not found");
        }

        int userId;
        try {
            userId = findUserIdByUsername(username);
        } catch (SQLException e) {
            throw new DataAccessException("Error finding user", e);
        }

        final String checkSeat = """
            SELECT u.username
            FROM game_players gp
            LEFT JOIN users u ON u.id = gp.user_id
            WHERE gp.game_id = ? AND gp.color = ?
            """;

        final String insertSeat = """
            INSERT INTO game_players (game_id, user_id, color)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)
            """;

        try (var conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String occupant = null;
                try (var ps = conn.prepareStatement(checkSeat)) {
                    ps.setInt(1, gameID);
                    ps.setString(2, seat);
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            occupant = rs.getString("username");
                        }
                    }
                }

                if (occupant != null && !occupant.isEmpty()) {
                    throw new DataAccessException("Error: " + seat + " seat already taken by " + occupant);
                }

                try (var ps = conn.prepareStatement(insertSeat)) {
                    ps.setInt(1, gameID);
                    ps.setInt(2, userId);
                    ps.setString(3, seat);
                    ps.executeUpdate();
                }

                try (var ps = conn.prepareStatement(
                        "UPDATE games SET creator_id = ? WHERE id = ? AND creator_id IS NULL")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, gameID);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new DataAccessException(e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: couldn't join player to game", e);
        }

        return getGame(gameID);
    }

    private boolean gameExists(int gameId) throws DataAccessException {
        try (var conn = getConnection();
             var ps = conn.prepareStatement("SELECT 1 FROM games WHERE id = ?")) {
            ps.setInt(1, gameId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: couldn't check game existence", e);
        }
    }


    private int findUserIdByUsername(String username) throws SQLException, DataAccessException {
        final String query = "SELECT id FROM users WHERE username = ?";
        try (var conn = getConnection();
             var ps = conn.prepareStatement(query)) {

            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new DataAccessException("Error: User not found: " + username);
                }
            }
        }
    }

    //saves a json string representing game to database given a game object
    @Override
    public void saveGameState(int gameId, chess.ChessGame game) throws DataAccessException {
        var json = GSON.toJson(serialization.GameStateMapper.gameToDTO(game));
        final String sql = "UPDATE games SET state_json = ? WHERE id = ?";
        try (var conn = getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setString(1, json);
            ps.setInt(2, gameId);
            if (ps.executeUpdate() == 0) throw new DataAccessException("Error: game not found");
        } catch (java.sql.SQLException e) {
            throw new DataAccessException("Error: couldn't save game state", e);
        }
    }

    //returns game loaded from database information
    @Override
    public chess.ChessGame loadGameState(int gameId) throws DataAccessException {
        final String sql = "SELECT state_json FROM games WHERE id = ?";
        try (var conn = getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new DataAccessException("Error: game not found");
                var json = rs.getString(1);
                if (json == null || json.isBlank()) return new chess.ChessGame();
                var dto = GSON.fromJson(json, serialization.GameStateDTO.class);
                return serialization.GameStateMapper.dtoToGame(dto);
            }
        } catch (java.sql.SQLException e) {
            throw new DataAccessException("Error loading game state", e);
        }
    }
    @Override
    public void updateGameStatus(int gameId, String status, String result) throws DataAccessException {
        final String sql = "UPDATE games SET status = ?, result = ? WHERE id = ?";
        try (var conn = getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, result);
            ps.setInt(3, gameId);

            if (ps.executeUpdate() == 0) {
                throw new DataAccessException("Error: game not found");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: could not update status", e);
        }
    }
    @Override
    public void removePlayerSeat(int gameId, String color) throws DataAccessException {
        final String sql = "DELETE FROM game_players WHERE game_id = ? AND color = ?";
        try (var conn = getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.setString(2, color.toUpperCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error: couldn't remove player seat", e);
        }
    }


}
