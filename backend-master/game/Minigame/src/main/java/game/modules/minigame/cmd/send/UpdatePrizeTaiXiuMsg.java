/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdatePrizeTaiXiuMsg
extends BaseMsgEx {
    public short moneyType;
    public long totalMoney;
    public long currentMoney;

    public UpdatePrizeTaiXiuMsg() {
        super(2114);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.moneyType);
        buffer.putLong(this.totalMoney);
        buffer.putLong(this.currentMoney);
        return this.packBuffer(buffer);
    }
}

