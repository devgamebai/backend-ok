/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.admin.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ResponseAdminMsg
extends BaseMsg {
    public String info = "{}";

    public ResponseAdminMsg() {
        super((short)4000);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.info);
        return this.packBuffer(bf);
    }
}

