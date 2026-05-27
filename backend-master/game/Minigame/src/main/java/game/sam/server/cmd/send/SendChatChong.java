/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendChatChong
extends BaseMsgEx {
    public int winChair;
    public int lostChair;
    public long winMoney;
    public long lostMoney;
    public long curWinPlayerMoney;
    public long curLostPlayerMoney;

    public SendChatChong() {
        super(3113);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.winChair);
        bf.put((byte)this.lostChair);
        this.putLong(bf, this.winMoney);
        this.putLong(bf, this.lostMoney);
        this.putLong(bf, this.curWinPlayerMoney);
        this.putLong(bf, this.curLostPlayerMoney);
        return this.packBuffer(bf);
    }
}

