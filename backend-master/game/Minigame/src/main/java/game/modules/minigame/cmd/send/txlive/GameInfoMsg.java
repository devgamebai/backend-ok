/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.txlive;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class GameInfoMsg
extends BaseMsgEx {
    public String linkLive;

    public GameInfoMsg() {
        super(29011);
    }

    public byte[] createData() {
        ByteBuffer buffer = this.makeBuffer();
        this.putStr(buffer, this.linkLive);
        return this.packBuffer(buffer);
    }
}

