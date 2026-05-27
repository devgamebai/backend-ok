/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendMoiDatCuoc
extends BaseMsg {
    public byte countDownTime;

    public SendMoiDatCuoc() {
        super((short)3114);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.countDownTime);
        return this.packBuffer(bf);
    }
}

