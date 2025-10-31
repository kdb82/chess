package dataaccess;

import exceptions.AlreadyTakenException;
import model.UserData;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static dataaccess.DatabaseManager.getConnection;
import static org.junit.jupiter.api.Assertions.*;

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
    @Test
    @DisplayName("2) createUser: duplicate username throws DataAccessException")
    void createUser_duplicateUsername_throws() throws DataAccessException {
        String username = "dupe_" + UUID.randomUUID();
        String email1 = username + "@mail.com";
        String email2 = username + "+2@mail.com";
        String hash = BCrypt.hashpw("pw", BCrypt.gensalt());

        userDao.createUser(new UserData(username, hash, email1));
        // Same username again should violate unique constraint → DataAccessException
        assertThrows(AlreadyTakenException.class,
                () -> userDao.createUser(new UserData(username, hash, email2)),
                "Expected AlreadyTakenException for duplicate username");
    }

    // ---------- getUser ----------

    @Test
    @DisplayName("3) getUser: success returns the user row")
    void getUser_success() throws DataAccessException {
        String username = "get_" + UUID.randomUUID();
        String email = username + "@mail.com";
        String plain = "S3cret!";
        String hash = BCrypt.hashpw(plain, BCrypt.gensalt());

        userDao.createUser(new UserData(username, hash, email));

        UserData out = userDao.getUser(username);
        assertEquals(username, out.username());
        assertEquals(email, out.email());
        assertTrue(BCrypt.checkpw(plain, out.password()));
    }

    @Test
    @DisplayName("4) getUser: unknown username throws DataAccessException")
    void getUser_notFound_throws() {
        String missing = "missing_" + UUID.randomUUID();
        assertThrows(DataAccessException.class,
                () -> userDao.getUser(missing),
                "Expected DataAccessException for unknown username");
    }

    // ---------- clear ----------

    @Test
    @DisplayName("5) clear: removes all users")
    void clear_removesAllUsers() throws DataAccessException {
        String u1 = "c1_" + UUID.randomUUID();
        String u2 = "c2_" + UUID.randomUUID();
        String hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        String hash2 = BCrypt.hashpw("pw2", BCrypt.gensalt());


        userDao.createUser(new UserData(u1, hash, u1 + "@mail.com"));
        userDao.createUser(new UserData(u2, hash2, u2 + "@mail.com"));

        // Act
        userDao.clear();

        // Assert: both should now be gone
        assertThrows(DataAccessException.class, () -> userDao.getUser(u1));
        assertThrows(DataAccessException.class, () -> userDao.getUser(u2));
    }

    @Test
    @DisplayName("6) clear: idempotent (calling twice is safe)")
    void clear_idempotent() throws DataAccessException {
        userDao.clear(); // first call on empty table
        // second call should not throw
        assertDoesNotThrow(() -> userDao.clear());
        // and you can still insert afterwards
        String username = "postclear_" + UUID.randomUUID();
        String hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        userDao.createUser(new UserData(username, hash, username + "@mail.com"));
        assertEquals(username, userDao.getUser(username).username());
    }
}
