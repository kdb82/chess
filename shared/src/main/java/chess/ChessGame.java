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

    public static void main(String[] args) throws InvalidMoveException {
        ChessGame game = new ChessGame();

        var moves = game.validMoves(new ChessPosition(2,3));
        System.out.println("Moves: " + moves);

        ChessMove move = new ChessMove(new ChessPosition(2,3), new ChessPosition(3,3), null);
        game.makeMove(move);
        ChessMove move2 = new ChessMove(new ChessPosition(7,6), new ChessPosition(6,6), null);
        game.makeMove(move2);
        ChessMove move3 = new ChessMove(new ChessPosition(1,4), new ChessPosition(3,2), null);
        game.makeMove(move3);
        String boardString = BoardPrinter.boardString(game.board);
        System.out.println(boardString);
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

        //Can't promote to King or Pawn
        if (move.getPromotionPiece() != null) {
            var promotedPiece = move.getPromotionPiece();
            if (promotedPiece == ChessPiece.PieceType.KING || promotedPiece == ChessPiece.PieceType.PAWN) {
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

        if (currentPiece == null) return java.util.Collections.emptySet();

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
        if (move == null) throw new InvalidMoveException("Move is null");
        ChessPosition start = move.getStartPosition();

        ChessPiece mover = board.getPiece(start);
        if (mover == null) throw new InvalidMoveException("No piece at start");

        if (mover.getTeamColor() != turnColor) {
            throw new InvalidMoveException("Not your turn");
        }

        //moves returns a hashset of all legal moves
        var legalMoves = validMoves(start);
        if (legalMoves.isEmpty() || !legalMoves.contains(move)) throw new InvalidMoveException("Not a valid move");


        simulateMove(move);
        turnColor = (turnColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        TeamColor opp = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        ChessPiece king = (teamColor == TeamColor.WHITE) ? whiteKing : blackKing;
        ChessPosition kingPos = king.getPiecePosition();

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = board.getPiece(pos);
                if (p == null || p.getTeamColor() != opp) continue;

                for (ChessMove m : p.pieceMoves(board, pos)) {
                    if (m.getEndPosition().equals(kingPos)) {
                        return true; //King in Check
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) return false;


        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece == null || piece.getTeamColor() != teamColor) continue;

                Collection<ChessMove> legal = validMoves(pos);
                if (!legal.isEmpty()) return false;
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheckmate(teamColor)) return false;

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece == null || piece.getTeamColor() != teamColor) continue;

                Collection<ChessMove> legal = validMoves(pos);
                if (!legal.isEmpty()) return false;
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;

        this.whiteKing = null;
        this.blackKing = null;

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(new ChessPosition(r, c));
                if (piece == null) continue;

                piece.setPiecePosition(pos);
                if (piece.getPieceType() == ChessPiece.PieceType.KING) {
                    if (piece.getTeamColor() == TeamColor.WHITE) { this.whiteKing = piece; }
                    else {this.blackKing = piece;}
                }
            }
        }
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
