package chess;

/**
 * Utility for pretty-printing a ChessBoard to ASCII.
 * Rows are shown top-to-bottom (8 down to 1).
 * Columns are shown left-to-right (1 through 8).
 */
public final class BoardPrinter {
    private BoardPrinter() {}

    /**
     * Returns a simple 8x8 text board. Uppercase = WHITE, lowercase = BLACK, '.' = empty.
     * Files are columns 1..8; ranks are rows 8..1 on subsequent lines.
     */
    public static String boardString(ChessBoard b) {
        StringBuilder sb = new StringBuilder();
        for (int r = 8; r >= 1; r--) {
            sb.append(r).append(" ");
            for (int c = 1; c <= 8; c++) {
                ChessPiece p = b.getPiece(new ChessPosition(r, c));
                sb.append(p == null ? '.' : symbol(p)).append(' ');
            }
            sb.append('\n');
        }
        sb.append("  1 2 3 4 5 6 7 8\n");
        return sb.toString();
    }

    private static char symbol(ChessPiece p) {
        char ch;
        switch (p.getPieceType()) {
            case KING -> ch = 'k';
            case QUEEN -> ch = 'q';
            case ROOK -> ch = 'r';
            case BISHOP -> ch = 'b';
            case KNIGHT -> ch = 'n';
            case PAWN -> ch = 'p';
            default -> ch = '?';
        }
        return p.getTeamColor() == ChessGame.TeamColor.WHITE
                ? Character.toUpperCase(ch)
                : ch;
    }
}
