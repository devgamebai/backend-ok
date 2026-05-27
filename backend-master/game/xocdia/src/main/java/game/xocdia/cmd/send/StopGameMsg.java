/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class StopGameMsg
extends BaseMsg {
    public String banker;

    public StopGameMsg(String banker) {
        super((short)3122);
        this.banker = banker;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.banker);
        return this.packBuffer(bf);
    }
}

