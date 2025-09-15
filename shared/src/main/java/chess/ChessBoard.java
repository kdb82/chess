package chess;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {

    final private ChessPiece[][] board = new ChessPiece[8][8];
    public ChessBoard() {
    }

    public static boolean inBounds(int row, int col) {
        boolean success = (row >= 1 && row <= 8) && (col >= 1 && col <= 8);
//        if (!success) {
//            System.out.print("tried to get piece out of bounds");
//        }
        return success;
    }


    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        final int r = position.getRow();
        final int c = position.getColumn();
        if (!inBounds(r,c)) {
            throw new IllegalArgumentException("Out of bounds: row=" + r + "col=" + c);
        }
        board[r- 1][c - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        final int r = position.getRow();
        final int c = position.getColumn();
        if (!inBounds(r, c)) {
            throw new IndexOutOfBoundsException("Piece out of bounds at (" + r + ", " + c + ")");
        }
        return board[r- 1][c - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                addPiece(new ChessPosition(r, c), null);
            }
        }

        final ChessPiece.PieceType[] BackRow = {
                ChessPiece.PieceType.ROOK,
                ChessPiece.PieceType.KNIGHT,
                ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.QUEEN,
                ChessPiece.PieceType.KING,
                ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.KNIGHT,
                ChessPiece.PieceType.ROOK
        };

        // add white pieces
        for (int c = 1; c <= 8; c++){
            addPiece(new ChessPosition(1, c), new ChessPiece(ChessGame.TeamColor.WHITE, BackRow[c-1]));
            addPiece(new ChessPosition(2, c), new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN));
        }

        // add black pieces
        for (int c = 1; c <= 8; c++) {
            addPiece(new ChessPosition(8, c), new ChessPiece(ChessGame.TeamColor.BLACK, BackRow[c-1]));
            addPiece(new ChessPosition(7, c), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN));
        }
    }
}
