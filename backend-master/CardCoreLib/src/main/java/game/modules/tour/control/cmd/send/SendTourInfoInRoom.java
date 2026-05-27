/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendTourInfoInRoom
extends BaseMsg {
    public byte level;
    public int prizePool;
    public int userCount;
    public int registerCount;
    public int endRegisterHour;
    public int endRegisterMinute;

    public SendTourInfoInRoom() {
        super((short)5209);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.level);
        bf.putInt(this.prizePool);
        bf.putShort((short)this.userCount);
        bf.putShort((short)this.registerCount);
        bf.put((byte)this.endRegisterHour);
        bf.put((byte)this.endRegisterMinute);
        return this.packBuffer(bf);
    }
}

