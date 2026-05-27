/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ApiOtpConfirmMsg
extends BaseMsgEx {
    public long currentMoney;

    public ApiOtpConfirmMsg() {
        super(20041);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}

