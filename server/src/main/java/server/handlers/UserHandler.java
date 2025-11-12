package server.handlers;

import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.http.Context;
import requests.*;
import service.UserService;
import results.*;

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
            if (request == null
                    || request.username() == null || request.username().isBlank()
                    || request.password() == null || request.password().isBlank()
                    || request.email() == null    || request.email().isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            RegisterResult result = userService.register(request);
            ctx.status(200).json(result);
        }
        catch(AlreadyTakenException ex) {
            ctx.status(403).json(Map.of("message", "Error: username already taken"));
        } catch(BadRequestException ex) {
            ctx.status(400).json(Map.of("message", ex.getMessage()));
        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", e.getMessage()));
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
            if (request.password() == null || request.username() == null ||request.password().isBlank() || request.username().isBlank()) {
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
            ctx.status(401).json(Map.of("message", "Error: incorrect username or password or user nonexistent"));
        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", e.getMessage()));
        }
    }

    public void logout(Context ctx) {
        try {
            String token = ctx.header("authorization");
            if (token == null || token.isBlank()) {
                ctx.status(400).json(Map.of("message", "Error: bad request"));
                return;
            }

            LogoutRequest request = new  LogoutRequest(token);
            userService.logout(request);
            ctx.status(200).json(Map.of());

        }
        catch(UnauthorizedException ex) {
            ctx.status(401).json(Map.of("message", "Error: user already exists"));
        } catch (DataAccessException e) {
            ctx.status(500).json(Map.of("message", e.getMessage()));
        }
    }
}
