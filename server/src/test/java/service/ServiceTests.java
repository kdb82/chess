package service;

import dataaccess.*;
import exceptions.*;
import model.GameData;
import org.junit.jupiter.api.*;
import requests.*;
import results.*;

import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
public class ServiceTests {

    private UserDao userDao;
    private AuthDao authDao;
    private GameDao gameDao;
    private DatabaseManager databaseManager;

    private UserService userService;
    private GameService gameService;
    private ClearService clearService;

    @BeforeEach
    void setup() {
        userDao = new SqlUserDao();
//        userDao = new MemoryUserDao();
//        authDao = new MemoryAuthDao();
        authDao = new SqlAuthDao();
        gameDao = new MemoryGameDao();



        userService = new UserService(userDao, authDao);
        gameService = new GameService(gameDao, authDao);
        clearService = new ClearService(userDao, authDao, gameDao);
    }

    @AfterEach
    void tearDown() throws DataAccessException {
        if (userDao instanceof SqlUserDao) {
            userDao.clear();
        }
    }

    // =========================
    // UserService: register
    // =========================

    @Test
    @DisplayName("01 Register - Positive")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest("alice", "pw", "alice@mail.com");
        RegisterResult res = userService.register(req);

        assertEquals("alice", res.username());
        assertNotNull(res.authToken());
        assertFalse(res.authToken().isEmpty());

