/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendThongTinHu
extends BaseMsg {
    public String gameName = "";
    public int remainTime = -1;
    public long goldAmmount = -1;

    public SendThongTinHu() {
        super((short)3009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.gameName);
        bf.putInt(this.remainTime);
        bf.putLong(this.goldAmmount);
        return this.packBuffer(bf);
    }
}

