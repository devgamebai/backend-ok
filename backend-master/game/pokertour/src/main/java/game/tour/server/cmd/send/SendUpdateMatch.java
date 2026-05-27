/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tour.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.tour.server.GamePlayer;
import game.tour.server.logic.PokerPlayerInfo;
import game.tour.server.sPlayerInfo;
import java.nio.ByteBuffer;

public class SendUpdateMatch
extends BaseMsg {
    public byte chair;
    public boolean[] hasInfoAtChair = new boolean[6];
    public GamePlayer[] pInfos = new GamePlayer[6];

    public SendUpdateMatch() {
        super((short)3123);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putBooleanArray(bf, this.hasInfoAtChair);
        for (int i = 0; i < 6; ++i) {
            if (!this.hasInfoAtChair[i]) continue;
            GamePlayer gp = this.pInfos[i];
            bf.putLong(gp.spInfo.pokerInfo.currentMoney);
            bf.putInt(gp.getPlayerStatus());
        }
        return this.packBuffer(bf);
    }
}

