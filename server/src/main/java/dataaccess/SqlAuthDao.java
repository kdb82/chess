package dataaccess;

import model.AuthData;

public class SqlAuthDao implements AuthDao {
    private final DatabaseManager databaseManager;

    public SqlAuthDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void clear() {

    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {

    }

    @Override
    public AuthData getAuth(String token) throws DataAccessException {
        return null;
    }

    @Override
    public void deleteAuth(AuthData authData) {

    }
}
