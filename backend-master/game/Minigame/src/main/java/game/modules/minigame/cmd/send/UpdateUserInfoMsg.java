/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateUserInfoMsg
extends BaseMsgEx {
    public long currentMoney;
    public short moneyType;

    public UpdateUserInfoMsg() {
        super(2003);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.currentMoney);
        buffer.putShort(this.moneyType);
        return this.packBuffer(buffer);
    }
}

