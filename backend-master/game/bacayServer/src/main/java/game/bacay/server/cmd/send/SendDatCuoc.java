/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendDatCuoc
extends BaseMsg {
    public static final int DAT_CUOC_THANH_CONG = 0;
    public static final int ERROR_DA_DAT_CUOC = 1;
    public static final int ERROR_KHONG_DU_TIEN = 2;
    public static final int MUC_CUOC_KHONG_DUNG = 3;
    public int chair;
    public int rate;

    public SendDatCuoc() {
        super((short)3109);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        bf.put((byte)this.rate);
        return this.packBuffer(bf);
    }
}

