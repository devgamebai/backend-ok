/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GetBalanceLiveCasinoMsg
extends BaseMsgEx {
    public String balance;

    public GetBalanceLiveCasinoMsg() {
        super(20300);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.balance);
        return this.packBuffer(bf);
    }
}

