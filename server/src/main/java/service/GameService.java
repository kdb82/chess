package service;

import dataaccess.AuthDao;
import dataaccess.DataAccessException;
import dataaccess.GameDao;
import exceptions.AlreadyTakenException;
import exceptions.BadRequestException;
import exceptions.UnauthorizedException;
import model.AuthData;
import model.GameData;
import requests.CreateGameRequest;
import requests.JoinGameRequest;
import requests.ListGameRequest;
import results.CreateGameResult;
import results.GameSummary;
import results.ListGamesResult;
import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import serialization.GameStateDTO;
import serialization.GameStateMapper;


import java.util.ArrayList;
import java.util.List;

public class GameService {
    private final GameDao gameDao;
    private final AuthDao authDao;

    public GameService(GameDao gameDao, AuthDao authDao) {
        this.gameDao = gameDao;
        this.authDao = authDao;
    }

    public void joinGame(JoinGameRequest request, String authToken) throws DataAccessException,BadRequestException, UnauthorizedException {
        if (request == null) {
            throw new BadRequestException("Error: null request");
        }
        if (request.gameID() <= 0 || request.playerColor() == null) {
            throw new BadRequestException("Error: gameID and playerColor can't be null");
        }

        AuthData authData = authDao.getAuth(authToken);
        if (authData == null) {
            throw new UnauthorizedException("Error: Unauthorized");
        }
        String username = authData.username();

        GameData game =  gameDao.getGame(request.gameID());
        if (game == null) {throw new BadRequestException("Error: Game not found");}

        String playerColor = normalizeColor(request.playerColor());
        if (playerColor == null) {
            throw new BadRequestException("Error: null playerColor");
        }

        try {
            if (playerColor.equals("WHITE")) {
                String game_user = game.whiteUsername();
                if (game_user == null) {
                    gameDao.updateGamePlayer(request.gameID(), playerColor, username);
                } else if (!game_user.equals(username)) {
                    throw new DataAccessException("Error: " + playerColor + " seat already taken by " + game_user);
                }
            } else if (playerColor.equals("BLACK")) {
                String game_user = game.blackUsername();
                if (game_user == null) {
                    gameDao.updateGamePlayer(request.gameID(), playerColor, username);
                }  else if (!game_user.equals(username)) {
                    throw new DataAccessException("Error: " + playerColor + " seat already taken by " + game_user);
                }
            }
        } catch (DataAccessException ex) {
            String m = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (m.contains("already taken") || m.contains("white already taken") || m.contains("black already taken")) {
                throw new AlreadyTakenException("Error: already taken");
            }
            if (m.contains("invalid color") || m.contains("not found")) {
                throw new BadRequestException("Error: bad request");
            }
            throw ex; // 500
        }

    }

    public CreateGameResult createGame(CreateGameRequest request, String authToken) throws DataAccessException, UnauthorizedException, BadRequestException {
        AuthData authData = authDao.getAuth(authToken);
        if (authData == null) {
            throw new UnauthorizedException("Error: Unauthorized");
        }

        if (request == null || request.gameName() == null || request.gameName().isBlank()) {
            throw new BadRequestException("Error: request is null");
        }

        int id = gameDao.createGame(request.gameName());

        return new CreateGameResult(id);
    }

    public ListGamesResult listGames(ListGameRequest request) throws DataAccessException {
        String token = request.authToken();

        AuthData authData = authDao.getAuth(token);
        if (authData == null || authData.authToken() == null || !authData.authToken().equals(token)) {
            throw new UnauthorizedException("Error: Invalid token");
        }

        var games = gameDao.listGames();
        List<GameSummary> gamesList = new ArrayList<>(games.size());
        for (GameData game : games) {
            gamesList.add(new GameSummary(game.gameID(), game.whiteUsername(), game.blackUsername(), game.gameName()));
        }

        return new ListGamesResult(gamesList);
    }

    private static String normalizeColor(String c) {
        if (c == null) return null;
        String t = c.trim().toUpperCase();
        return ("WHITE".equals(t) || "BLACK".equals(t)) ? t : null;
    }

    public record MoveResult(
            GameStateDTO gameState,
            String moveNotification,
            String statusNotification
    ) {}

