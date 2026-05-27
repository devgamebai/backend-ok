/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class RegisChangeLockPotMsg
extends BaseMsg {
    public static final byte SUCCESS = 0;
    public boolean bChangeLock;

    public RegisChangeLockPotMsg() {
        super((short)3131);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.bChangeLock));
        return this.packBuffer(bf);
    }
}

