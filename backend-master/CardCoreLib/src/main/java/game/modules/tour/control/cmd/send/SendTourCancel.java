/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendTourCancel
extends BaseMsg {
    public int tourId;

    public SendTourCancel() {
        super((short)5208);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.tourId);
        return this.packBuffer(bf);
    }
}

