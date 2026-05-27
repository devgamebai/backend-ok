/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultNapXuMsg
extends BaseMsgEx {
    public long currentMoneyVin;
    public long currentMoneyXu;

    public ResultNapXuMsg() {
        super(20031);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoneyVin);
        bf.putLong(this.currentMoneyXu);
        return this.packBuffer(bf);
    }
}

