/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.txlive;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultTaiXiuLiveMsg
extends BaseMsgEx {
    public long currentMoney;
    public long totalMoney;

    public ResultTaiXiuLiveMsg() {
        super(29007);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.currentMoney);
        buffer.putLong(this.totalMoney);
        return this.packBuffer(buffer);
    }
}

