package dataaccess;

import model.AuthData;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import static dataaccess.DatabaseManager.getConnection;

public class SqlAuthDao implements AuthDao {
    private final DatabaseManager databaseManager;

    public SqlAuthDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void clear() {

    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String token = auth.authToken();
        final int userId;
        try {
            userId = findUserIdByUsername(auth.username()); // throws if not found
        } catch (SQLException e) {
            throw new DataAccessException("Error resolving user by username", e);
        }


        final String sql = "INSERT INTO tokens (token, user_id) VALUES (?, ?)";
        try (var conn = getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DataAccessException("Error: duplicate token or invalid user_id", dup);
        } catch (SQLException e) {
            throw new DataAccessException("Error creating token", e);
        }
    }

    @Override
    public AuthData getAuth(String token) throws DataAccessException {
        return null;
    }

    @Override
    public void deleteAuth(AuthData authData) {

    }

    private int findUserIdByUsername(String username) throws SQLException {
        String query = "SELECT id FROM users WHERE username = ?";
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