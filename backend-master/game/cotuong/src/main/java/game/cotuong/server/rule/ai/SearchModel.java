/*
 * Decompiled with CFR 0.144.
 */
package game.cotuong.server.rule.ai;

import game.cotuong.server.rule.Board;
import game.cotuong.server.rule.GameController;
import game.cotuong.server.rule.Piece;
import game.cotuong.server.rule.Rules;
import game.cotuong.server.rule.ai.AlphaBetaNode;
import game.cotuong.server.rule.ai.EvalModel;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class SearchModel {
    private static int DEPTH = 3;
    private Board board;
    private GameController controller = new GameController();

    public AlphaBetaNode search(Board board) {
        this.board = board;
        if (board.pieces.size() < 28) {
            DEPTH = 3;
        }
        if (board.pieces.size() < 16) {
            DEPTH = 4;
        }
        if (board.pieces.size() < 6) {
            DEPTH = 5;
        }
        if (board.pieces.size() < 4) {
            DEPTH = 6;
        }
        long startTime = System.currentTimeMillis();
        AlphaBetaNode best = null;
        boolean isMax = board.player == 'b';
        ArrayList<AlphaBetaNode> moves = this.generateMovesForAll(isMax);
        for (AlphaBetaNode n : moves) {
            Piece eaten = board.updatePiece(n.piece, n.to);
            n.value = this.alphaBeta(DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            if (best == null || n.value >= best.value) {
                best = n;
            }
            board.updatePiece(n.piece, n.from);
            if (eaten == null) continue;
            board.pieces.put(eaten.key, eaten);
            board.backPiece(eaten.key);
        }
        long finishTime = System.currentTimeMillis();
        System.out.println(finishTime - startTime);
        return best;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private int alphaBeta(int depth, int alpha, int beta, boolean isMax) {
        if (depth == 0 || this.controller.hasWin(this.board) != 'x') {
            return new EvalModel().eval(this.board, 'b');
        }
        ArrayList<AlphaBetaNode> moves = this.generateMovesForAll(isMax);
        SearchModel searchModel = this;
        synchronized (searchModel) {
            for (AlphaBetaNode n : moves) {
                Piece eaten = this.board.updatePiece(n.piece, n.to);
                final int finalBeta = beta;
                final int finalAlpha = alpha;
                final int finalDepth = depth;
                final int[] temp = new int[1];
                if (depth == 2) {
                    if (isMax) {
                        new Thread(new Runnable(){

                            @Override
                            public void run() {
                                temp[0] = Math.max(finalAlpha, SearchModel.this.alphaBeta(finalDepth - 1, finalAlpha, finalBeta, false));
                            }
                        }).run();
                        alpha = temp[0];
                    } else {
                        new Thread(new Runnable(){

                            @Override
                            public void run() {
                                temp[0] = Math.min(finalBeta, SearchModel.this.alphaBeta(finalDepth - 1, finalAlpha, finalBeta, true));
                            }
                        }).run();
                        beta = temp[0];
                    }
                } else if (isMax) {
                    alpha = Math.max(alpha, this.alphaBeta(depth - 1, alpha, beta, false));
                } else {
                    beta = Math.min(beta, this.alphaBeta(depth - 1, alpha, beta, true));
                }
                this.board.updatePiece(n.piece, n.from);
                if (eaten != null) {
                    this.board.pieces.put(eaten.key, eaten);
                    this.board.backPiece(eaten.key);
                }
                if (beta > alpha) continue;
                break;
            }
        }
        return isMax ? alpha : beta;
    }

    private ArrayList<AlphaBetaNode> generateMovesForAll(boolean isMax) {
        ArrayList<AlphaBetaNode> moves = new ArrayList<AlphaBetaNode>();
        Rules rule = new Rules();
        for (Map.Entry<String, Piece> stringPieceEntry : this.board.pieces.entrySet()) {
            Piece piece = stringPieceEntry.getValue();
            if (isMax && piece.color == 'r' || !isMax && piece.color == 'b') continue;
            for (int[] nxt : rule.getNextMove(piece.key, piece.position, this.board)) {
                moves.add(new AlphaBetaNode(piece.key, piece.position, nxt));
            }
        }
        return moves;
    }

}

