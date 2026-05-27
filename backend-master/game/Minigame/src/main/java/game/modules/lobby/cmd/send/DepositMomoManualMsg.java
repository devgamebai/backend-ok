/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class DepositMomoManualMsg
extends BaseMsgEx {
    public String data;

    public DepositMomoManualMsg() {
        super(20202);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.data);
        return this.packBuffer(bf);
    }
}

