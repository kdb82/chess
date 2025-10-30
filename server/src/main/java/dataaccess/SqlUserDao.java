package dataaccess;

import exceptions.AlreadyTakenException;
import model.UserData;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import static dataaccess.DatabaseManager.getConnection;

public class SqlUserDao implements UserDao {
    private final DatabaseManager databaseManager;

    public SqlUserDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void clear() {

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
            throw new DataAccessException("username or password already exists", e);
        }
    }

    @Override
    public UserData getUser(String username) throws AlreadyTakenException {
        return null;
    }
}
