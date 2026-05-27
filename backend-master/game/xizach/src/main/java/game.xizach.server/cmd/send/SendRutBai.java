/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendRutBai
extends BaseMsg {
    public static final int RUT_BAI_THANH_CONG = 0;
    public static final int KHONG_THE_RUT_BAI = 1;
    public byte chair;
    public byte card;

    public SendRutBai() {
        super((short)3128);
    }

    public void initData(byte chair, byte card) {
        this.chair = chair;
        this.card = card;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        bf.put(this.card);
        return this.packBuffer(bf);
    }
}

