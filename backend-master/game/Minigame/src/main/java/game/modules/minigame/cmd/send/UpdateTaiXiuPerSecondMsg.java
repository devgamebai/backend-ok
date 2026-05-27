/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateTaiXiuPerSecondMsg
extends BaseMsgEx {
    public short remainTime;
    public boolean bettingState;
    public long potTai;
    public long potXiu;
    public short numBetTai;
    public short numBetXiu;
    public long referenceId;
    public long realPotTai;
    public long realPotXiu;
    public short realNumBetTai;
    public short realNumBetXiu;
    public long fundJpTai;
    public long fundJpXiu;

    public UpdateTaiXiuPerSecondMsg() {
        super(2112);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.remainTime);
        this.putBoolean(buffer, this.bettingState);
        buffer.putLong(this.potTai);
        buffer.putLong(this.potXiu);
        buffer.putShort(this.numBetTai);
        buffer.putShort(this.numBetXiu);
        buffer.putLong(this.fundJpTai);
        buffer.putLong(this.fundJpXiu);
        buffer.putLong(this.referenceId);
        buffer.putLong(this.realPotTai);
        buffer.putLong(this.realPotXiu);
        buffer.putShort(this.realNumBetTai);
        buffer.putShort(this.realNumBetXiu);
        return this.packBuffer(buffer);
    }
}

