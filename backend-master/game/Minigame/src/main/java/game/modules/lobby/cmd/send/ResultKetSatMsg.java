/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultKetSatMsg
extends BaseMsgEx {
    public long moneyUse;
    public long safe;
    public long currentMoney;

    public ResultKetSatMsg() {
        super(20029);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyUse);
        bf.putLong(this.safe);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

