package ui;

public final class DrawBoard {

    private DrawBoard() {}

    /** Draw the initial chess position from a chosen perspective. */
    public static void drawInitial(boolean whitePerspective) {
        System.out.print(EscapeSequences.ERASE_SCREEN);

        // Header files (a..h for white, h..a for black)
        printFilesHeader(whitePerspective);

        for (int rDisp = 0; rDisp < 8; rDisp++) {
            // Absolute rank (1..8) shown on the left/right borders
            int rank = whitePerspective ? (8 - rDisp) : (1 + rDisp);

            // Left rank label
            System.out.printf(" %d ", rank);

            for (int cDisp = 0; cDisp < 8; cDisp++) {
                // Absolute file number (1..8) and letter (a..h) for this printed cell
                int fileNum = whitePerspective ? (1 + cDisp) : (8 - cDisp);

                // Choose background color: light if (rank + fileNum) is odd (puts a8 & h1 light)
                boolean light = ((rank + fileNum) % 2 == 1);
                String bg = light ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY
                        : EscapeSequences.SET_BG_COLOR_DARK_GREY;

                // Piece on this absolute square in the STARTING position
                String glyph = pieceAtStart(rank, fileNum);

                // Paint square
                System.out.print(bg + glyph + EscapeSequences.RESET_BG_COLOR);
            }

            // Right rank label
            System.out.printf(" %d%n", rank);
        }

        // Footer files
        printFilesHeader(whitePerspective);
        System.out.flush();
    }

    // ---------- helpers ----------

    private static void printFilesHeader(boolean whitePerspective) {
        System.out.print("    ");
        for (int i = 0; i < 8; i++) {
            int fileNum = whitePerspective ? (1 + i) : (8 - i);
            char fileLetter = (char) ('a' + (fileNum - 1));
            System.out.print(" " + fileLetter + "  ");
        }
        System.out.println();
    }

    /** Unicode piece to place at (rank 1..8, file 1..8) in the initial position. */
    private static String pieceAtStart(int rank, int fileNum) {
        // Pawns
        if (rank == 2) return EscapeSequences.WHITE_PAWN;
        if (rank == 7) return EscapeSequences.BLACK_PAWN;

        // Back ranks
        if (rank == 1) return whiteBackRank(fileNum);
        if (rank == 8) return blackBackRank(fileNum);

        return EscapeSequences.EMPTY;
    }

    private static String whiteBackRank(int f) {
        return switch (f) {
            case 1, 8 -> EscapeSequences.WHITE_ROOK;
            case 2, 7 -> EscapeSequences.WHITE_KNIGHT;
            case 3, 6 -> EscapeSequences.WHITE_BISHOP;
            case 4     -> EscapeSequences.WHITE_QUEEN;  // queen on her color
            case 5     -> EscapeSequences.WHITE_KING;
            default    -> EscapeSequences.EMPTY;
        };
    }

    private static String blackBackRank(int f) {
        return switch (f) {
            case 1, 8 -> EscapeSequences.BLACK_ROOK;
            case 2, 7 -> EscapeSequences.BLACK_KNIGHT;
            case 3, 6 -> EscapeSequences.BLACK_BISHOP;
            case 4     -> EscapeSequences.BLACK_QUEEN;
            case 5     -> EscapeSequences.BLACK_KING;
            default    -> EscapeSequences.EMPTY;
        };
    }
}
