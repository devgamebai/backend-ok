/*
 * Decompiled with CFR 0.152.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class DepositBankManualMsg
extends BaseMsgEx {
    public String data;

    public DepositBankManualMsg() {
        super(20203);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.data);
        return this.packBuffer(bf);
    }
}

