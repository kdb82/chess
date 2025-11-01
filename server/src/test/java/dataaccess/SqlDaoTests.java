package dataaccess;

import exceptions.AlreadyTakenException;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static dataaccess.DatabaseManager.getConnection;
import static org.junit.jupiter.api.Assertions.*;

public class SqlDaoTests {
    private static DatabaseManager db;
    private static UserDao userDao;
    private static AuthDao authDao;
    private static GameDao gameDao;
    private Connection conn; // <-- not static; one per test

    @BeforeAll
    public static void createDatabase() throws Exception {
        db = new DatabaseManager();


        try (Connection setupConn = getConnection()) {
            setupConn.setAutoCommit(false);
            DatabaseManager.createDatabase();
            DatabaseManager.initializeSchema();
            db.closeConnection(setupConn, true);
        }
        userDao = new SqlUserDao();
        authDao = new SqlAuthDao();
        gameDao = new SqlGameDao();
    }

    @BeforeEach
    public void setup() throws DataAccessException, SQLException {
        conn = getConnection();
        conn.setAutoCommit(false);

    }

    @AfterEach
    public void tearDown() throws SQLException, DataAccessException {
        db.closeConnection(conn, false);
        conn = null;
        authDao.clear();
        gameDao.clear();
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


        userDao.clear();


        assertThrows(DataAccessException.class, () -> userDao.getUser(u1));
        assertThrows(DataAccessException.class, () -> userDao.getUser(u2));
    }

    @Test
    @DisplayName("6) clear: idempotent (calling twice is safe)")
    void clear_idempotent() throws DataAccessException {
        userDao.clear();

        assertDoesNotThrow(() -> userDao.clear());

        String username = "postclear_" + UUID.randomUUID();
        String hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        userDao.createUser(new UserData(username, hash, username + "@mail.com"));
        assertEquals(username, userDao.getUser(username).username());
    }

    @Test
    void createAuthSuccess() throws DataAccessException {
        String username = "u_" + UUID.randomUUID();
        String email = username + "@mail.com";
        String plain = "StrongP@ssw0rd!";
        String hash = BCrypt.hashpw(plain, BCrypt.gensalt());

        UserData user = new UserData(username, hash, email);
        userDao.createUser(user);

        String token = UUID.randomUUID().toString();
        var authData = new AuthData(token, username);

        authDao.createAuth(authData);

        AuthData out = authDao.getAuth(token);

        assertEquals(out.username(), authData.username(), "Username mismatch");
        assertEquals(out.authToken(), authData.authToken(), "Auth token mismatch");

    }
    private String createUserReturnUsername() throws DataAccessException {
        String username = "u_" + UUID.randomUUID();
        String email = username + "@mail.com";
        String hash = BCrypt.hashpw("pw", BCrypt.gensalt());
        userDao.createUser(new UserData(username, hash, email));
        return username;
    }


    @Test
    void createAuth_duplicateToken_throws() throws DataAccessException {
        String username = createUserReturnUsername();
        String token = UUID.randomUUID().toString();

        authDao.createAuth(new AuthData(token, username));
        assertThrows(DataAccessException.class,
                () -> authDao.createAuth(new AuthData(token, username)),
                "Expected DataAccessException for duplicate token");
    }

    // --- getAuth ---

    @Test
    void getAuth_success() throws DataAccessException {
        String username = createUserReturnUsername();
        String token = UUID.randomUUID().toString();

        authDao.createAuth(new AuthData(token, username));

        AuthData out = ((SqlAuthDao) authDao).getAuth(token);
        assertEquals(token, out.authToken());
        assertEquals(username, out.username());
    }

    @Test
    void getAuth_missing_throws() {
        String missing = UUID.randomUUID().toString();
        assertThrows(DataAccessException.class,
                () -> ((SqlAuthDao) authDao).getAuth(missing),
                "Expected DataAccessException for missing token");
    }

    // --- deleteAuth ---

    @Test
    void deleteAuth_success() throws DataAccessException {
        String username = createUserReturnUsername();
        String token = UUID.randomUUID().toString();

        authDao.createAuth(new AuthData(token, username));
        assertEquals(username, ((SqlAuthDao) authDao).getAuth(token).username());

        authDao.deleteAuth(new AuthData(token, username));

        assertThrows(DataAccessException.class, () -> ((SqlAuthDao) authDao).getAuth(token));
    }

    @Test
    void deleteAuth_missing_throws() {
        String missing = UUID.randomUUID().toString();
        // username is irrelevant to deletion; DAO uses token
        assertThrows(DataAccessException.class,
                () -> authDao.deleteAuth(new AuthData(missing, "irrelevant")),
                "Expected DataAccessException for missing token");
    }

