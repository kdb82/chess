package service;

import dataaccess.*;
import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import requests.*;
import results.*;

import java.util.UUID;

public class UserService {
    private final UserDao userDao;
    private final AuthDao authDao;

    public UserService(UserDao userDao, AuthDao authDao) {
        this.userDao = userDao;
        this.authDao = authDao;
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    public RegisterResult register(RegisterRequest request) throws AlreadyTakenException, BadRequestException, DataAccessException {
        if (request == null || request.username() == null ||
                request.password() == null || request.email() == null ||
                request.username().isBlank() || request.password().isBlank() || request.email().isBlank()) {
            throw new BadRequestException("Error: username or password or email is blank");
        }
        var username = request.username();
        if (userDao.getUser(username) != null) {
            throw new AlreadyTakenException("Error: username is already taken");
        }

        String hashedPw = BCrypt.hashpw(request.password(), BCrypt.gensalt());
        UserData userData = new UserData(username, hashedPw, request.email());
        userDao.createUser(userData);

        var token = generateToken();
        AuthData auth = new AuthData(token, username);
        authDao.createAuth(auth);

        return new RegisterResult(username, token);
    }

    public LoginResult login(LoginRequest request) throws UnauthorizedException, BadRequestException, DataAccessException {
        validateLoginData(request);
        var username = request.username();

        UserData user = userDao.getUser(username);
        if (user == null) {
            throw new UnauthorizedException("Error: username or password is invalid");
        }

        String hashedPw = BCrypt.hashpw(request.password(), BCrypt.gensalt());

        //verifyPassword
        if (!BCrypt.checkpw(user.password(), hashedPw)) {
            throw new UnauthorizedException("Error: password is incorrect");
        }

        AuthData authData = new AuthData(generateToken(), username);
        authDao.createAuth(authData);

        return new LoginResult(user.username(), authData.authToken());
    }

    public void logout(LogoutRequest request) throws DataAccessException, UnauthorizedException {
        final String token = request.authToken();
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Error: token is null");
        }
        AuthData authData = authDao.getAuth(token);
        if (authData == null) {
            throw new UnauthorizedException("Error: token is not valid");
        }
        authDao.deleteAuth(authData);
    }

    private static void validateLoginData(LoginRequest request) throws BadRequestException {
        if (request == null || request.username() == null || request.password() == null ||
                request.username().isBlank() || request.password().isBlank()) {
            throw new BadRequestException("Error: username or password or email is blank");
        }
    }

}
