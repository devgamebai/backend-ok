/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultChuyenKhoanMsg
extends BaseMsgEx {
    public long moneyUse;
    public long currentMoney;

    public ResultChuyenKhoanMsg() {
        super(20034);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.moneyUse);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

