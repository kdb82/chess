package dataaccess;

import model.AuthData;

public interface AuthDao {

    /**
     * Removes all authorization data (used for the /db clear endpoint)
     */
    void clear();

    /**
     * Creates a new authentication record for a logged-in or newly registered user
     *
     * @param auth The AuthData record (token + username)
     * @throws DataAccessException if the token cannot be created
     */
    void createAuth(AuthData auth) throws DataAccessException;

    /**
     * Retrieves an authentication record by its token
     *
     * @param token The auth token string
     * @return The AuthData if found, or null if not found
     * @throws DataAccessException if database access fails
     */
    AuthData getAuth(String token) throws DataAccessException;

    void deleteAuth(AuthData authData);

}

