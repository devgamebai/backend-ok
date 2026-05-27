package game.cotuong.server.rule;

import java.util.ArrayList;
import java.util.Map;

public class Rules
{
    private int[] pos;
    private Board board;
    private char player;

    public Rules() {}

    public ArrayList<int[]> getNextMove(String piece, int[] pos, Board board)
    {
        this.pos = pos;
        this.board = board;
        player = piece.charAt(0);
        switch (piece.charAt(1)) {
            case 'x':
                return xeRules();
            case 'm':
                return maRules();
            case 'p':
                return phaoRules();
            case 't':
                return voiRules();
            case 's':
                return syRules();
            case 'g':
                return generalRules();
            case 'z':
                return totRules();
        }
        return null;
    }


    private ArrayList<int[]> maRules()
    {
        ArrayList<int[]> moves = new ArrayList();
        int[][] target = { { 1, -2 }, { 2, -1 }, { 2, 1 }, { 1, 2 }, { -1, 2 }, { -2, 1 }, { -2, -1 }, { -1, -2 } };
        int[][] obstacle = { { 0, -1 }, { 1, 0 }, { 1, 0 }, { 0, 1 }, { 0, 1 }, { -1, 0 }, { -1, 0 }, { 0, -1 } };
        for (int i = 0; i < target.length; i++) {
            int[] e = { pos[0] + target[i][0], pos[1] + target[i][1] };
            int[] f = { pos[0] + obstacle[i][0], pos[1] + obstacle[i][1] };
            if ((board.isInside(e)) &&
                    (board.isEmpty(f))) {
                if (board.isEmpty(e)) { moves.add(e);
                } else if (board.getPiece(e).color != player) moves.add(e);
            }
        }
        return moves;
    }

    private ArrayList<int[]> xeRules() {
        ArrayList<int[]> moves = new ArrayList();
        int[] yOffsets = { 1, 2, 3, 4, 5, 6, 7, 8 };
        int[] xOffsets = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        for (int offset : yOffsets) {
            int[] rMove = { pos[0], pos[1] + offset };
            if (board.isEmpty(rMove)) { moves.add(rMove);
            } else { if ((!board.isInside(rMove)) || (board.getPiece(rMove).color == player)) break;
                moves.add(rMove);
                break;
            }
        }
        for (int offset : yOffsets) {
            int[] lMove = { pos[0], pos[1] - offset };
            if (board.isEmpty(lMove)) { moves.add(lMove);
            } else { if ((!board.isInside(lMove)) || (board.getPiece(lMove).color == player)) break;
                moves.add(lMove);
                break;
            }
        }
        for (int offset : xOffsets) {
            int[] uMove = { pos[0] - offset, pos[1] };
            if (board.isEmpty(uMove)) { moves.add(uMove);
            } else { if ((!board.isInside(uMove)) || (board.getPiece(uMove).color == player)) break;
                moves.add(uMove);
                break;
            }
        }
        for (int offset : xOffsets) {
            int[] dMove = { pos[0] + offset, pos[1] };
            if (board.isEmpty(dMove)) { moves.add(dMove);
            } else { if ((!board.isInside(dMove)) || (board.getPiece(dMove).color == player)) break;
                moves.add(dMove);
                break;
            }
        }
        return moves;
    }

    private ArrayList<int[]> phaoRules() {
        ArrayList<int[]> moves = new ArrayList();
        int[] yOffsets = { 1, 2, 3, 4, 5, 6, 7, 8 };
        int[] xOffsets = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        boolean rr = false;boolean ll = false;boolean uu = false;boolean dd = false;
        for (int offset : yOffsets) {
            int[] rMove = { pos[0], pos[1] + offset };
            if (!board.isInside(rMove)) break;
            boolean e = board.isEmpty(rMove);
            if (!rr) {
                if (e) moves.add(rMove); else
                    rr = true;
            } else if (!e) {
                if (board.getPiece(rMove).color == player) break; moves.add(rMove); break;
            }
        }

        for (int offset : yOffsets) {
            int[] lMove = { pos[0], pos[1] - offset };
            if (!board.isInside(lMove)) break;
            boolean e = board.isEmpty(lMove);
            if (!ll) {
                if (e) moves.add(lMove); else
                    ll = true;
            } else if (!e) {
                if (board.getPiece(lMove).color == player) break;
                moves.add(lMove); break;
            }
        }


        for (int offset : xOffsets) {
            int[] uMove = { pos[0] - offset, pos[1] };
            if (!board.isInside(uMove)) break;
            boolean e = board.isEmpty(uMove);
            if (!uu) {
                if (e) moves.add(uMove); else
                    uu = true;
            } else if (!e) {
                if (board.getPiece(uMove).color == player) break; moves.add(uMove); break;
            }
        }

        for (int offset : xOffsets) {
            int[] dMove = { pos[0] + offset, pos[1] };
            if (!board.isInside(dMove)) break;
            boolean e = board.isEmpty(dMove);
            if (!dd) {
                if (e) moves.add(dMove); else
                    dd = true;
            } else if (!e) {
                if (board.getPiece(dMove).color == player) break; moves.add(dMove); break;
            }
        }

        return moves;
    }

