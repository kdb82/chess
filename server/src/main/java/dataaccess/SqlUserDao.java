package dataaccess;

import exceptions.AlreadyTakenException;
import model.UserData;

public class SqlUserDao implements UserDao {
    private final DatabaseManager databaseManager;

    public SqlUserDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void clear() {

    }

    @Override
    public void createUser(UserData user) throws DataAccessException {

    }

    @Override
    public UserData getUser(String username) throws AlreadyTakenException {
        return null;
    }
}
