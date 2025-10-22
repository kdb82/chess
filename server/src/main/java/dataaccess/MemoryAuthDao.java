package dataaccess;

import model.AuthData;

import java.util.HashMap;

public class MemoryAuthDao implements AuthDao {
    private final HashMap<String, AuthData> authDataHashMap = new HashMap<>();

    @Override
    public void clear() {
        authDataHashMap.clear();
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        var token = auth.authToken();
        authDataHashMap.put(token, auth);
    }

    @Override
    public AuthData getAuth(String token) throws DataAccessException {
        if (authDataHashMap.get(token) == null) {
            throw new DataAccessException("failed to get auth");
        }
        return authDataHashMap.get(token);
    }
}
