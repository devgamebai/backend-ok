/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.txmini_md5;

import game.BaseMsgEx;
import game.modules.minigame.cmd.MiniGameCMD;

import java.nio.ByteBuffer;

public class BetTaiXiuMsg
extends BaseMsgEx {
    public long currentMoney;

    public BetTaiXiuMsg() {
        super(MiniGameCMD.BET_TXMINI_MD5);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putLong(this.currentMoney);
        return this.packBuffer(buffer);
    }
}

