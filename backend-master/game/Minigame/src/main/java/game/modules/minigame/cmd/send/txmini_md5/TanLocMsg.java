/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class TanLocMsg
extends BaseMsgEx {
    public short result;
    public long currentMoney;

    public TanLocMsg() {
        super(MiniGameCMD.TXMINI_MD5_TAN_LOC);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putShort(this.result);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

