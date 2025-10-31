package dataaccess;

import exceptions.AlreadyTakenException;
import model.UserData;

public interface UserDao {

    /**
     * Deletes all User Data used for /db clear endpoint
     */
    void clear() throws DataAccessException;

    /**
     *
     * @param user add UserData to user data structure
     */
    void createUser(UserData user) throws DataAccessException;

    /**
     * @param username username of user record to add to db
     * @return UserData object if found
     * @throws AlreadyTakenException If username is already taken
     */
    UserData getUser(String username) throws AlreadyTakenException, DataAccessException;




}
