package serialization;

import chess.ChessGame;
import chess.ChessPiece;

public record GamePieceDTO(ChessPiece.PieceType type, ChessGame.TeamColor color, int row, int col) {
}
