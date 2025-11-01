package server;

import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.*;
import io.javalin.json.JavalinGson;
import server.handlers.*;
import service.ClearService;
import service.GameService;
import service.UserService;

public class Server {

    private final Javalin app;

    public Server() {
        Gson serializer = new Gson();
        app = Javalin.create(config -> {
            config.staticFiles.add("web");
            config.jsonMapper(new JavalinGson());   // <-- wire Gson
        });

        // Register your endpoints and exception handlers here.

        DatabaseManager db = new DatabaseManager();
        try {
            DatabaseManager.createDatabase();
            DatabaseManager.initializeSchema();
        } catch (DataAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        UserDao userDao = new SqlUserDao();
        AuthDao authDao = new SqlAuthDao();
        GameDao gameDao = new SqlGameDao();

//         UserDao userDao = new MemoryUserDao();
//         AuthDao authDao = new MemoryAuthDao();
//         GameDao gameDao = new MemoryGameDao();

        UserService userService = new UserService(userDao, authDao);
        UserHandler userHandler = new UserHandler(serializer, userService);

        GameService gameService = new GameService(gameDao, authDao);
        GameHandler gameHandler = new GameHandler(serializer,gameService);

        ClearService clearService = new ClearService(userDao, authDao, gameDao);
        ClearHandler clearHandler = new ClearHandler(clearService);

        registerEndpoints(userHandler, gameHandler, clearHandler);

    }

    public int run(int desiredPort) {
        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        app.stop();
    }

    private void registerEndpoints(UserHandler userHandler, GameHandler gameHandler, ClearHandler clearHandler) {

        app.delete("/db", clearHandler::clear);


        app.post("/user", userHandler::register);
        app.post("/session", userHandler::login);
        app.delete("/session",  userHandler::logout);

        app.get("/game", gameHandler::listGames);
        app.post("/game", gameHandler::createGame);
        app.put("/game", gameHandler::joinGame);

    }
}
