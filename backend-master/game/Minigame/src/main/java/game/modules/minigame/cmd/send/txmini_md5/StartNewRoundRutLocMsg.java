/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class StartNewRoundRutLocMsg
extends BaseMsgEx {
    public int remainTime;

    public StartNewRoundRutLocMsg() {
        super(MiniGameCMD.TXMINI_MD5_START_NEW_ROUND_RUT_LOC);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.remainTime);
        return this.packBuffer(bf);
    }
}

