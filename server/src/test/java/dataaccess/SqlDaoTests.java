package dataaccess;

import model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static dataaccess.DatabaseManager.getConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqlDaoTests {
    private static DatabaseManager db;
    private static UserDao userDao;

    @BeforeAll
    public static void createDatabase() throws DataAccessException, SQLException {
        db = new DatabaseManager();
        db.openConnection();

        try {
            DatabaseManager.createDatabase();
            DatabaseManager.initializeSchema();
        } catch (DataAccessException e) {
            throw new DataAccessException(e.getMessage());
        }
        userDao = new SqlUserDao(db);

        db.closeConnection(true);
    }

    @BeforeEach
    public void setup() throws DataAccessException {
        db.openConnection();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        db.closeConnection(false);
    }

    @Test
    void createUser_success() throws DataAccessException {
        String username = "u_" + UUID.randomUUID();
        String email = username + "@mail.com";
        String plain = "StrongP@ssw0rd!";
        String hash = BCrypt.hashpw(plain, BCrypt.gensalt());

        UserData user = new UserData(username, email, hash);
        userDao.createUser(user);

        UserData u = userDao.getUser(username);
        assertEquals(username, u.username(), "Username mismatch");
        assertEquals(email, u.email(), "Email mismatch");

        // Ensure stored value looks like a BCrypt hash, not plaintext
        assertTrue(BCrypt.checkpw(hash, u.password()), "Password hash should be a BCrypt hash");
    }
}
