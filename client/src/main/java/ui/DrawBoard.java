package ui;

import chess.ChessGame;
import chess.ChessPiece;
import serialization.GamePieceDTO;
import serialization.GameStateDTO;

public final class DrawBoard {

    public static void drawInitial(boolean drawWhiteSide) {
        System.out.print(EscapeSequences.ERASE_SCREEN);

        printLetters(drawWhiteSide);
        for(int r = 0; r < 8; r++) {
            int rowNum = drawWhiteSide ? 8 - r : 1 + r;
            System.out.printf(" %d ", rowNum);

            for(int c = 0; c < 8; c++) {
                int colNum = drawWhiteSide ? 1 + c : 8 - c;
                boolean light = ((rowNum + colNum) % 2 == 1);

                String bg = light ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY : EscapeSequences.SET_BG_COLOR_DARK_GREY;
                String gamePiece = getGamePiece(rowNum, colNum);

                System.out.print(bg + gamePiece + EscapeSequences.RESET_BG_COLOR);
            }
            System.out.printf(" %d\n", rowNum);
        }
        printLetters(drawWhiteSide);
        System.out.flush();
    }

    private static String getGamePiece(int rowNum, int colNum) {
        return switch (rowNum) {
            case 1 -> getBackRowPiece(true, colNum);
            case 8 -> getBackRowPiece(false, colNum);
            case 2 -> EscapeSequences.WHITE_PAWN;
            case 7 -> EscapeSequences.BLACK_PAWN;
            default -> EscapeSequences.EMPTY;
        };
    }

    private static String getBackRowPiece(boolean isWhite, int colNum) {
        if (isWhite) {
            switch (colNum) {
                case 1, 8: return EscapeSequences.WHITE_ROOK;
                case 2, 7: return EscapeSequences.WHITE_KNIGHT;
                case 3, 6: return EscapeSequences.WHITE_BISHOP;
                case 4: return EscapeSequences.WHITE_QUEEN;
                case 5: return EscapeSequences.WHITE_KING;
            }
        } else {
            switch (colNum) {
                case 1, 8: return EscapeSequences.BLACK_ROOK;
                case 2, 7: return EscapeSequences.BLACK_KNIGHT;
                case 3, 6: return EscapeSequences.BLACK_BISHOP;
                case 4: return EscapeSequences.BLACK_QUEEN;
                case 5: return EscapeSequences.BLACK_KING;
            }
        }
        return EscapeSequences.EMPTY;
    }

    private static void printLetters(boolean drawWhiteSide) {
        System.out.print("   ");
        for (int i = 0; i < 8; i++) {
            char letter = (char) (drawWhiteSide ? 'a' + i : 'a' + (7-i));
            System.out.print(" " + letter + " ");
        }
        System.out.println();
    }

    public static void redraw(GameStateDTO state, boolean drawWhiteSide) {
        GamePieceDTO[][] board = new GamePieceDTO[9][9];
        for (GamePieceDTO piece : state.gamePieces()) {
            board[piece.row()][piece.col()] = piece;
        }

        System.out.print(EscapeSequences.ERASE_SCREEN);

        System.out.println("Turn: " + state.turn());
        printLetters(drawWhiteSide);

        for(int r = 0; r < 8; r++) {
            int rowNum = drawWhiteSide ? 8 - r : 1 + r;
            System.out.printf(" %d ", rowNum);

            for(int c = 0; c < 8; c++) {
                int colNum = drawWhiteSide ? 1 + c : 8 - c;
                boolean light = ((rowNum + colNum) % 2 == 1);

                String bg = light
                        ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY
                        : EscapeSequences.SET_BG_COLOR_DARK_GREY;

                GamePieceDTO piece = board[rowNum][colNum];
                String glyph = findSymbol(piece);

                System.out.print(bg + glyph + EscapeSequences.RESET_BG_COLOR);
            }
            System.out.printf(" %d ", rowNum);
        }
        printLetters(drawWhiteSide);
        System.out.flush();
    }

    private static String findSymbol(GamePieceDTO piece) {
        if (piece == null) {
            return EscapeSequences.EMPTY;
        }

        ChessPiece.PieceType type = piece.type();
        ChessGame.TeamColor color = piece.color();

        return switch (color) {
            case WHITE -> switch (type) {
                case KING -> EscapeSequences.WHITE_KING;
                case QUEEN -> EscapeSequences.WHITE_QUEEN;
                case ROOK -> EscapeSequences.WHITE_ROOK;
                case BISHOP -> EscapeSequences.WHITE_BISHOP;
                case KNIGHT -> EscapeSequences.WHITE_KNIGHT;
                case PAWN -> EscapeSequences.WHITE_PAWN;
            };
            case BLACK -> switch (type) {
                case KING -> EscapeSequences.BLACK_KING;
                case QUEEN -> EscapeSequences.BLACK_QUEEN;
                case ROOK -> EscapeSequences.BLACK_ROOK;
                case BISHOP -> EscapeSequences.BLACK_BISHOP;
                case KNIGHT -> EscapeSequences.BLACK_KNIGHT;
                case PAWN -> EscapeSequences.BLACK_PAWN;
            };
        };
    }
}
