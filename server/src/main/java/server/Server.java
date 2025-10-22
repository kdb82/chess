package server;

import com.google.gson.Gson;
import dataaccess.MemoryAuthDao;
import dataaccess.MemoryUserDao;
import io.javalin.*;
import io.javalin.json.JavalinGson;
import server.handlers.*;
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
        var userDao = new MemoryUserDao();
        var authDao = new MemoryAuthDao();

        UserService userService = new UserService(userDao, authDao);

        UserHandler userHandler = new UserHandler(serializer, userService);

        app.post("/user", userHandler::register);

    }

    public int run(int desiredPort) {
        app.start(desiredPort);
        return app.port();
    }

    public void stop() {
        app.stop();
    }
}
