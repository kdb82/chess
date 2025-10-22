package server.handlers;

import com.google.gson.Gson;
import io.javalin.http.Context;
import service.ClearService;

import java.util.Map;

public class ClearHandler {
    private final ClearService clearService;

    public ClearHandler(ClearService clearService) {
        this.clearService = clearService;
    }

    public void clear(Context ctx) {
        var result = clearService.clear();
        ctx.status(200).json(Map.of());

    }

}
