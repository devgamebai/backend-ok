/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class DepositLiveCasinoMsg
extends BaseMsgEx {
    public String amount;

    public DepositLiveCasinoMsg() {
        super(20302);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.amount);
        return this.packBuffer(bf);
    }
}

