/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 */
package game.cotuong.server.rule;

import bitzero.util.common.business.Debug;
import game.cotuong.server.rule.Board;
import game.cotuong.server.rule.GameResult;
import game.cotuong.server.rule.Piece;
import game.cotuong.server.rule.Rules;
import game.cotuong.server.rule.ai.AlphaBetaNode;
import game.cotuong.server.rule.ai.Move;
import game.cotuong.server.rule.ai.SearchModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameController {
    public Rules rule = new Rules();

    public static void main(String[] args) {
        GameController controller = new GameController();
        Board board = new Board();
        controller.initBoard(board);
        controller.printBoard(board);
    }

    private Map<String, Piece> initPieces(Board board) {
        Map<String, Piece> pieces = board.pieces;
        pieces.put("bx0", new Piece("bx0", new int[]{0, 0}));
        pieces.put("bm0", new Piece("bm0", new int[]{0, 1}));
        pieces.put("bt0", new Piece("bt0", new int[]{0, 2}));
        pieces.put("bs0", new Piece("bs0", new int[]{0, 3}));
        pieces.put("bg0", new Piece("bg0", new int[]{0, 4}));
        pieces.put("bs1", new Piece("bs1", new int[]{0, 5}));
        pieces.put("bt1", new Piece("bt1", new int[]{0, 6}));
        pieces.put("bm1", new Piece("bm1", new int[]{0, 7}));
        pieces.put("bx1", new Piece("bx1", new int[]{0, 8}));
        pieces.put("bp0", new Piece("bp0", new int[]{2, 1}));
        pieces.put("bp1", new Piece("bp1", new int[]{2, 7}));
        pieces.put("bz0", new Piece("bz0", new int[]{3, 0}));
        pieces.put("bz1", new Piece("bz1", new int[]{3, 2}));
        pieces.put("bz2", new Piece("bz2", new int[]{3, 4}));
        pieces.put("bz3", new Piece("bz3", new int[]{3, 6}));
        pieces.put("bz4", new Piece("bz4", new int[]{3, 8}));
        pieces.put("rx0", new Piece("rx0", new int[]{9, 0}));
        pieces.put("rm0", new Piece("rm0", new int[]{9, 1}));
        pieces.put("rt0", new Piece("rt0", new int[]{9, 2}));
        pieces.put("rs0", new Piece("rs0", new int[]{9, 3}));
        pieces.put("rg0", new Piece("rg0", new int[]{9, 4}));
        pieces.put("rs1", new Piece("rs1", new int[]{9, 5}));
        pieces.put("rt1", new Piece("rt1", new int[]{9, 6}));
        pieces.put("rm1", new Piece("rm1", new int[]{9, 7}));
        pieces.put("rx1", new Piece("rx1", new int[]{9, 8}));
        pieces.put("rp0", new Piece("rp0", new int[]{7, 1}));
        pieces.put("rp1", new Piece("rp1", new int[]{7, 7}));
        pieces.put("rz0", new Piece("rz0", new int[]{6, 0}));
        pieces.put("rz1", new Piece("rz1", new int[]{6, 2}));
        pieces.put("rz2", new Piece("rz2", new int[]{6, 4}));
        pieces.put("rz3", new Piece("rz3", new int[]{6, 6}));
        pieces.put("rz4", new Piece("rz4", new int[]{6, 8}));
        return pieces;
    }

    public void initBoard(Board board) {
        this.initPieces(board);
        for (Map.Entry<String, Piece> stringPieceEntry : board.pieces.entrySet()) {
            board.update(stringPieceEntry.getValue());
        }
    }

    public Move moveChess(int[] from, int[] to, Board board) {
        Move move = new Move();
        Piece p = board.getPiece(from);
        if (p != null) {
            move.piece = p;
            move.from = p.position;
            ArrayList<int[]> canMoves = this.rule.getNextMove(p.key, from, board);
            boolean valid = false;
            for (int[] m : canMoves) {
                if (m[0] != to[0] || m[1] != to[1]) continue;
                valid = true;
                break;
            }
            if (p.color != board.player) {
                valid = false;
            }
            if (valid) {
                boolean state;
                move.to = to;
                Debug.trace((Object[])new Object[]{"board player before: ", Character.valueOf(board.player)});
                move.eatenPiece = board.updatePiece(p.key, to);
                Debug.trace((Object[])new Object[]{"board player later: ", Character.valueOf(board.player)});
                int check = this.checkMove(board, move);
                boolean chieuCuNhay = false;
                if (check != 0) {
                    if (check == 6) {
                        move.result.result = GameResult.Name.DRAW;
                        return move;
                    }
                    if (check == 5) {
                        chieuCuNhay = true;
                    }
                }
                boolean bl = state = this.checkValidState(board) && !chieuCuNhay;
                if (!state) {
                    board.revert(move);
                    move.result.result = GameResult.Name.ERROR_STATE;
                } else {
                    boolean endGame = this.checkGameEnd(board);
                    move.result.result = endGame ? GameResult.Name.WIN_LOST : GameResult.Name.CONTINUE;
                    board.moveList.add(move);
                }
            } else {
                move.to = to;
                move.result.result = GameResult.Name.ERROR_INVALID;
            }
        } else {
            move.from = from;
            move.to = to;
            move.result.result = GameResult.Name.ERROR_UNEXIST;
        }
        return move;
    }

    public boolean checkValidState(Board board) {
        String general = "rg0";
        if (board.player == 'r') {
            general = "bg0";
        }
        ArrayList<Piece> halfPieces = new ArrayList<Piece>();
        for (Map.Entry<String, Piece> entry : board.pieces.entrySet()) {
            Piece piece = entry.getValue();
            if (piece.key.charAt(0) != board.player) continue;
            halfPieces.add(piece);
        }
        Piece enemyGeneral = board.pieces.get(general);
        for (Piece p : halfPieces) {
            ArrayList<int[]> moves = this.rule.getNextMove(p.key, p.position, board);
            for (int[] m : moves) {
                if (m[0] != enemyGeneral.position[0] || m[1] != enemyGeneral.position[1]) continue;
                return false;
            }
        }
        return true;
    }

    public int checkMove(Board board, Move move) {
        if (move.eatenPiece == null) {
            ++board.stateCount;
            if (board.stateCount >= 60) {
                return 6;
            }
        } else {
            board.stateCount = 0;
        }
        char key = 'b';
        String general = "rg0";
        if (board.player == 'b') {
            key = 'r';
            general = "bg0";
        }
        ArrayList<Piece> halfPieces = new ArrayList<Piece>();
        for (Map.Entry<String, Piece> entry : board.pieces.entrySet()) {
            Piece piece = entry.getValue();
            if (piece.key.charAt(0) != key) continue;
            halfPieces.add(piece);
        }
        Piece enemyGeneral = board.pieces.get(general);
        boolean chieuTuong = false;
        Piece chieuPiece = null;
        for (Piece p : halfPieces) {
            ArrayList<int[]> moves = this.rule.getNextMove(p.key, p.position, board);
            for (int[] m : moves) {
                if (m[0] != enemyGeneral.position[0] || m[1] != enemyGeneral.position[1]) continue;
                chieuTuong = true;
                chieuPiece = p;
                break;
            }
            if (!chieuTuong) continue;
            break;
        }
        if (chieuTuong) {
            Move m = board.getPreviousMove(move);
            if (move.piece.color == 'b') {
                board.blackMate = m == null || m.piece.key.equalsIgnoreCase(chieuPiece.key) ? ++board.blackMate : 0;
            } else {
                board.redMate = m == null || m.piece.key.equalsIgnoreCase(chieuPiece.key) ? ++board.redMate : 0;
            }
            if (board.blackMate > 3 || board.redMate > 3) {
                return 5;
            }
        } else if (move.piece.color == 'r') {
            board.redMate = 0;
        } else {
            board.blackMate = 0;
        }
        return 0;
    }

    public boolean checkGameEnd(Board board) {
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 9; ++j) {
                Piece p = board.cells[i][j];
                if (p == null || p.color != board.player) continue;
                int[] from = new int[]{i, j};
                ArrayList<int[]> moves = this.rule.getNextMove(p.key, from, board);
                for (int[] to : moves) {
                    Move move = new Move();
                    move.from = from;
                    move.to = to;
                    move.piece = p;
                    move.eatenPiece = board.updatePiece(p.key, to);
                    boolean state = this.checkValidState(board);
                    board.revert(move);
                    if (!state) continue;
                    return false;
                }
            }
        }
        return true;
    }

    public Move findBestMove(Board board) {
        Piece p;
        Move move = new Move();
        SearchModel searchModel = new SearchModel();
        AlphaBetaNode result = searchModel.search(board);
        move.piece = p = board.getPiece(result.piece);
        move.from = p.position;
        move.to = result.to;
        return move;
    }

    public void printBoard(Board board) {
        Debug.trace((Object[])new Object[]{"Player: ", Character.valueOf(board.player)});
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append("\n ");
            for (int j = 0; j < 9; ++j) {
                Piece p = board.getPiece(i, j);
                if (p != null) {
                    sb.append(p.color).append(p.character).append(" ");
                    continue;
                }
                sb.append(". ").append(" ");
            }
        }
        Debug.trace((Object[])new Object[]{"Board:\n", sb.toString()});
    }

    public char hasWin(Board board) {
        boolean isBlackWin;
        boolean isRedWin = board.pieces.get("bg0") == null;
        boolean bl = isBlackWin = board.pieces.get("rg0") == null;
        if (isRedWin) {
            return 'r';
        }
        if (isBlackWin) {
            return 'b';
        }
        return 'x';
    }
}