    // --- clear ---

    @Test
    void clear_wipesAllTokens() throws DataAccessException {
        String username = createUserReturnUsername();
        String t1 = UUID.randomUUID().toString();
        String t2 = UUID.randomUUID().toString();

        authDao.createAuth(new AuthData(t1, username));
        authDao.createAuth(new AuthData(t2, username));

        authDao.clear();

        assertThrows(DataAccessException.class, () -> ((SqlAuthDao) authDao).getAuth(t1));
        assertThrows(DataAccessException.class, () -> ((SqlAuthDao) authDao).getAuth(t2));
    }


    private int getUserIdByUsername(String username) throws Exception {
        try (var ps = conn.prepareStatement("SELECT id FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next(), "user should exist");
                return rs.getInt(1);
            }
        }
    }

    private Integer getCreatorId(int gameId) throws Exception {
        try (var conn = getConnection();
             var ps = conn.prepareStatement("SELECT creator_id FROM games WHERE id=?")) {
            ps.setInt(1, gameId);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next(), "game should exist");
                int v = rs.getInt(1);
                return rs.wasNull() ? null : v;
            }
        }
    }


    // ---------------- clear ----------------

    @Test
    void clear_wipes() throws Exception {
        int id = gameDao.createGame("G1");
        assertNotEquals(0, id);
        gameDao.clear();
        assertNull(((SqlGameDao) gameDao).getGame(id));
    }

    @Test
    void game_clear_idempotent() {
        assertDoesNotThrow(() -> gameDao.clear());
        assertDoesNotThrow(() -> gameDao.clear());
    }

    // ---------------- createGame ----------------

    @Test
    void createGame_success() throws Exception {
        String name = "My Test Game";
        int id = gameDao.createGame(name);
        assertTrue(id > 0);

        GameData g = ((SqlGameDao) gameDao).getGame(id);
        assertNotNull(g);
        assertEquals(id, g.gameID());
        assertEquals(name, g.gameName());
        assertNull(g.whiteUsername());
        assertNull(g.blackUsername());

        Integer creatorId = getCreatorId(id);
        assertNull(creatorId, "creator_id should be NULL until someone joins");
    }

    // ---------------- getGame ----------------

    @Test
    void getGame_missing_returnsNull() throws DataAccessException {
        assertNull(((SqlGameDao) gameDao).getGame(9_999_999));
    }

    // ---------------- listGames ----------------

    @Test
    void listGames_containsCreated() throws Exception {
        int id = gameDao.createGame("Listed");
        List<GameData> list = gameDao.listGames();
        assertTrue(list.stream().anyMatch(g ->
                g.gameID() == id && "Listed".equals(g.gameName())));
    }

    @Test
    void listGames_empty() throws Exception {
        gameDao.clear();
        List<GameData> list = gameDao.listGames();
        assertTrue(list.isEmpty());
    }

    // ---------------- updateGamePlayer ----------------

    @Test
    void updateGamePlayer_white_success_setsSeatAndCreator() throws Exception {
        String user = createUserReturnUsername();
        int userId = getUserIdByUsername(user);
        int gameId = gameDao.createGame("Joinable");

        GameData updated = gameDao.updateGamePlayer(gameId, "WHITE", user);
        assertNotNull(updated);
        assertEquals(user, updated.whiteUsername());
        assertNull(updated.blackUsername());

        Integer creatorId = getCreatorId(gameId);
        assertNotNull(creatorId);
        assertEquals(userId, creatorId.intValue());
    }

    @Test
    void updateGamePlayer_invalidColor_throws() throws Exception {
        String user = createUserReturnUsername();
        int gameId = gameDao.createGame("BadColor");
        assertThrows(DataAccessException.class,
                () -> gameDao.updateGamePlayer(gameId, "GREEN", user));
    }

    @Test
    void updateGamePlayer_missingGame_throws() throws Exception {
        String user = createUserReturnUsername();
        assertThrows(DataAccessException.class,
                () -> gameDao.updateGamePlayer(42424242, "WHITE", user));
    }

    @Test
    void updateGamePlayer_seatTaken_keepsOriginal() throws Exception {
        String u1 = createUserReturnUsername();
        String u2 = createUserReturnUsername();
        int gameId = gameDao.createGame("SeatTest");

        // First join fills WHITE
        gameDao.updateGamePlayer(gameId, "WHITE", u1);

        assertThrows(Exception.class,
                () -> gameDao.updateGamePlayer(gameId, "WHITE", u2));

    }

}



