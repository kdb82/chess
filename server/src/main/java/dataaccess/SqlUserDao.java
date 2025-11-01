package dataaccess;

import exceptions.AlreadyTakenException;
import model.UserData;

import java.sql.SQLException;

import static dataaccess.DatabaseManager.getConnection;

public class SqlUserDao implements UserDao {

    public SqlUserDao() {
    }

    @Override
    public void clear() throws DataAccessException {
        try (var conn = getConnection(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM users");
        } catch (SQLException e) {
            throw new DataAccessException("Error: Database error clearing users", e);
        }
    }


    @Override
    public void createUser(UserData user) throws DataAccessException {
        String userName = user.username();
        String password = user.password();
        String email = user.email();
        final String query = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (var conn = getConnection(); var stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userName);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new AlreadyTakenException("Error: username or password already exists");
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        final String sql = "SELECT username, password, email FROM users WHERE username = ? LIMIT 1";

        try (var conn = getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (var rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new DataAccessException("Error: username not found");
                }

                return new UserData(
                        rs.getString("username"),
                        rs.getString("password"), // stored BCrypt hash
                        rs.getString("email")
                );
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage(), e);
        }
    }

}

