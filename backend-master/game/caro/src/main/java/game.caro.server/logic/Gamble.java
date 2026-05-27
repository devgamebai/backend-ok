/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.modules.gameRoom.entities.GameRoomIdGenerator
 */
package game.caro.server.logic;

import game.caro.server.logic.CardSuit;
import game.caro.server.logic.CaroTable;
import game.modules.gameRoom.entities.GameRoomIdGenerator;

public class Gamble {
    public CardSuit suit = new CardSuit();
    public CaroTable table = new CaroTable();
    public int id = Gamble.getID();
    public boolean isCheat = false;
    public long logTime = System.currentTimeMillis();

    private static int getID() {
        int id = GameRoomIdGenerator.instance().getId();
        return id;
    }

    public void reset() {
        this.id = Gamble.getID();
        this.logTime = System.currentTimeMillis();
        this.table.reset();
    }
}

