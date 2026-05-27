/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.baucua;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class StartNewGameBauCuaMsg
extends BaseMsgEx {
    public long referenceId;

    public StartNewGameBauCuaMsg() {
        super(5007);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putLong(bf, this.referenceId);
        return this.packBuffer(bf);
    }
}

