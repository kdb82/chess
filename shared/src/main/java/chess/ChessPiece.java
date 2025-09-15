package chess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    //helper to make new chess position objects
    private ChessPosition cpos(int row, int col) {
        return new ChessPosition(row, col);
    }

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    private void addPawnMove(HashSet<ChessMove> moves, ChessPosition start, int endrow, int endcol, boolean promote) {
        ChessPosition end = new ChessPosition(endrow, endcol);
        if (!promote) {
            moves.add(new ChessMove(start, end, null));
        } else {
            moves.add(new ChessMove(start, end, PieceType.QUEEN));
            moves.add(new ChessMove(start, end, PieceType.ROOK));
            moves.add(new ChessMove(start, end, PieceType.BISHOP));
            moves.add(new ChessMove(start, end, PieceType.KNIGHT));
        }
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        var moves = new HashSet<ChessMove>();

        switch (this.type) {
            case ROOK:
                break;
            case KNIGHT:
                break;
            case BISHOP:
                break;
            case QUEEN:
                break;
            case KING:
                break;
            case PAWN:
                final int row = myPosition.getRow();
                final int col = myPosition.getColumn();
                final boolean white = (this.pieceColor == ChessGame.TeamColor.WHITE);
                final int direction = white ? +1 : -1;
                final int start_row = white ? 2 : 7;
                final int promo_row = white ? 8 : 1;

                int row_move = row + direction;

                // single forward: check if in bounds and endposition is empty
                if(ChessBoard.inBounds(row_move, col) && board.getPiece(cpos(row_move, col)) == null) {
                    boolean promote = (row_move == promo_row);
                    addPawnMove(moves, myPosition, row_move, col, promote);

                    //check if on starting row,
                    if (row == start_row) {
                        int row_move2 = row + 2 * direction;
                        if (ChessBoard.inBounds(row_move2, col) && board.getPiece(cpos(row_move2, col)) == null && board.getPiece(cpos(row_move,col)) == null) {
                            addPawnMove(moves, myPosition, row_move2, col, false);
                        }
                    }
                }
            break;
        }
        return moves;
    }



    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }
}
