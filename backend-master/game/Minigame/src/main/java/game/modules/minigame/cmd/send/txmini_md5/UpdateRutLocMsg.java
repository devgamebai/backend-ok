/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class UpdateRutLocMsg
extends BaseMsgEx {
    public int soLuotRut = 0;

    public UpdateRutLocMsg() {
        super(MiniGameCMD.TXMINI_MD5_UPDATE_SO_LUOT_RUT_LOC);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.soLuotRut);
        return this.packBuffer(bf);
    }
}

