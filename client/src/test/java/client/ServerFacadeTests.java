package client;

import exception.ResponseException;
import org.junit.jupiter.api.*;
import requests.*;
import results.*;
import server.Server;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        String url = "http://localhost:" + port;
        System.out.println("Started test HTTP server on " + url);
        facade = new ServerFacade(url);
    }

    @AfterAll
    static void stopServer() throws ResponseException {
        facade.clear();
        server.stop();
    }

    @BeforeEach
    public void clearBetweenTests() throws ResponseException {
        facade.clear();
    }


    @Test
    public void registerPositive() throws Exception {
        var req = new RegisterRequest("alice", "password", "alice@email.com");
        RegisterResult res = facade.register(req);

        assertNotNull(res);
        assertNotNull(res.authToken());
        assertFalse(res.authToken().isBlank());
    }

    @Test
    public void registerNegative_DuplicateUsername() throws Exception {
        var req = new RegisterRequest("bob", "password", "bob@email.com");
        facade.register(req);

        assertThrows(ResponseException.class, () -> facade.register(req));
    }

    @Test
    public void loginPositive() throws Exception {
        var regReq = new RegisterRequest("charlie", "pw123", "charlie@email.com");
        facade.register(regReq);

        var loginReq = new LoginRequest("charlie", "pw123");
        LoginResult res = facade.login(loginReq);

        assertNotNull(res);
        assertNotNull(res.authToken());
        assertFalse(res.authToken().isBlank());
    }

    @Test
    public void loginNegative_WrongPassword() throws Exception {
        var regReq = new RegisterRequest("dan", "correct", "dan@email.com");
        facade.register(regReq);

        var badLogin = new LoginRequest("dan", "wrong");
        assertThrows(ResponseException.class, () -> facade.login(badLogin));
    }


    @Test
    public void logoutPositive() throws Exception {
        var regReq = new RegisterRequest("eve", "secret", "eve@email.com");
        RegisterResult regRes = facade.register(regReq);

        var logoutReq = new LogoutRequest(regRes.authToken());
        LogoutResult res = facade.logout(logoutReq);

        assertNotNull(res);
    }

    @Test
    public void logoutNegative_InvalidToken() {
        var logoutReq = new LogoutRequest("not-a-real-token");
        assertThrows(ResponseException.class, () -> facade.logout(logoutReq));
    }


    @Test
    public void createGamePositive() throws Exception {
        var regReq = new RegisterRequest("frank", "pw", "frank@email.com");
        RegisterResult regRes = facade.register(regReq);

        var gameReq = new CreateGameRequest("Frank's Game");
        CreateGameResult gameRes = facade.createGame(gameReq, regRes.authToken());

        assertNotNull(gameRes);
        assertTrue(gameRes.gameID() > 0);
    }

    @Test
    public void createGameNegative_InvalidAuth() {
        var gameReq = new CreateGameRequest("NoAuth Game");
        assertThrows(ResponseException.class,
                () -> facade.createGame(gameReq, "bad-token"));
    }

    @Test
    public void listGamesPositive() throws Exception {
        var regReq = new RegisterRequest("gina", "pw", "gina@email.com");
        RegisterResult regRes = facade.register(regReq);

        var gameReq = new CreateGameRequest("Game 1");
        facade.createGame(gameReq, regRes.authToken());

        var listReq = new ListGameRequest(regRes.authToken());
        ListGamesResult listRes = facade.listGames(listReq);

        assertNotNull(listRes);
        List<GameSummary> games = listRes.games();
        assertNotNull(games);
        assertFalse(games.isEmpty());
    }

    @Test
    public void listGamesNegative_InvalidAuth() {
        var listReq = new ListGameRequest("bogus-token");
        assertThrows(ResponseException.class, () -> facade.listGames(listReq));
    }


    @Test
    public void joinGamePositive() throws Exception {
        var regReq = new RegisterRequest("henry", "pw", "henry@email.com");
        RegisterResult regRes = facade.register(regReq);

        var gameReq = new CreateGameRequest("Joinable Game");
        CreateGameResult gameRes = facade.createGame(gameReq, regRes.authToken());

        var joinReq = new JoinGameRequest(gameRes.gameID(), "WHITE");
        assertDoesNotThrow(() -> facade.joinGame(joinReq, regRes.authToken()));
    }

    @Test
    public void joinGameNegative_BadGameId() throws Exception {
        var regReq = new RegisterRequest("irene", "pw", "irene@email.com");
        RegisterResult regRes = facade.register(regReq);

        var joinReq = new JoinGameRequest(999999, "WHITE");
        assertThrows(ResponseException.class, () -> facade.joinGame(joinReq, regRes.authToken()));
    }


    @Test
    public void clearPositive_AllowsReRegisterSameUser() throws Exception {
        var req = new RegisterRequest("jack", "pw", "jack@email.com");
        facade.register(req);

        facade.clear();

        RegisterResult res2 = facade.register(req);
        assertNotNull(res2);
        assertNotNull(res2.authToken());
    }

    @Test
    public void clearNegative_BadBaseUrl() {
        ServerFacade badFacade = new ServerFacade("http://localhost:1");
        assertThrows(ResponseException.class, badFacade::clear);
    }
}
