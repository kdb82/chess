package chess;

import org.junit.jupiter.api.Test;

import java.util.*;

public class MyTests {

    @Test
    void printInitialBoard() {
        ChessBoard board = new ChessBoard();
        board.resetBoard();
        System.out.println(BoardPrinter.boardString(board));
    }

    //place your pieces using test and generate valid moves to check move logic
    @Test
    void pawnMovestTester() {
        ChessBoard board = new ChessBoard();

        // put a white pawn at e2 (row 2, col 5)
        board.addPiece(new ChessPosition(7,3), new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN));
        ChessPosition start = new ChessPosition(4, 6);
        ChessPiece queen = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.QUEEN);
        board.addPiece(start, queen);

        System.out.println(BoardPrinter.boardString(board));
        // get all moves for the pawn
        Collection<ChessMove> moves = queen.pieceMoves(board, start);

        List<ChessMove> moveList = new ArrayList<>(moves);
        moveList.sort(Comparator
                .comparingInt((ChessMove m) -> m.getEndPosition().getRow())
                .thenComparingInt(m -> m.getEndPosition().getColumn()));
        // print them
            for (ChessMove move : moveList) {
                System.out.println(move);
        }
        System.out.println("there are " + moves.size() + " moves");
    }
}

