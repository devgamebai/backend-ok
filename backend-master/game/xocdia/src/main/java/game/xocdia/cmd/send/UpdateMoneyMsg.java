/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdateMoneyMsg
extends BaseMsg {
    public String nickname;
    public long money;

    public UpdateMoneyMsg() {
        super((short)3135);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.nickname);
        bf.putLong(this.money);
        return this.packBuffer(bf);
    }
}

