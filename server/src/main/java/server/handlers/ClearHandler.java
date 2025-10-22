package server.handlers;

import com.google.gson.Gson;
import dataaccess.UserDao;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import service.ClearService;
import service.UserService;

public class ClearHandler {
    private final Gson serializer;
    private final ClearService clearService;


    public ClearHandler() {
        this.serializer = new Gson();
        this.clearService = new ClearService();
    }

    public void clear(Context ctx) {


    }

}
