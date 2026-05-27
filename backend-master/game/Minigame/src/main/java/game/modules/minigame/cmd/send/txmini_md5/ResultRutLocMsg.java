/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class ResultRutLocMsg
extends BaseMsgEx {
    public int prize = 0;
    public long currentMoney;

    public ResultRutLocMsg() {
        super(MiniGameCMD.TXMINI_MD5_RUT_LOC);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

