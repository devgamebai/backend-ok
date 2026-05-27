/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendNewFreeTicket
extends BaseMsg {
    public static final byte INVALID_CODE = 1;
    public int ticket = 0;
    public int timeBlock = 0;

    public SendNewFreeTicket() {
        super((short)5215);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.ticket);
        bf.putInt(this.timeBlock);
        return this.packBuffer(bf);
    }
}

