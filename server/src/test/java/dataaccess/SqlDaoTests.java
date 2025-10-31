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
    private Connection conn; // <-- not static; one per test

    @BeforeAll
    public static void createDatabase() throws Exception {
        db = new DatabaseManager();

        // one-off setup connection
        try (Connection setupConn = getConnection()) {
            setupConn.setAutoCommit(false);
            DatabaseManager.createDatabase();
            DatabaseManager.initializeSchema();
            db.closeConnection(setupConn, true);
        }
        userDao = new SqlUserDao(db);
    }

    @BeforeEach
    public void setup() throws DataAccessException, SQLException {
        conn = getConnection();
        conn.setAutoCommit(false);// <-- capture the connection used this test

    }

    @AfterEach
    public void tearDown() throws SQLException, DataAccessException {
        db.closeConnection(conn, false);     // <-- close the SAME connection
        conn = null;
        userDao.clear();
    }

    @Test
    void createUser_success() throws DataAccessException {
        String username = "u_" + UUID.randomUUID();
        String email = username + "@mail.com";
        String plain = "StrongP@ssw0rd!";
        String hash = BCrypt.hashpw(plain, BCrypt.gensalt());

        UserData user = new UserData(username, hash, email);
        userDao.createUser(user);

        UserData u = userDao.getUser(username);
        assertEquals(username, u.username(), "Username mismatch");
        assertEquals(email, u.email(), "Email mismatch");


        assertTrue(BCrypt.checkpw(plain, u.password()), "Password hash should verify with plaintext");

    }
}
