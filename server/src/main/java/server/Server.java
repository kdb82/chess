package server;

import com.google.gson.Gson;
import dataaccess.MemoryAuthDao;
import dataaccess.MemoryUserDao;
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
        var userDao = new MemoryUserDao();
        var authDao = new MemoryAuthDao();

        UserService userService = new UserService(userDao, authDao);
        UserHandler userHandler = new UserHandler(serializer, userService);
        GameHandler gameHandler = new GameHandler();

        registerendpoints(userHandler, gameHandler);

    }

    public int run(int desiredPort) {
        if (desiredPort == 0) {
            Random random = new Random();
            desiredPort = random.nextInt(60000-50000+1) + 50000;
        }
        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        app.stop();
    }

    private void registerendpoints(UserHandler userHandler, GameHandler gameHandler) {

        app.delete("/db", ctx -> {
//            ClearHandler.clear();
            ctx.status(200).json("{}");
        });

        app.post("/user", userHandler::register);
        app.post("/session", userHandler::login);
        app.post("/session",  userHandler::logout);


    }
}
