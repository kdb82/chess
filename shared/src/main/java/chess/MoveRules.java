package chess;

import java.util.Collection;

public interface MoveRules {
    Collection<ChessMove> validMoves(ChessPosition startPosition);
    void makeMove (ChessMove move);
    class MoveSnapshot {}

    private MoveSnapshot simulateMove(ChessMove m) throws InvalidMoveException // for sim (returns snapshot)
    {
        return null;
    }

    void undoSimulatedMove(ChessBoard b, MoveSnapshot s);                                    // for sim undo
}
