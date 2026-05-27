/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.modules.gameRoom.entities.GameRoomIdGenerator
 *  game.utils.GameUtils
 */
package game.bacay.server.logic;

import game.bacay.server.logic.CardSuit;
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
        this.id = GameRoomIdGenerator.instance().getId();
        this.logTime = System.currentTimeMillis();
        if (!this.isCheat || !GameUtils.isCheat) {
            this.suit.setRandom();
        }
    }
}

