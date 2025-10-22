package service;

import Exceptions.AlreadyTakenException;
import Exceptions.BadRequestException;
import Exceptions.UnauthorizedException;
import dataaccess.DataAccessException;
import dataaccess.MemoryAuthDao;
import dataaccess.MemoryUserDao;
import model.AuthData;
import model.UserData;
import requests.*;
import results.*;

import java.util.UUID;

public class UserService {
    private final MemoryUserDao userDao;
    private final MemoryAuthDao authDao;

    public UserService(MemoryUserDao userDao, MemoryAuthDao authDao) {
        this.userDao = userDao;
        this.authDao = authDao;
    }

    public static String generateToken() {
        return UUID.randomUUID().toString();
    }

    public RegisterResult register(RegisterRequest request) throws AlreadyTakenException, BadRequestException, DataAccessException {
        if (request.username().isBlank() || request.password().isBlank() || request.email().isBlank()) {
            throw new BadRequestException("username or password or email is blank");
        }
        var username = request.username();
        if (userDao.getUser(username) != null) {
            throw new AlreadyTakenException("username is already taken");
        }

        UserData userData = new UserData(username, request.password(), request.email());
        userDao.createUser(userData);

        var token = generateToken();
        AuthData auth = new AuthData(token, username);
        authDao.createAuth(auth);

        return new RegisterResult(username, token);
    }

    public LoginResult login(LoginRequest request) throws UnauthorizedException, BadRequestException{
        validateLoginData(request);
        var username = request.username();
        var requestPassword = request.password();

        UserData user = userDao.getUser(username);
        if (user == null) {
            throw new UnauthorizedException("username or password is invalid");
        }

        verifyPassword(requestPassword, user.password());

        AuthData authData = new AuthData(generateToken(), username);
        authDao.createAuth(authData);

        return new LoginResult(user.username(), user.password());
    }

    public void logout(LogoutRequest request) {
        String token = request.authToken();


    }

    private static void validateLoginData(LoginRequest request) throws BadRequestException {
        if (request.username().isBlank() || request.password().isBlank()) {
            throw new BadRequestException("username or password or email is blank");
        }
    }

    private static void verifyPassword(String requestedPassword, String recievedPassword) throws UnauthorizedException {
        if (!requestedPassword.equals(recievedPassword)) {
            throw new UnauthorizedException("password is incorrect");
        }

    }

}
