package serialization;

import chess.ChessGame;

import java.util.List;

public record GameStateDTO(ChessGame.TeamColor turn, List<GamePieceDTO> gamePieces) {
}
