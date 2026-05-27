/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.modules.gameRoom.entities.GameRoomIdGenerator
 *  game.utils.GameUtils
 */
package game.binh.server.logic;

import game.binh.server.logic.CardSuit;
import game.modules.gameRoom.entities.GameRoomIdGenerator;
import game.utils.GameUtils;

public class Gamble {
    public CardSuit suit = new CardSuit();
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
        if (!this.isCheat || !GameUtils.isCheat) {
            this.suit.setRandom();
        }
    }
}

