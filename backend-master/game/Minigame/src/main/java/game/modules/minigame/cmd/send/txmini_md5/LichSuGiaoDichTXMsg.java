/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class LichSuGiaoDichTXMsg
extends BaseMsgEx {
    public String[] data;

    public LichSuGiaoDichTXMsg() {
        super(MiniGameCMD.LICH_SU_GIAO_DICH_TXMINI_MD5);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStringArray(bf, this.data);
        return super.createData();
    }
}

