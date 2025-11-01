package dataaccess;

import model.GameData;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static dataaccess.DatabaseManager.getConnection;

public class SqlGameDao implements GameDao {

    @Override
    public void clear() {
        try (var conn = getConnection(); var stmnt = conn.createStatement()) {
            stmnt.executeUpdate("DELETE FROM games");
            stmnt.executeUpdate("DELETE FROM game_players");
            stmnt.executeUpdate("DELETE FROM game_moves");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        final String insertGame = """
                INSERT INTO games (creator_id, game_name, turn_color, black_king_location, white_king_location, status, result)
                VALUES (NULL, ?, 'WHITE',' e8', 'e1', 'OPEN', 'UNDECIDED')
                """;
        try (var conn = getConnection(); var stmt = conn.prepareStatement(insertGame, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, gameName);
            stmt.executeUpdate();
            if (stmt.getGeneratedKeys().next()) {
                return stmt.getGeneratedKeys().getInt(1);
            } else {
                throw new DataAccessException("Error: Couldn't Create Game");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public GameData getGame(int gameID) {
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
                    GameData out = new GameData(
                            rs.getInt("gameId"),
                            rs.getString("white_username"),
                            rs.getString("black_username"),
                            rs.getString("game_name"),
                            null
                    );
                    return out;

                } else return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error: couldn't get game",e);
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
        if (color == null ||
                (!color.equalsIgnoreCase("WHITE") && !color.equalsIgnoreCase("BLACK"))) {
            throw new DataAccessException("Invalid color (use WHITE or BLACK)");
        }
        String colorToUpdate = color.toUpperCase();

        if (!gameExists(gameID)) throw new DataAccessException("Error: game not found");

        final int userId;
        try {
            userId = findUserIdByUsername(username);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        final String insertSeat = """
            INSERT INTO game_players (game_id, user_id, color)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE user_id = IF(user_id IS NULL, VALUES(user_id), user_id)
        """;

        try (var conn = getConnection()) {
            conn.setAutoCommit(false);
            try (var ps = conn.prepareStatement(insertSeat)) {
                ps.setInt(1, gameID);
                ps.setInt(2, userId);
                ps.setString(3, colorToUpdate);
                ps.executeUpdate();
            }


            try (var creatorStmt = conn.prepareStatement(
                    "UPDATE games SET creator_id = ? WHERE id = ? AND creator_id IS NULL")) {
                creatorStmt.setInt(1, userId);
                creatorStmt.setInt(2, gameID);
                creatorStmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new DataAccessException("Error joining player to game", e);
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
            throw new DataAccessException("Error checking game existence", e);
        }
    }


    private int findUserIdByUsername(String username) throws SQLException {
        final String query = "SELECT id FROM users WHERE username = ?";
        try (var conn = getConnection();
             var ps = conn.prepareStatement(query)) {

            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new RuntimeException("User not found: " + username);
                }
            }
        }
    }

}
