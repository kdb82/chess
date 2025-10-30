package dataaccess;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static String databaseName;
    private static String dbUsername;
    private static String dbPassword;
    private static String connectionUrl;

    /*
     * Load the database information for the db.properties file.
     */
    static {
        loadPropertiesFromResources();
    }

    /**
     * Creates the database if it does not already exist.
     */
    static public void createDatabase() throws DataAccessException {
        var statement = "CREATE DATABASE IF NOT EXISTS " + databaseName;
        try (var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("failed to create database", ex);
        }
    }

    /**
     * Create a connection to the database and sets the catalog based upon the
     * properties specified in db.properties. Connections to the database should
     * be short-lived, and you must close the connection when you are done with it.
     * The easiest way to do that is with a try-with-resource block.
     * <br/>
     * <code>
     * try (var conn = DatabaseManager.getConnection()) {
     * // execute SQL statements.
     * }
     * </code>
     */
    static Connection getConnection() throws DataAccessException {
        try {
            //do not wrap the following line with a try-with-resources
            var conn = DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
            conn.setCatalog(databaseName);
            return conn;
        } catch (SQLException ex) {
            throw new DataAccessException("failed to get connection", ex);
        }
    }

    public void openConnection() throws DataAccessException {
        try {
            var conn = getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeConnection(Boolean commit) throws DataAccessException {
        var conn = getConnection();
        try  {
            if (commit) {
                conn.commit();
            } else  {
                conn.rollback();
            }
            conn.close();
        } catch (SQLException e) {
            throw new DataAccessException("failed to close connection", e);
        }
    }

    private static void loadPropertiesFromResources() {
        try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
            if (propStream == null) {
                throw new Exception("Unable to load db.properties");
            }
            Properties props = new Properties();
            props.load(propStream);
            loadProperties(props);
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties", ex);
        }
    }

    private static void loadProperties(Properties props) {
        databaseName = props.getProperty("db.name");
        dbUsername = props.getProperty("db.user");
        dbPassword = props.getProperty("db.password");

        var host = props.getProperty("db.host");
        var port = Integer.parseInt(props.getProperty("db.port"));
        connectionUrl = String.format("jdbc:mysql://%s:%d", host, port);
    }

    public static void initializeSchema() throws DataAccessException {
        final String users = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT not null PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL UNIQUE,
                    email VARCHAR(255) NOT NULL UNIQUE
                );
                """;

        final String tokens =
            """
            CREATE TABLE IF NOT EXISTS tokens (
                 token varchar(255) NOT NULL,
                 user_id INT NOT NULL,
                 PRIMARY KEY (token),
                 INDEX idx_tokens_user (user_id),
                 CONSTRAINT fk_tokens
                 FOREIGN KEY (user_id) REFERENCES users(id)
                     ON DELETE CASCADE
                     ON UPDATE RESTRICT
                     );
        """;

        final String games = """
            CREATE TABLE IF NOT EXISTS games (
                id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
                creator_id INT NOT NULL,
                game_name VARCHAR(255) NULL,
                turn_color ENUM('WHITE', 'BLACK') NOT NULL DEFAULT 'WHITE',
                black_king_location VARCHAR(5) NOT NULL DEFAULT 'e1',
                white_king_location VARCHAR(5) NOT NULL DEFAULT 'e8',
                status ENUM('OPEN','IN_PROGRESS','FINISHED','ABANDONED') NOT NULL DEFAULT 'OPEN',
                result ENUM('WHITE','BLACK','DRAW','UNDECIDED') NOT NULL DEFAULT 'UNDECIDED',
                turn ENUM('WHITE','BLACK') NOT NULL DEFAULT 'WHITE',
                CONSTRAINT fk_games
                    FOREIGN KEY (creator_id) REFERENCES users(id)
                    ON DELETE RESTRICT
                    ON UPDATE RESTRICT
            );
        """;

        final String game_players = """
                CREATE TABLE IF NOT EXISTS game_players (
                    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                
                    game_id INT NOT NULL,
                    user_id INT NOT NULL,
                    color ENUM('WHITE', 'BLACK') NOT NULL,
                
                    CONSTRAINT fk_gp_game
                        FOREIGN KEY (game_id)
                        REFERENCES games(id)
                        ON DELETE CASCADE
                        ON UPDATE RESTRICT,
                
                    CONSTRAINT fk_gp_user
                        FOREIGN KEY (user_id)
                        REFERENCES users(id)
                        ON DELETE CASCADE
                        ON UPDATE RESTRICT,
                
                    UNIQUE (game_id, color),
                    UNIQUE (game_id, user_id)
                );
                
                """;

        final String game_moves = """
                CREATE TABLE IF NOT EXISTS game_moves (
                    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    game_id INT NOT NULL,
                    move_number INT NOT NULL,
                    made_by ENUM('WHITE', 'BLACK') NOT NULL,
                    from_sq VARCHAR(5) NOT NULL,
                    to_sq VARCHAR(5) NOT NULL,
                    promotion TINYINT NOT NULL DEFAULT 0,
                    capture TINYINT NOT NULL DEFAULT 0,
                    is_check TINYINT NOT NULL DEFAULT 0,
                    checkmate TINYINT NOT NULL DEFAULT 0,
                    CONSTRAINT fk_game_moves
                        FOREIGN KEY (game_id) REFERENCES games(id)
                        ON DELETE CASCADE
                        ON UPDATE RESTRICT
                    );
        """;

        try (var connection = getConnection()) {
            if (connection != null) {
                var stmnt = connection.createStatement();
                stmnt.executeUpdate(users);
                stmnt.executeUpdate(tokens);
                stmnt.executeUpdate(games);
                stmnt.executeUpdate(game_players);
                stmnt.executeUpdate(game_moves);
            }
            else  {
                throw new SQLException("Couldn't connect to database");
            }
        }
        catch (SQLException e) {
            throw new DataAccessException("Failed to initialize tables", e);
        }
    }
}
