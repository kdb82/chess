package server.handlers;

import com.google.gson.Gson;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class ClearHandler {
    private Gson gson;


    public ClearHandler() {
        this.gson = new Gson();
    }

}
