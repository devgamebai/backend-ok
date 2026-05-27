/*
 * Decompiled with CFR 0.144.
 */
package game.modules.mission.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class RewardMissionMsg
extends BaseMsgEx {
    public int prize;
    public long currentMoney;

    public RewardMissionMsg() {
        super(21001);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