        // token persisted
        assertNotNull(authDao.getAuth(res.authToken()));
        // user persisted
        assertNotNull(userDao.getUser("alice"));
    }

    @Test
    @DisplayName("02 Register - Negative (Duplicate Username)")
    void register_duplicateUsername() throws Exception {
        userService.register(new RegisterRequest("bob", "pw", "bob@mail.com"));

        assertThrows(AlreadyTakenException.class, () ->
                userService.register(new RegisterRequest("bob", "pw2", "bob2@mail.com")));
    }

    // =========================
    // UserService: login
    // =========================

    @Test
    @DisplayName("03 Login - Positive")
    void login_success() throws Exception {
        userService.register(new RegisterRequest("carl", "goodpw", "carl@mail.com"));

        LoginResult res = userService.login(new LoginRequest("carl", "goodpw"));

        assertEquals("carl", res.username());
        assertNotNull(res.authToken());
        assertNotNull(authDao.getAuth(res.authToken()));
    }

    @Test
    @DisplayName("04 Login - Negative (Wrong Password)")
    void login_wrongPassword() throws Exception {
        userService.register(new RegisterRequest("dana", "goodpw", "dana@mail.com"));

        assertThrows(UnauthorizedException.class, () ->
                userService.login(new LoginRequest("dana", "BAD!PASSWORD")));
    }

    // =========================
    // UserService: logout
    // =========================

    @Test
    @DisplayName("05 Logout - Positive")
    void logout_success() throws Exception {
        RegisterResult reg = userService.register(new RegisterRequest("ed", "pw", "ed@mail.com"));

        assertDoesNotThrow(() -> userService.logout(new LogoutRequest(reg.authToken())));
        // token removed

        assertThrows(Exception.class, () -> userService.logout(new LogoutRequest(reg.authToken())));

    }

    @Test
    @DisplayName("06 Logout - Negative (Invalid Token)")
    void logout_invalidToken() {
        assertThrows(Exception.class, () ->
                userService.logout(new LogoutRequest("no-such-token")));
    }

    // =========================
    // GameService: listGames
    // =========================

    @Test
    @DisplayName("07 ListGames - Positive (Authorized)")
    void listGames_success() throws Exception {
        // Need an authorized user
        RegisterResult reg = userService.register(new RegisterRequest("fran", "pw", "fran@mail.com"));

        // Create a couple of games (authorized)
        gameService.createGame(new GameRequest("G1"), reg.authToken());
        gameService.createGame(new GameRequest("G2"), reg.authToken());

        ListGameRequest request = new ListGameRequest(reg.authToken());
        ListGamesResult out = gameService.listGames(request);
        List<GameSummary> games = out.games();

        assertNotNull(games);
        assertTrue(games.size() >= 2);
        assertTrue(games.stream().anyMatch(g -> "G1".equals(g.gameName())));
        assertTrue(games.stream().anyMatch(g -> "G2".equals(g.gameName())));
    }

    @Test
    @DisplayName("08 ListGames - Negative (Unauthorized)")
    void listGames_unauthorized() {
        ListGameRequest request = new ListGameRequest("no-such-token");
        assertThrows(Exception.class, () ->
                gameService.listGames(request));
    }

    // =========================
    // GameService: createGame
    // =========================

    @Test
    @DisplayName("09 CreateGame - Positive")
    void createGame_success() throws Exception {
        RegisterResult reg = userService.register(new RegisterRequest("gina", "pw", "gina@mail.com"));
        CreateGameResult created = gameService.createGame(new GameRequest("My Match"), reg.authToken());

        assertTrue(created.gameID() > 0);
        GameData stored = gameDao.getGame(created.gameID());
        assertNotNull(stored);
        assertEquals("My Match", stored.gameName());
    }

    @Test
    @DisplayName("10 CreateGame - Negative (Unauthorized)")
    void createGame_unauthorized() {
        assertThrows(Exception.class, () ->
                gameService.createGame(new GameRequest("X"), "invalid-token"));
    }

    // =========================
    // GameService: joinGame
    // =========================

    @Test
    @DisplayName("11 JoinGame - Positive (Claim WHITE)")
    void joinGame_success_white() throws Exception {
        RegisterResult reg = userService.register(new RegisterRequest("hank", "pw", "hank@mail.com"));
        int id = gameService.createGame(new GameRequest("Room"), reg.authToken()).gameID();

        assertDoesNotThrow(() ->
                gameService.joinGame(new JoinGameRequest(id, "WHITE"), reg.authToken()));

        GameData game = gameDao.getGame(id);
        assertEquals("hank", game.whiteUsername());
        assertNull(game.blackUsername());
    }

    @Test
    @DisplayName("12 JoinGame - Negative (Seat Already Taken)")
    void joinGame_alreadyTaken() throws Exception {
        // alice claims WHITE
        RegisterResult alice = userService.register(new RegisterRequest("alice2", "pw", "a2@mail.com"));
        int id = gameService.createGame(new GameRequest("Room2"), alice.authToken()).gameID();
        gameService.joinGame(new JoinGameRequest(id, "WHITE"), alice.authToken());

        // bob tries to claim WHITE
        RegisterResult bob = userService.register(new RegisterRequest("bob2", "pw", "b2@mail.com"));

        assertThrows(AlreadyTakenException.class, () ->
                gameService.joinGame(new JoinGameRequest(id, "WHITE"), bob.authToken()));
    }

    // (Optional extra negative for join: bad color or bad gameID)
    @Test
    @DisplayName("13 JoinGame - Negative (Bad Request: invalid color)")
    void joinGame_badRequest_invalidColor() throws Exception {
        RegisterResult reg = userService.register(new RegisterRequest("ivy", "pw", "ivy@mail.com"));
        int id = gameService.createGame(new GameRequest("Room3"), reg.authToken()).gameID();

        assertThrows(BadRequestException.class, () ->
                gameService.joinGame(new JoinGameRequest(id, "BLUE"), reg.authToken()));
    }

    // =========================
    // ClearService: clear
    // =========================

    @Test
    @DisplayName("14 Clear - Positive (Wipes users, auth, games)")
    void clear_success() throws Exception {
        // seed data
        RegisterResult reg = userService.register(new RegisterRequest("kate", "pw", "kate@mail.com"));
        int id = gameService.createGame(new GameRequest("Z1"), reg.authToken()).gameID();

        assertNotNull(userDao.getUser("kate"));
        assertNotNull(authDao.getAuth(reg.authToken()));
        assertNotNull(gameDao.getGame(id));

        // clear
        assertDoesNotThrow(() -> clearService.clear());

        if (userDao instanceof SqlUserDao) {
            assertThrows(DataAccessException.class, () -> userDao.getUser("kate"));
        } else {assertNull(userDao.getUser("kate"));}


        assertThrows(Exception.class, () -> authDao.getAuth(reg.authToken()));
        assertTrue(gameDao.listGames().isEmpty());
    }

    @Test
    @DisplayName("15 Clear - Negative (Idempotent: clearing empty state)")
    void clear_whenEmpty_doesNotThrow() {
        assertDoesNotThrow(() -> clearService.clear());
        // call again to prove idempotence
        assertDoesNotThrow(() -> clearService.clear());
    }
}
