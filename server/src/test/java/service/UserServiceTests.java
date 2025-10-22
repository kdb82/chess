package service;

import Exceptions.AlreadyTakenException;
import dataaccess.MemoryAuthDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.RegisterRequest;
import results.RegisterResult;
import dataaccess.MemoryUserDao;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTests {

    private UserService userService;

    @BeforeEach
    void setup() {
        var Userdao = new MemoryUserDao();
        var Authdao = new MemoryAuthDao();
        userService = new UserService(Userdao, Authdao);
    }

    @Test
    void register_returnsCorrectUsername() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("kaden", "pw", "kaden@example.com");

        // Act
        RegisterResult result = userService.register(request);

        System.out.println(result);

        // Assert
        assertEquals("kaden", result.username(),
                "Expected the username in RegisterResult to match the request username.");
        assertNotNull(result.authToken(),
                "Expected an authToken to be generated and not null.");
        assertFalse(result.authToken().isEmpty(),
                "Expected the authToken string to not be empty.");
    }

    @Test
    void register_failsWhenUsernameAlreadyExists() throws Exception {
        // Arrange
        RegisterRequest first = new RegisterRequest("kaden", "pw", "k@k.com");
        RegisterRequest second = new RegisterRequest("kaden", "pw2", "dup@k.com");

        userService.register(first); // first call should succeed
        System.out.println("Successfully registered the first register");
        // Act + Assert
        Exception ex = assertThrows(AlreadyTakenException.class, () -> {
            userService.register(second);
        });

        // Optionally confirm the exception message
        assertTrue(ex.getMessage().contains("taken") || ex.getMessage().contains("exists"));
        assertNotNull(ex.getMessage());
    }
}
