/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class TaiXiuRefundMsg
extends BaseMsgEx {
    public long moneyRefund;

    public TaiXiuRefundMsg(long moneyRefund) {
        super(2200);
        this.moneyRefund = moneyRefund;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyRefund);
        return this.packBuffer(bf);
    }
}

