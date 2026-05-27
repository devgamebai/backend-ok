/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class DestroyBankerMsg
extends BaseMsg {
    public boolean bDestroy;
    public String bankerName;

    public DestroyBankerMsg(String bankerName) {
        super((short)3130);
        this.bankerName = bankerName;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.bDestroy));
        this.putStr(bf, this.bankerName);
        return this.packBuffer(bf);
    }
}

