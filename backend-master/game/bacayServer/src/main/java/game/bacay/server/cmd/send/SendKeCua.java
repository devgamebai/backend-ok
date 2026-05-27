/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendKeCua
extends BaseMsg {
    public static final int KE_CUA_THANH_CONG = 0;
    public static final int ERROR_DA_KE_CUA = 1;
    public static final int ERROR_KHONG_DU_TIEN = 2;
    public int fromChair;
    public int toChair;
    public int rate;

    public SendKeCua() {
        super((short)3106);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.fromChair);
        bf.put((byte)this.toChair);
        bf.put((byte)this.rate);
        return this.packBuffer(bf);
    }
}

