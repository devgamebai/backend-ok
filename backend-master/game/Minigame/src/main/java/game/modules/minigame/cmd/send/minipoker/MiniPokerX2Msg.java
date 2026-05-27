/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.minipoker;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class MiniPokerX2Msg
extends BaseMsgEx {
    public String ngayX2;

    public MiniPokerX2Msg() {
        super(4009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.ngayX2);
        return this.packBuffer(bf);
    }
}

