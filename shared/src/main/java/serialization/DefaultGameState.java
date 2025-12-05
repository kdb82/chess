package serialization;

import chess.ChessGame;

public final class DefaultGameState {
    private DefaultGameState() {}

    public static GameStateDTO loadDefaultGameState() {
        ChessGame game = new ChessGame();
        return GameStateMapper.gameToDTO(game);
    }
}
