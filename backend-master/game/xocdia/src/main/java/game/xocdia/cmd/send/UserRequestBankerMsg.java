/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UserRequestBankerMsg
extends BaseMsg {
    public UserRequestBankerMsg() {
        super((short)3114);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        return this.packBuffer(bf);
    }
}

