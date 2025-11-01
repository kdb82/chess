package dataaccess;

import model.AuthData;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import static dataaccess.DatabaseManager.getConnection;

public class SqlAuthDao implements AuthDao {


    @Override
    public void clear() throws DataAccessException {
        try (var conn = getConnection(); var st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM tokens");
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing tokens", e);
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String token = auth.authToken();
        final int userId;
        try {
            userId = findUserIdByUsername(auth.username());
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

    public AuthData getAuth(String token) throws DataAccessException {
        final String sql = """
        SELECT t.token, u.username
        FROM tokens t
        JOIN users u ON u.id = t.user_id
        WHERE t.token = ?
        LIMIT 1
    """;
        try (var con = getConnection();
             var ps  = con.prepareStatement(sql)) {

            ps.setString(1, token);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new DataAccessException("Error: token not found");
                }
                return new AuthData(rs.getString("token"), rs.getString("username"));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error reading auth token", e);
        }
    }


    @Override
    public void deleteAuth(AuthData authData) throws DataAccessException {
        String token = authData.authToken();
        final String sql = "DELETE FROM tokens WHERE token = ?";
        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            int rows = stmt.executeUpdate();
            if (rows == 0) throw new DataAccessException("Token not found");

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting auth token", e);
        } catch (DataAccessException e) {
            throw new DataAccessException(e.getMessage(), e);
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