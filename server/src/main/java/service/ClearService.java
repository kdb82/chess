package service;

import dataaccess.AuthDao;
import dataaccess.DataAccessException;
import dataaccess.GameDao;
import dataaccess.UserDao;
import results.ClearResult;

public class ClearService {
    private final AuthDao authDao;
    private final UserDao userDao;
    private final GameDao gameDao;

    public ClearService(UserDao userDao, AuthDao authDao, GameDao gameDao) {
        this.authDao = authDao;
        this.userDao = userDao;
        this.gameDao = gameDao;
    }

    public ClearResult clear() throws DataAccessException {
        gameDao.clear();
        authDao.clear();
        userDao.clear();



        return new ClearResult();

    }
}
