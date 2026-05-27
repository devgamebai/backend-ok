/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.binh.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.binh.server.logic.KetQuaSoBai;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SendEndGame
extends BaseMsg {
    public List<KetQuaSoBai> ketqua = new ArrayList<KetQuaSoBai>();
    public long[] moneyArray = new long[4];
    public int countdownsochi;

    public SendEndGame() {
        super((short)3103);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putShort((short)this.ketqua.size());
        for (int i = 0; i < this.ketqua.size(); ++i) {
            KetQuaSoBai kq = this.ketqua.get(i);
            bf.put((byte)kq.chair);
            bf.putInt(kq.maubinhType);
            this.putByteArray(bf, kq.chi1);
            this.putByteArray(bf, kq.chi2);
            this.putByteArray(bf, kq.chi3);
            this.putLongArray(bf, kq.moneyInChi);
            bf.putLong(kq.moneyAt);
            bf.putLong(kq.moneyCommon);
            bf.putLong(kq.getMoneySapTong());
            bf.putLong(this.moneyArray[kq.chair]);
        }
        bf.put((byte)this.countdownsochi);
        return this.packBuffer(bf);
    }
}

