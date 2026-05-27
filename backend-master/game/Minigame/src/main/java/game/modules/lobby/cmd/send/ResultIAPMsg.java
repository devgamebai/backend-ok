/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultIAPMsg
extends BaseMsgEx {
    public byte productId;
    public long currentMoney;

    public ResultIAPMsg() {
        super(20038);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.productId);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

