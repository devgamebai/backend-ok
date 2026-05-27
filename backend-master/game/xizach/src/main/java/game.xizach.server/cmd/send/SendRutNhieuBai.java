/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xizach.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SendRutNhieuBai
extends BaseMsg {
    public static final int RUT_BAI_THANH_CONG = 0;
    public static final int KHONG_THE_RUT_BAI = 1;
    public byte chair;
    public List<Integer> listCard = new ArrayList<Integer>();

    public SendRutNhieuBai() {
        super((short)3133);
    }

    public void initData(byte chair, byte[] cards) {
        this.chair = chair;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.chair);
        this.putByteArray(bf, this.convert(this.listCard));
        return this.packBuffer(bf);
    }

    public byte[] convert(List<Integer> list) {
        byte[] d = new byte[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            d[i] = list.get(i).byteValue();
        }
        return d;
    }
}

