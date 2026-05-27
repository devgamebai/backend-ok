/*
 * Decompiled with CFR 0.144.
 */
package game.caro.server.logic;

import java.util.List;

public class GameResult {
    public Name result;
    public List<Byte> listX;
    public List<Byte> listY;

    public static enum Name {
        WIN_LOST,
        DRAW,
        CONTINUE,
        ERROR;
        
    }

}

