/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class LichSuPhienMsg
extends BaseMsgEx {
    public String data;

    public LichSuPhienMsg() {
        super(MiniGameCMD.LICH_SU_PHIEN_TXMINI_MD5);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        this.putStr(buffer, this.data);
        return this.packBuffer(buffer);
    }
}

