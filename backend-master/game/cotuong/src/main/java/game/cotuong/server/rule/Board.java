/*
 * Decompiled with CFR 0.144.
 */
package game.cotuong.server.rule;

import game.cotuong.server.rule.Piece;
import game.cotuong.server.rule.ai.Move;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Board {
    public static final int BOARD_WIDTH = 9;
    public static final int BOARD_HEIGHT = 10;
    public Map<String, Piece> pieces = new HashMap<String, Piece>();
    public Map<String, Piece> deadPieces = new HashMap<String, Piece>();
    public List<Move> moveList = new LinkedList<Move>();
    public char player = (char)98;
    public Piece[][] cells = new Piece[10][9];
    public int stateCount = 0;
    public int blackMate = 0;
    public int redMate = 0;

    public void reset() {
        this.pieces.clear();
        this.deadPieces.clear();
        this.moveList.clear();
        this.player = (char)98;
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.cells[i][j] = null;
            }
        }
        this.stateCount = 0;
    }

    public boolean isInside(int[] position) {
        return this.isInside(position[0], position[1]);
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 9;
    }

    public boolean isEmpty(int[] position) {
        return this.isEmpty(position[0], position[1]);
    }

    public boolean isEmpty(int x, int y) {
        return this.isInside(x, y) && this.cells[x][y] == null;
    }

    public boolean update(Piece piece) {
        int[] pos = piece.position;
        this.cells[pos[0]][pos[1]] = piece;
        return true;
    }

    public Piece updatePiece(String key, int[] newPos) {
        Piece orig = this.pieces.get(key);
        Piece inNewPos = this.getPiece(newPos);
        if (inNewPos != null) {
            this.pieces.remove(inNewPos.key);
            this.deadPieces.put(inNewPos.key, inNewPos);
        }
        int[] origPos = orig.position;
        this.cells[origPos[0]][origPos[1]] = null;
        this.cells[newPos[0]][newPos[1]] = orig;
        orig.position = newPos;
        this.player = (char)(this.player == 'r' ? 98 : 114);
        return inNewPos;
    }

    public boolean backPiece(String key) {
        int[] origPos = this.pieces.get((Object)key).position;
        this.cells[origPos[0]][origPos[1]] = this.pieces.get(key);
        return true;
    }

    public Piece getPiece(int[] pos) {
        return this.getPiece(pos[0], pos[1]);
    }

    public Piece getPiece(int x, int y) {
        return this.cells[x][y];
    }

    public Board clone() {
        Piece piece;
        Board b = new Board();
        b.player = this.player;
        b.pieces = new HashMap<String, Piece>();
        for (Map.Entry<String, Piece> entry : this.pieces.entrySet()) {
            piece = entry.getValue();
            b.pieces.put(piece.key, piece);
            b.cells[piece.position[0]][piece.position[1]] = piece;
        }
        for (Map.Entry<String, Piece> entry : this.deadPieces.entrySet()) {
            piece = entry.getValue();
            b.deadPieces.put(piece.key, piece);
        }
        return b;
    }

    public Piece getPiece(String key) {
        return this.pieces.get(key);
    }

    public void revert(Move move) {
        this.cells[move.from[0]][move.from[1]] = move.piece;
        move.piece.position = move.from;
        if (move.eatenPiece != null) {
            this.cells[move.to[0]][move.to[1]] = move.eatenPiece;
            this.pieces.put(move.eatenPiece.key, move.eatenPiece);
            this.deadPieces.remove(move.eatenPiece.key);
        } else {
            this.cells[move.to[0]][move.to[1]] = null;
        }
        this.player = this.player == 'r' ? (char)98 : (char)114;
    }

    public byte[][] getMap() {
        byte[][] map = new byte[10][9];
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 9; ++j) {
                Piece p = this.cells[i][j];
                map[i][j] = (byte)p.character;
            }
        }
        return new byte[0][0];
    }

    public String[][] getMapKey() {
        String[][] map = new String[10][9];
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 9; ++j) {
                Piece p = this.cells[i][j];
                map[i][j] = p != null ? p.key : "";
            }
        }
        return map;
    }

    public int checkLoopMove(Move move) {
        int rangeCheck;
        int size = this.moveList.size();
        if (size < (rangeCheck = 12)) {
            return 0;
        }
        int min = size - rangeCheck;
        int c = 0;
        for (int i = size - 1; i >= min; --i) {
            Move m = this.moveList.get(i);
            if (!m.sameWith(move)) continue;
            ++c;
        }
        if (c >= 3) {
            return 5;
        }
        return 0;
    }

    public Move getLastMove() {
        int size = this.moveList.size();
        if (size > 0) {
            return this.moveList.get(size - 1);
        }
        return null;
    }

    public Move getPreviousMove(Move move) {
        int size = this.moveList.size();
        for (int i = size - 1; i >= 0; --i) {
            Move m = this.moveList.get(i);
            if (m.piece.color != move.piece.color) continue;
            return m;
        }
        return null;
    }
}

