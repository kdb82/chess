package service;

import dataaccess.*;
import exceptions.*;
import model.*;
import org.junit.jupiter.api.*;
import requests.*;
import results.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private UserService service;
    private MemoryUserDao userDao;
    private MemoryAuthDao authDao;

    @BeforeEach
    void setup() {
        userDao = new MemoryUserDao();
        authDao = new MemoryAuthDao();
        service = new UserService(userDao, authDao);
    }

    // --- REGISTER TESTS ---

    @Test
    @DisplayName("Register Success (Positive Test)")
    void registerSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("alice", "password", "alice@email.com");
        RegisterResult result = service.register(req);

        assertEquals("alice", result.username());
        assertNotNull(result.authToken());
        assertNotNull(authDao.getAuth(result.authToken()));
    }

    @Test
    @DisplayName("Register Fail (Negative Test)")
    void registerAlreadyTaken() throws Exception {
        RegisterRequest req = new RegisterRequest("bob", "pw", "bob@email.com");
        service.register(req);

        RegisterRequest duplicate = new RegisterRequest("bob", "pw", "bob@email.com");
        assertThrows(AlreadyTakenException.class, () -> service.register(duplicate));
    }

    // --- LOGIN TESTS ---

    @Test
    @DisplayName("Login Success (Positive Test)")
    void loginSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("chris", "securepw", "chris@mail.com");
        service.register(req);

        LoginRequest login = new LoginRequest("chris", "securepw");
        LoginResult result = service.login(login);

        assertEquals("chris", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    @DisplayName("Login Fail (Negative Test)")
    void loginWrongPassword() throws Exception {
        RegisterRequest req = new RegisterRequest("dana", "goodpw", "dana@mail.com");
        service.register(req);

        LoginRequest bad = new LoginRequest("dana", "WRONGPW");
        assertThrows(UnauthorizedException.class, () -> service.login(bad));
    }

    // --- LOGOUT TESTS ---

    @Test
    @DisplayName("Logout Success (Positive Test)")
    void logoutSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest("ed", "pw", "ed@mail.com");
        RegisterResult reg = service.register(req);

        LogoutRequest logout = new LogoutRequest(reg.authToken());
        assertDoesNotThrow(() -> service.logout(logout));

        assertNull(authDao.getAuth(reg.authToken())); // token should be deleted
    }

    @Test
    @DisplayName("Logout Fail (Negative Test)")
    void logoutInvalidToken() {
        LogoutRequest logout = new LogoutRequest("invalid-token");
        assertThrows(UnauthorizedException.class, () -> service.logout(logout));
    }
}
