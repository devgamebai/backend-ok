/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.baucua;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class UpdateBauCuaPerSecondMsg
extends BaseMsgEx {
    public String potData;
    public byte remainTime;
    public boolean bettingState;

    public UpdateBauCuaPerSecondMsg() {
        super(5006);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.potData);
        bf.put(this.remainTime);
        this.putBoolean(bf, Boolean.valueOf(this.bettingState));
        return this.packBuffer(bf);
    }
}

