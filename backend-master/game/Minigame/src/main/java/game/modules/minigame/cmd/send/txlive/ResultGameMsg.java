/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.txlive;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultGameMsg
extends BaseMsgEx {
    public long currentMoney;
    public long moneyWin;
    public short dice1;
    public short dice2;
    public short dice3;

    public ResultGameMsg() {
        super(29003);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.currentMoney);
        buffer.putLong(this.moneyWin);
        buffer.putShort(this.dice1);
        buffer.putShort(this.dice2);
        buffer.putShort(this.dice3);
        return this.packBuffer(buffer);
    }
}

