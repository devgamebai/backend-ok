/*
 * Decompiled with CFR 0.144.
 */
package game.cotuong.server.rule;

public class GameResult {
    public Name result;

    public static enum Name {
        WIN_LOST,
        DRAW,
        CONTINUE,
        ERROR_UNEXIST,
        ERROR_INVALID,
        ERROR_STATE,
        TIME_OUT,
        RESIGN;
        
    }

}

