package serialization;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.ArrayList;

public class GameStateMapper {
    private GameStateMapper() {}

    public static GameStateDTO gameToDTO(ChessGame game) {
        var pieces =  new ArrayList<GamePieceDTO>();
        ChessBoard board = game.getBoard();

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition position = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(position);
                if (piece != null) {
                    pieces.add(new GamePieceDTO(piece.getPieceType(), piece.getTeamColor(), r, c));
                }
            }
        }
        return new GameStateDTO(game.getTeamTurn(), pieces);
    }

    public static ChessGame dtoToGame(GameStateDTO gameStateDTO) {
        ChessGame game = new ChessGame();
        ChessBoard board = new ChessBoard();

        for (var pieceDTO : gameStateDTO.gamePieces()) {
            var pos = new ChessPosition(pieceDTO.row(), pieceDTO.col());
            var piece = new ChessPiece(pieceDTO.color(), pieceDTO.type(), pos);
            board.addPiece(pos, piece);
            if (piece.getPieceType() == ChessPiece.PieceType.KING) {
                if (piece.getTeamColor() == ChessGame.TeamColor.WHITE) {
                    game.setWhiteKing(piece);
                } else { game.setBlackKing(piece); }
            }
        }
        game.setBoard(board);
        game.setTeamTurn(gameStateDTO.turn());
        return game;
    }

}
