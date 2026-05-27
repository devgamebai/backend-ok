/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class LaunchGameCasinoMsg
extends BaseMsgEx {
    public String gameUrl;

    public LaunchGameCasinoMsg() {
        super(20303);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.gameUrl);
        return this.packBuffer(bf);
    }
}

