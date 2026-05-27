/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.baucua;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ResultBetBauCuaMsg
extends BaseMsgEx {
    public byte result;
    public long currentMoney;

    public ResultBetBauCuaMsg() {
        super(5004);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.result);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

