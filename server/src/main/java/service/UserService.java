package service;

import Exceptions.AlreadyTakenException;
import Exceptions.BadRequestException;
import dataaccess.DataAccessException;
import dataaccess.MemoryAuthDao;
import dataaccess.MemoryUserDao;
import model.AuthData;
import model.UserData;
import requests.*;
import results.*;

import java.util.UUID;

public class UserService {
    MemoryUserDao userDao = new MemoryUserDao();
    MemoryAuthDao authDao = new MemoryAuthDao();

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

        AuthData auth = new AuthData(generateToken(), username);
        authDao.createAuth(auth);
    }

    public LoginResult login(LoginRequest request) {
        return null;
    }

    public LogoutResult logout(LogoutRequest request) {
        return null;
    }
}
