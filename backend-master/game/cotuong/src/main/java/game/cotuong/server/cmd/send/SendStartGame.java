/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.cotuong.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.cotuong.server.GamePlayer;
import game.cotuong.server.sPlayerInfo;
import java.nio.ByteBuffer;

public class SendStartGame
extends BaseMsg {
    public int starter;
    public boolean[] hasInfoAtChair = new boolean[20];
    public GamePlayer[] gamePlayers = new GamePlayer[20];

    public SendStartGame() {
        super((short)3100);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.starter);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 20; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            bf.put((byte)this.gamePlayers[i].gameChair);
            bf.put((byte)this.gamePlayers[i].playerStatus);
            bf.put((byte)this.gamePlayers[i].spInfo.chessColor);
            bf.putInt(this.gamePlayers[i].spInfo.turnTime);
            bf.putInt(this.gamePlayers[i].spInfo.gameTime);
        }
        return this.packBuffer(bf);
    }
}

