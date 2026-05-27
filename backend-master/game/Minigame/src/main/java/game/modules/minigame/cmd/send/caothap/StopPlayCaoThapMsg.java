/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class StopPlayCaoThapMsg
extends BaseMsgEx {
    public byte result;
    public long currentMoney;
    public long moneyExchange;

    public StopPlayCaoThapMsg() {
        super(6007);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.result);
        bf.putLong(this.currentMoney);
        bf.putLong(this.moneyExchange);
        return this.packBuffer(bf);
    }
}

