/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.binh.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendBinhSoChiSuccess
extends BaseMsg {
    public static final byte BAI_KHONG_HOP_LE = 1;
    public static final byte NGUOI_CHOI_KHONG_HOP_LE = 2;
    public int chair;

    public SendBinhSoChiSuccess() {
        super((short)3101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.chair);
        return this.packBuffer(bf);
    }
}