    public MoveResult makeMove(String authToken, int gameId, ChessMove move)
            throws DataAccessException, UnauthorizedException, BadRequestException, InvalidMoveException {

        if (move == null) {
            throw new BadRequestException("Error: move is null");
        }

        AuthData auth = authDao.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("Error: Unauthorized");
        }
        String username = auth.username();

        GameData gameData = gameDao.getGame(gameId);
        if (gameData == null) {
            throw new BadRequestException("Error: Game not found");
        }

        ChessGame game = gameData.game();
        if (game == null) {
            game = gameDao.loadGameState(gameId);
        }

        ChessGame.TeamColor playerColor;
        if (username.equals(gameData.whiteUsername())) {
            playerColor = ChessGame.TeamColor.WHITE;
        } else if (username.equals(gameData.blackUsername())) {
            playerColor = ChessGame.TeamColor.BLACK;
        } else {
            throw new UnauthorizedException("Error: you are not a player in this game");
        }

        if (game.getTeamTurn() != playerColor) {
            throw new BadRequestException("Error: not your turn");
        }

        game.makeMove(move);

        gameDao.saveGameState(gameId, game);

        GameStateDTO dto = GameStateMapper.gameToDTO(game);

        String moveNotification = describeMove(username, move);

        ChessGame.TeamColor opponent =
                (playerColor == ChessGame.TeamColor.WHITE) ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
        String opponentUsername = switch (opponent) {
            case WHITE -> gameData.whiteUsername();
            case BLACK -> gameData.blackUsername();
        };

        String statusNotification = null;
        if (game.isInCheckmate(opponent)) {
            statusNotification = opponentUsername + " is in checkmate";
            String winner = (playerColor == ChessGame.TeamColor.WHITE) ? "WHITE" : "BLACK";
            gameDao.updateGameStatus(gameId, "FINISHED", winner);
        } else if (game.isInStalemate(opponent)) {
            statusNotification = "Stalemate";
            gameDao.updateGameStatus(gameId, "FINISHED", "DRAW");
        } else if (game.isInCheck(opponent)) {
            statusNotification = opponentUsername + " is in check";
        }

        return new MoveResult(dto, moveNotification, statusNotification);
    }

    private String describeMove(String username, ChessMove move) {
        var start = move.getStartPosition();
        var end = move.getEndPosition();
        String from = toChessSquareFormat(start);
        String to = toChessSquareFormat(end);

        String base = username + " moved from " + from + " to " + to;
        if (move.getPromotionPiece() != null) {
            base += " and promoted to " + move.getPromotionPiece();
        }
        return base;
    }

    private String toChessSquareFormat(chess.ChessPosition pos) {
        char file = (char) ('a' + (pos.getColumn() - 1));
        int rank  = pos.getRow();
        return "" + file + rank;
    }
    public void resignGame(int gameId, String username)
            throws DataAccessException, BadRequestException {

        if (username == null || username.isBlank()) {
            throw new BadRequestException("Error: username required");
        }

        GameData game = gameDao.getGame(gameId);
        if (game == null) {
            throw new BadRequestException("Error: Game not found");
        }

        String white = game.whiteUsername();
        String black = game.blackUsername();

        String result;
        if (username.equals(white)) {
            // white resigns → black wins
            result = "BLACK_WON_RESIGN";
        } else if (username.equals(black)) {
            // black resigns → white wins
            result = "WHITE_WON_RESIGN";
        } else {
            // observers can’t resign the game
            throw new BadRequestException("Error: Only a player can resign");
        }

        // mark game as over in DB
        gameDao.updateGameStatus(gameId, "OVER", result);
    }


    public void leaveGame(int gameId, String authToken)
            throws DataAccessException, UnauthorizedException, BadRequestException {

        AuthData auth = authDao.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("Error: Unauthorized");
        }
        String username = auth.username();

        GameData gameData = gameDao.getGame(gameId);
        if (gameData == null) {
            throw new BadRequestException("Error: Game not found");
        }

        String white = gameData.whiteUsername();
        String black = gameData.blackUsername();

        if (username.equals(white)) {
            gameDao.removePlayerSeat(gameId, "WHITE");
        } else if (username.equals(black)) {
            gameDao.removePlayerSeat(gameId, "BLACK");
        }
    }


}
