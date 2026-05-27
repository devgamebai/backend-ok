/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.caro.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.caro.server.GamePlayer;
import java.nio.ByteBuffer;

public class SendStartGame
extends BaseMsg {
    public int starter;
    public boolean[] hasInfoAtChair = new boolean[2];
    public GamePlayer[] gamePlayers = new GamePlayer[2];

    public SendStartGame() {
        super((short)3100);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.starter);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 2; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            bf.put((byte)this.gamePlayers[i].getPlayerStatus());
            bf.put((byte)this.gamePlayers[i].tickType);
        }
        return this.packBuffer(bf);
    }
}

