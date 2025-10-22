package service;

import dataaccess.AuthDao;
import dataaccess.GameDao;
import dataaccess.UserDao;

public class ClearService {
    private final AuthDao authDao;
    private final UserDao userDao;
    private final GameDao gameDao;

    public ClearService(AuthDao authDao, UserDao userDao, GameDao gameDao) {
        this.authDao = authDao;
        this.userDao = userDao;
        this.gameDao = gameDao;
    }

    public void clear(){
        authDao.clear();
        userDao.clear();
//        gameDao.clear();

    }
}