    private ArrayList<int[]> voiRules() {
        ArrayList<int[]> moves = new ArrayList();
        int[][] target = { { -2, -2 }, { 2, -2 }, { -2, 2 }, { 2, 2 } };
        int[][] obstacle = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        for (int i = 0; i < target.length; i++) {
            int[] e = { pos[0] + target[i][0], pos[1] + target[i][1] };
            int[] f = { pos[0] + obstacle[i][0], pos[1] + obstacle[i][1] };
            if ((board.isInside(e)) && ((e[0] <= 4) || (player != 'b')) && ((e[0] >= 5) || (player != 'r')) &&
                    (board.isEmpty(f))) {
                if (board.isEmpty(e)) { moves.add(e);
                } else if (board.getPiece(e).color != player) moves.add(e);
            }
        }
        return moves;
    }

    private ArrayList<int[]> syRules() {
        ArrayList<int[]> moves = new ArrayList();
        int[][] target = { { -1, -1 }, { 1, 1 }, { -1, 1 }, { 1, -1 } };
        for (int[] aTarget : target) {
            int[] e = { pos[0] + aTarget[0], pos[1] + aTarget[1] };
            if ((board.isInside(e)) && (((e[0] <= 2) && (e[1] >= 3) && (e[1] <= 5)) || ((player != 'b') && (((e[0] >= 7) && (e[1] >= 3) && (e[1] <= 5)) || (player != 'r')))))
            {
                if (board.isEmpty(e)) { moves.add(e);
                } else if (board.getPiece(e).color != player) moves.add(e); }
        }
        return moves;
    }

    private ArrayList<int[]> generalRules() {
        ArrayList<int[]> moves = new ArrayList();

        int[][] target = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        for (int[] aTarget : target) {
            int[] e = { pos[0] + aTarget[0], pos[1] + aTarget[1] };
            if ((board.isInside(e)) && (((e[0] <= 2) && (e[1] >= 3) && (e[1] <= 5)) || ((player != 'b') && (((e[0] >= 7) && (e[1] >= 3) && (e[1] <= 5)) || (player != 'r')))))
            {
                if (board.isEmpty(e)) { moves.add(e);
                } else if (board.getPiece(e).color != player) moves.add(e);
            }
        }
        boolean flag = true;
        int[] oppoBoss = (player == 'r' ? board.pieces.get("bg0").position : board.pieces.get("rg0").position);
        if (oppoBoss[1] == pos[1]) {
            for (int i = Math.min(oppoBoss[0], pos[0]) + 1; i < Math.max(oppoBoss[0], pos[0]); i++) {
                if (board.getPiece(i, pos[1]) != null) {
                    flag = false;
                    break;
                }
            }
            if (flag) moves.add(oppoBoss);
        }
        return moves;
    }

    private ArrayList<int[]> totRules() {
        ArrayList<int[]> moves = new ArrayList();
        int[][] targetU = { { 0, 1 }, { 0, -1 }, { -1, 0 } };
        int[][] targetD = { { 0, 1 }, { 0, -1 }, { 1, 0 } };
        int[] e;
        if (player == 'r') {
            if (pos[0] > 4) {
                e = new int[] { pos[0] - 1, pos[1] };
                if (board.isEmpty(e)) { moves.add(e);
                } else if (board.getPiece(e).color != player) moves.add(e);
            } else {
                for (int[] aTarget : targetU) {
                    e = new int[] { pos[0] + aTarget[0], pos[1] + aTarget[1] };
                    if (board.isInside(e))
                        if (board.isEmpty(e)) { moves.add(e);
                        } else if (board.getPiece(e).color != player) moves.add(e);
                }
            }
        }
        if (player == 'b') {
            if (pos[0] < 5) {
                e = new int[] { pos[0] + 1, pos[1] };
                if (board.isEmpty(e)) { moves.add(e);
                } else if (board.getPiece(e).color != player) moves.add(e);
            } else {
                for (int[] aTarget : targetD) {
                    e = new int[]{ pos[0] + aTarget[0], pos[1] + aTarget[1] };
                    if (board.isInside(e)) {
                        if (board.isEmpty(e)) { moves.add(e);
                        } else if (board.getPiece(e).color != player) moves.add(e);
                    }
                }
            }
        }
        return moves;
    }
}