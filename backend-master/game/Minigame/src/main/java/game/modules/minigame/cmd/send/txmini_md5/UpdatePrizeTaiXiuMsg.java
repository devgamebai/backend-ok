/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class UpdatePrizeTaiXiuMsg
extends BaseMsgEx {
    public short moneyType;
    public long totalMoney;
    public long currentMoney;

    public UpdatePrizeTaiXiuMsg() {
        super(MiniGameCMD.UPDATE_TXMINI_MD5_PRIZE_TAI_XIU);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort(this.moneyType);
        buffer.putLong(this.totalMoney);
        buffer.putLong(this.currentMoney);
        return this.packBuffer(buffer);
    }
}

