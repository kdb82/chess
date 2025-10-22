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

    public LoginResult login(LoginRequest request) {
        return null;
    }

    public void logout(LogoutRequest request) {
    }
}
