/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class CashOutAllLiveCasinoMsg
extends BaseMsgEx {
    public String balance;
    public String amount;

    public CashOutAllLiveCasinoMsg() {
        super(20301);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.balance);
        this.putStr(bf, this.amount);
        return this.packBuffer(bf);
    }
}

