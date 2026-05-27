/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultNapTienDienThoaiMsg
extends BaseMsgEx {
    public long currentMoney;

    public ResultNapTienDienThoaiMsg() {
        super(20036);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

