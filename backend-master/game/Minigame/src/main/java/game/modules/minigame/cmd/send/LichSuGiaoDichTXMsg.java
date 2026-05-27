/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class LichSuGiaoDichTXMsg
extends BaseMsgEx {
    public String[] data;

    public LichSuGiaoDichTXMsg() {
        super(2117);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStringArray(bf, this.data);
        return super.createData();
    }
}

