package server;

import com.google.gson.Gson;
import io.javalin.*;
import server.handlers.ClearHandler;
import server.handlers.*;
import service.UserService;

public class Server {

    private final Javalin app;

    public Server() {
        app = Javalin.create(config -> config.staticFiles.add("web"));
        // Register your endpoints and exception handlers here.
        Gson gson = new Gson();
//        DataAccess dao = new MemoryDataAccess();
        UserService userService = new UserService();
        UserHandler userHandler = new UserHandler(gson, userService);

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
