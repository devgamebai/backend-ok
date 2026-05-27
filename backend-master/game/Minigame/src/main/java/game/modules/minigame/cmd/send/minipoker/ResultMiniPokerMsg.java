/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.minipoker;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultMiniPokerMsg
extends BaseMsgEx {
    public short result;
    public long prize;
    public byte card1;
    public byte card2;
    public byte card3;
    public byte card4;
    public byte card5;
    public long currentMoney;

    public ResultMiniPokerMsg() {
        super(4001);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putShort(this.result);
        bf.putLong(this.prize);
        bf.put(this.card1);
        bf.put(this.card2);
        bf.put(this.card3);
        bf.put(this.card4);
        bf.put(this.card5);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

