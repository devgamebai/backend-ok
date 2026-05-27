/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.modules.gameRoom.entities.GameRoomIdGenerator
 */
package game.cotuong.server.logic;

import game.cotuong.server.logic.CardSuit;
import game.cotuong.server.rule.Board;
import game.cotuong.server.rule.GameController;
import game.modules.gameRoom.entities.GameRoomIdGenerator;

public class Gamble {
    public CardSuit suit = new CardSuit();
    public GameController controller = new GameController();
    public Board board = new Board();
    public int id;
    public boolean isCheat = false;
    public long logTime;

    private static int getID() {
        int id = GameRoomIdGenerator.instance().getId();
        return id;
    }

    public Gamble() {
        this.reset();
    }

    public void reset() {
        this.id = Gamble.getID();
        this.logTime = System.currentTimeMillis();
        this.board.reset();
        this.controller.initBoard(this.board);
    }
}

