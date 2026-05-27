/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultDoiVippointMsg
extends BaseMsgEx {
    public long currentMoney;
    public long moneyAdd;

    public ResultDoiVippointMsg() {
        super(20021);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoney);
        bf.putLong(this.moneyAdd);
        return this.packBuffer(bf);
    }
}

