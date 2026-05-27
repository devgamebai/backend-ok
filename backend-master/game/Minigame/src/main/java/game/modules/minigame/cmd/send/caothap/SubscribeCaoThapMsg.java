/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.cmd.send.caothap;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SubscribeCaoThapMsg
extends BaseMsgEx {
    public byte status;
    public byte roomId;

    public SubscribeCaoThapMsg() {
        super(6004);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.status);
        bf.put(this.roomId);
        return this.packBuffer(bf);
    }
}

