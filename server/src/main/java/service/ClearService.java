package service;

import dataaccess.AuthDao;
import dataaccess.GameDao;
import dataaccess.UserDao;
import results.ClearResult;

public class ClearService {
    private final AuthDao authDao;
    private final UserDao userDao;
//    private final GameDao gameDao;

    public ClearService(UserDao userDao, AuthDao authDao) {
        this.authDao = authDao;
        this.userDao = userDao;
//        this.gameDao = gameDao;
    }

    public ClearResult clear(){
        authDao.clear();
        userDao.clear();
//        gameDao.clear();



        return new ClearResult();

    }
}
