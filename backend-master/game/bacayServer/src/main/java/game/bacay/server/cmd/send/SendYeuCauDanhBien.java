/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendYeuCauDanhBien
extends BaseMsg {
    public static final int DANH_BIEN_THANH_CONG = 0;
    public static final int ERROR_DA_DANH_BIEN = 1;
    public static final int ERROR_DANH_BIEN_KHONG_HOP_LE = 2;
    public int chair;
    public int rate;

    public SendYeuCauDanhBien() {
        super((short)3104);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.put((byte)this.rate);
        return this.packBuffer(bf);
    }
}

