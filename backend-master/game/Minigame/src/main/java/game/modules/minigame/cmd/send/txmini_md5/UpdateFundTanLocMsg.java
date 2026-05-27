/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class UpdateFundTanLocMsg
extends BaseMsgEx {
    public long value;

    public UpdateFundTanLocMsg() {
        super(MiniGameCMD.TXMINI_MD5_UPDATE_FUND_TAN_LOC);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        return this.packBuffer(bf);
    }
}

