/*
 * Decompiled with CFR 0.144.
 */
package game.cotuong.server.rule.ai;

import game.cotuong.server.rule.GameResult;
import game.cotuong.server.rule.Piece;

public class Move {
    public static final int END_GAME = 1;
    public static final int CONTINUE = 2;
    public static final int ERROR_UNEXIST = 3;
    public static final int ERROR_INVALID = 4;
    public static final int ERROR_STATE = 4;
    public static final int CHIEU_CU_NHAY = 5;
    public static final int THE_HOA = 6;
    public Piece piece;
    public int[] from;
    public int[] to;
    public Piece eatenPiece;
    public GameResult result = new GameResult();

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.piece.color).append(this.piece.character).append(" move from {");
        sb.append(this.from[0]).append(",").append(this.from[1]).append("}");
        sb.append(" to {");
        sb.append(this.to[0]).append(",").append(this.to[1]).append("}");
        if (this.eatenPiece != null) {
            sb.append(" eat ").append(this.eatenPiece.color).append(this.eatenPiece.character);
        }
        sb.append(" result: ").append((Object)this.result.result);
        return sb.toString();
    }

    public boolean sameWith(Move m) {
        return m.piece.key == this.piece.key && (m.from[0] == this.from[0] && m.from[1] == this.from[1] && m.to[0] == this.to[0] && m.to[1] == this.to[1] || m.from[0] == this.to[0] && m.from[1] == this.to[1] && m.to[0] == this.from[0] && m.to[1] == this.from[1]);
    }
}

