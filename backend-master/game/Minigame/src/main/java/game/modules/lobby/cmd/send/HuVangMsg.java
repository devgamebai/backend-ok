/*
 * Decompiled with CFR 0.144.
 */
package game.modules.lobby.cmd.send;

import game.BaseMsgEx;
import java.nio.ByteBuffer;

public class HuVangMsg
extends BaseMsgEx {
    public int huBaCay;
    public int huBaiCao;
    public int huBinh;
    public int huSam;
    public int huTLMN;

    public HuVangMsg() {
        super(20104);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.huBaCay);
        bf.putInt(this.huBaiCao);
        bf.putInt(this.huBinh);
        bf.putInt(this.huSam);
        bf.putInt(this.huTLMN);
        return this.packBuffer(bf);
    }
}

