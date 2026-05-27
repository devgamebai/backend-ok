/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class UpdateTaiXiuPerSecondMsg
extends BaseMsgEx {
    public short remainTime;
    public boolean bettingState;
    public long potTai;
    public long potXiu;
    public short numBetTai;
    public short numBetXiu;

    public UpdateTaiXiuPerSecondMsg() {
        super(MiniGameCMD.UPDATE_TXMINI_MD5_PER_SECOND);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.remainTime);
        this.putBoolean(buffer, Boolean.valueOf(this.bettingState));
        buffer.putLong(this.potTai);
        buffer.putLong(this.potXiu);
        buffer.putShort(this.numBetTai);
        buffer.putShort(this.numBetXiu);
        return this.packBuffer(buffer);
    }
}

