/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class BroadcastTXTimeMsg
extends BaseMsgEx {
    public byte remainTime;
    public boolean betting;

    public BroadcastTXTimeMsg() {
        super(MiniGameCMD.BROADCAST_TXMINI_MD5_TIME);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remainTime);
        this.putBoolean(bf, Boolean.valueOf(this.betting));
        return this.packBuffer(bf);
    }
}

