package server.handlers;

import dataaccess.DataAccessException;
import io.javalin.http.Context;
import service.ClearService;

import java.util.Map;

public class ClearHandler {
    private final ClearService clearService;

    public ClearHandler(ClearService clearService) {
        this.clearService = clearService;
    }

    public void clear(Context ctx) throws DataAccessException {
        try {
            clearService.clear();
            ctx.status(200).json(Map.of());
        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", e.getMessage()));
        }

    }

}
