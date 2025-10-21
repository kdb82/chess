package server.handlers;

import Exceptions.AlreadyTakenException;
import Exceptions.BadRequestException;
import Exceptions.UnauthorizedException;
import com.google.gson.Gson;
import io.javalin.http.Context;
import requests.LoginRequest;
import requests.LogoutRequest;
import requests.RegisterRequest;
import results.*;
import service.UserService;

import java.util.Map;

public class UserHandler{
    private final Gson serializer;
    private final UserService userService;

    public UserHandler(Gson gson, UserService userService) {
        this.serializer = gson;
        this.userService = userService;
    }

    public void register(Context ctx) {
        try {
            String body = ctx.body();
            if (body.isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            RegisterRequest request = serializer.fromJson(body, RegisterRequest.class);
            if (request.email().isBlank() ||  request.password().isBlank() || request.username().isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            RegisterResult result = userService.register(request);
            ctx.status(200).json(result);
        }
        catch(AlreadyTakenException ex) {
            ctx.status(402).json(Map.of("message", "username already taken"));
        }
        catch(BadRequestException ex) {
            ctx.status(400).json(Map.of("message", ex.getMessage()));
        }

    }

    public void login(Context ctx) {
        try {
            String body = ctx.body();
            if (body.isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            LoginRequest request = serializer.fromJson(body, LoginRequest.class);
            if (request.password().isBlank() || request.username().isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            LoginResult result = userService.login(request);
            ctx.status(200).json(result);

        }

        catch (BadRequestException ex) {
            ctx.status(400).json(Map.of("message", "Error: bad request"));
        }
        catch(UnauthorizedException ex) {
            ctx.status(401).json(Map.of("message", "user already exists"));
        }
    }

    public void logout(Context ctx) {
        try {
            String token = ctx.header("authorization");
            if (token != null && token.isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            LogoutRequest request = serializer.fromJson(token, LogoutRequest.class);
            userService.logout(request);
            ctx.status(200).json(Map.of());

        }
        catch(UnauthorizedException ex) {
            ctx.status(401).json(Map.of("message", "user already exists"));
        }
    }
}
