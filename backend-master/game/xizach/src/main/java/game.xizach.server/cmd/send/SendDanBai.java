/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendDanBai
extends BaseMsg {
    public static final int RUT_BAI_THANH_CONG = 0;
    public static final int KHONG_THE_DAN_BAI = 1;
    public byte chair;

    public SendDanBai() {
        super((short)3129);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        return this.packBuffer(bf);
    }
}

