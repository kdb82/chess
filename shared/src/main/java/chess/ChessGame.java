package chess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private TeamColor turnColor;
    private ChessBoard board;
    private ChessPiece blackKing;
    private ChessPiece whiteKing;

    public ChessGame() {
        this.board = new ChessBoard();
        board.resetBoard();

        ChessPiece blackKing = board.getPiece(new ChessPosition(8,5));
        this.blackKing = blackKing;
        System.out.println(blackKing.piecePosition);

        ChessPiece whiteKing = board.getPiece(new ChessPosition(1,5));
        this.whiteKing = whiteKing;

        this.turnColor = TeamColor.WHITE;
    }

    public static void main(String[] args){
        ChessGame game = new ChessGame();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return this.turnColor;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.turnColor = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     * <p>
     * Additionally, a move is valid if:
     * 1. The move falls within that piece's moves collection
     * 2. It doesn't leave your king in check.
     */

    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        int row = startPosition.getRow();
        int col = startPosition.getColumn();
        ChessPiece currentPiece = board.getPiece(startPosition);
        if (currentPiece == null || currentPiece.getTeamColor() != this.turnColor) {
            return new HashSet<>(); // no piece or not their turn
        }

        HashSet<ChessMove> validMoves = new HashSet<>(currentPiece.pieceMoves(board, startPosition));

        Iterator<ChessMove> it = validMoves.iterator();
        while (it.hasNext()) {
            ChessMove move = it.next();

        }



        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        throw new RuntimeException("Not implemented");
    }
}
