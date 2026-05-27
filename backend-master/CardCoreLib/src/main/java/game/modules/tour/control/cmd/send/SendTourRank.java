/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendTourRank
extends BaseMsg {
    public int tourId = 0;
    public String bxh = "[]";

    public SendTourRank() {
        super((short)5203);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.tourId);
        this.putStr(bf, this.bxh);
        return this.packBuffer(bf);
    }
}

