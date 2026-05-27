/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class RequestBankerFailMsg
extends BaseMsg {
    public static final byte NOT_ENOUGH_MONEY = 1;
    public static final byte HAS_BANKER = 2;

    public RequestBankerFailMsg() {
        super((short)3123);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        return this.packBuffer(bf);
    }
}

