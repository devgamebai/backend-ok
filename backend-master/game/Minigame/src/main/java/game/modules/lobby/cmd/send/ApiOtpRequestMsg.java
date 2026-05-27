/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class ApiOtpRequestMsg
extends BaseMsgEx {
    public String requestId;
    public String url;
    public long time;
    public int numFail;

    public ApiOtpRequestMsg() {
        super(20040);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.requestId);
        this.putStr(bf, this.url);
        bf.putLong(this.time);
        bf.putInt(this.numFail);
        return this.packBuffer(bf);
    }
}

