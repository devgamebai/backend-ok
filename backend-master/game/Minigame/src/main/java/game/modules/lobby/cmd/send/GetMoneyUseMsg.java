/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GetMoneyUseMsg
extends BaseMsgEx {
    public long moneyUse;

    public GetMoneyUseMsg() {
        super(20051);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyUse);
        return this.packBuffer(bf);
    }
}

