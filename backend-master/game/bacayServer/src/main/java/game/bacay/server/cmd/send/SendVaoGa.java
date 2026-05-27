/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendVaoGa
extends BaseMsg {
    public int chair;
    public long tienVaoGa = 0L;
    public static final int VAO_GA_THANH_CONG = 0;
    public static final int ERROR_DA_VAO_GA = 1;
    public static final int ERROR_KHONG_DU_TIEN_VAO_GA = 2;

    public SendVaoGa() {
        super((short)3112);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.putLong(this.tienVaoGa);
        return this.packBuffer(bf);
    }
}

