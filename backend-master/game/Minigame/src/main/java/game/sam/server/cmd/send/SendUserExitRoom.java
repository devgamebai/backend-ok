/*
 * Decompiled with CFR 0.144.
 */
package game.sam.server.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class SendUserExitRoom
extends BaseMsgEx {
    public byte nChair;
    public String nickName;

    public SendUserExitRoom() {
        super(3119);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.nChair);
        this.putStr(bf, this.nickName);
        return this.packBuffer(bf);
    }
}

