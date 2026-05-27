/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendUpdateTourLevel
extends BaseMsg {
    public int tourId;
    public byte level;
    public int smallBlind;
    public int countDownNextLevel;

    public SendUpdateTourLevel() {
        super((short)5204);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.tourId);
        bf.put(this.level);
        bf.putInt(this.smallBlind);
        bf.putInt(this.countDownNextLevel);
        return this.packBuffer(bf);
    }
}

