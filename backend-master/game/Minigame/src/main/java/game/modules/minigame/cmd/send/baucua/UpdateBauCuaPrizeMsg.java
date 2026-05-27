/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.baucua;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateBauCuaPrizeMsg
extends BaseMsgEx {
    public long prize;
    public long currentMoney;
    public byte room;

    public UpdateBauCuaPrizeMsg() {
        super(5009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.prize);
        bf.putLong(this.currentMoney);
        bf.put(this.room);
        return this.packBuffer(bf);
    }
}

