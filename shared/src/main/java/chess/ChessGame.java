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

        this.blackKing = board.getPiece(new ChessPosition(8,5));
        this.whiteKing = board.getPiece(new ChessPosition(1,5));
        this.turnColor = TeamColor.WHITE;
    }

    public static void main(String[] args){
        ChessGame game = new ChessGame();

        var moves = game.validMoves(new ChessPosition(2,3));
        System.out.println("Moves: " + moves);

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

    private static class MoveSnapshot {
        ChessPosition start, end;
        ChessPiece targetBeforeMove;
        ChessPiece movingPiece;
        boolean promoted;
        ChessPiece.PieceType promotedTo;
    }

    private MoveSnapshot simulateMove(ChessMove move) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        ChessPiece mover = board.getPiece(start);
        if (mover == null) return null;


        MoveSnapshot snap = new MoveSnapshot();
        snap.start = start;
        snap.end = end;
        snap.targetBeforeMove = board.getPiece(end);
        snap.movingPiece = mover;

        board.movePiece(start, end, board);


        if (mover.getPieceType() == ChessPiece.PieceType.KING) {
            if (mover.getTeamColor() == TeamColor.WHITE) whiteKing = mover;
            else blackKing = mover;
        }


        // Handle promotion
        ChessPiece moved = board.getPiece(end);
        boolean reachedBackRank = (end.getRow() == 8 || end.getRow() == 1);

        if (move.getPromotionPiece() != null) {
            var to = move.getPromotionPiece();
            if (to == ChessPiece.PieceType.KING || to == ChessPiece.PieceType.PAWN) {
                throw new RuntimeException("Invalid promotion piece");
            }
        }

        if (moved != null && moved.getPieceType() == ChessPiece.PieceType.PAWN) {
            if (reachedBackRank) {
                if (move.getPromotionPiece() == null) {
                    throw new RuntimeException("Pawn must promote");
                }
                board.addPiece(end, new ChessPiece(moved.getTeamColor(), move.getPromotionPiece(), end));
                snap.promoted = true;
                snap.promotedTo = move.getPromotionPiece();
            }
        } else if (move.getPromotionPiece() != null) {
            throw new RuntimeException("Can't promote at that row");
        }

        return snap;
    }


    private void undoSimulatedMove(MoveSnapshot snap) {
        if (snap == null) return;

        if (snap.promoted) {
            board.addPiece(snap.end, snap.movingPiece);
        }

        board.movePiece(snap.end, snap.start, board);

        if (snap.targetBeforeMove != null) {
            board.addPiece(snap.end, snap.targetBeforeMove);
        }
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
     *
     */

    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece currentPiece = board.getPiece(startPosition);
        if (currentPiece == null) return null;

        HashSet<ChessMove> validMoves = new HashSet<>(currentPiece.pieceMoves(board, startPosition));
        if (validMoves.isEmpty()) return validMoves;

        final TeamColor currentTeamColor = currentPiece.getTeamColor();

        Iterator<ChessMove> it = validMoves.iterator();
        while (it.hasNext()) {
            ChessMove move = it.next();
            MoveSnapshot snap = null;
            boolean illegal = false;

            try {
                snap = simulateMove(move);
                if (snap == null) {
                    illegal = true;
                } else if (isInCheck(currentTeamColor)) {
                    illegal = true;
                }
            } catch (IllegalArgumentException e) {
                illegal = true;
            } finally {
                if (snap != null) {
                    undoSimulatedMove(snap);
                }
            }

            if (illegal) it.remove();
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


    private ChessPosition kingPosition(TeamColor color) {
        return (color == TeamColor.WHITE) ? whiteKing.getPiecePosition() : blackKing.getPiecePosition();
    }

    private Collection<ChessMove> getOpposingMoves(TeamColor currentTurnColor) {
        HashSet<ChessMove> opposingMoves = new HashSet<>();
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition position = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(position);
                if (piece.getTeamColor() == currentTurnColor) {
                    continue;
                } else {
                    HashSet<ChessMove> move = new HashSet<>(piece.pieceMoves(board, position));
                    opposingMoves.addAll(move);
                }
            }
        }
        return opposingMoves;
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
        return board;
    }
}
