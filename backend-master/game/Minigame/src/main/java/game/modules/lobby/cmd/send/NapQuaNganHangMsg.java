/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class NapQuaNganHangMsg
extends BaseMsgEx {
    public String url;

    public NapQuaNganHangMsg() {
        super(20013);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.url);
        return this.packBuffer(bf);
    }
}

