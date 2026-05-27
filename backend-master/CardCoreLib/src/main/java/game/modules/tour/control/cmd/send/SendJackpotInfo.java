/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendJackpotInfo
extends BaseMsg {
    public int currentJackpot = 0;

    public SendJackpotInfo() {
        super((short)5211);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.currentJackpot);
        return this.packBuffer(bf);
    }
}

