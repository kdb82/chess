package server.handlers;

import io.javalin.http.Context;
import service.ClearService;

import java.util.Map;

public class ClearHandler {
    private final ClearService clearService;

    public ClearHandler(ClearService clearService) {
        this.clearService = clearService;
    }

    public void clear(Context ctx) {
        clearService.clear();
        ctx.status(200).json(Map.of());

    }

}
