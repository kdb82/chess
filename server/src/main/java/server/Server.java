package server;

import com.google.gson.Gson;
import dataaccess.AuthDao;
import dataaccess.MemoryAuthDao;
import dataaccess.MemoryUserDao;
import dataaccess.UserDao;
import io.javalin.*;
import io.javalin.json.JavalinGson;
import server.handlers.*;
import service.UserService;
import java.util.Random;

public class Server {

    private final Javalin app;

    public Server() {
        Gson serializer = new Gson();
        app = Javalin.create(config -> {
            config.staticFiles.add("web");
            config.jsonMapper(new JavalinGson());   // <-- wire Gson
        });

        // Register your endpoints and exception handlers here.
        UserDao userDao = new MemoryUserDao();
        AuthDao authDao = new MemoryAuthDao();

        UserService userService = new UserService(userDao, authDao);
        UserHandler userHandler = new UserHandler(serializer, userService);

        GameHandler gameHandler = new GameHandler();

        registerEndpoints(userHandler, gameHandler);

    }

    public int run(int desiredPort) {
        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        app.stop();
    }

    private void registerEndpoints(UserHandler userHandler, GameHandler gameHandler) {

        app.delete("/db", ctx -> {
//            ClearHandler.clear();
            ctx.status(200).json("{}");
        });

        app.post("/user", userHandler::register);
        app.post("/session", userHandler::login);
        app.delete("/session",  userHandler::logout);


    }
}
