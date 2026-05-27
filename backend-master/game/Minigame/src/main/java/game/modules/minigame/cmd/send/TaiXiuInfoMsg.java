/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class TaiXiuInfoMsg
extends BaseMsgEx {
    public short gameId;
    public short moneyType;
    public long referenceId;
    public short remainTime;
    public boolean bettingState;
    public long potTai;
    public long potXiu;
    public long myBetTai;
    public long myBetXiu;
    public short dice1 = 0;
    public short dice2 = 0;
    public short dice3 = 0;
    public long jpTai;
    public long jpXiu;
    public short remainTimeRutLoc = 0;

    public TaiXiuInfoMsg() {
        super(2111);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.gameId);
        buffer.putShort(this.moneyType);
        buffer.putLong(this.referenceId);
        buffer.putShort(this.remainTime);
        this.putBoolean(buffer, this.bettingState);
        buffer.putLong(this.potTai);
        buffer.putLong(this.potXiu);
        buffer.putLong(this.myBetTai);
        buffer.putLong(this.myBetXiu);
        buffer.putShort(this.dice1);
        buffer.putShort(this.dice2);
        buffer.putShort(this.dice3);
        buffer.putShort(this.remainTimeRutLoc);
        buffer.putLong(this.jpTai);
        buffer.putLong(this.jpXiu);
        return this.packBuffer(buffer);
    }
}

