/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.txlive;

import game.BaseMsgEx;
import game.modules.minigame.model.BetInfo;
import java.nio.ByteBuffer;

public class BetInfoGameMsg
extends BaseMsgEx {
    public BetInfo[] betInfos;

    public BetInfoGameMsg() {
        super(29005);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        buffer.putShort((short)this.betInfos.length);
        for (BetInfo betInfo : this.betInfos) {
            this.putStr(buffer, betInfo.getBetType());
            buffer.putInt(betInfo.getTotalUser());
            buffer.putLong(betInfo.getTotalAmount());
        }
        return this.packBuffer(buffer);
    }
}

