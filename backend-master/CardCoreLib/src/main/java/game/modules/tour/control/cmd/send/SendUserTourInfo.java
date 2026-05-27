/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendUserTourInfo
extends BaseMsg {
    public int timeBuyTicket;
    public int timeOutTour;
    public int maxTimeBuyTicket;
    public int tourId;

    public SendUserTourInfo() {
        super((short)5202);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.timeBuyTicket);
        bf.put((byte)this.maxTimeBuyTicket);
        bf.putInt(this.tourId);
        bf.put((byte)this.timeOutTour);
        return this.packBuffer(bf);
    }
}

