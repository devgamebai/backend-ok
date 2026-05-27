/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendCountDownToPlay
extends BaseMsg {
    public int coundDown;

    public SendCountDownToPlay() {
        super((short)5213);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.coundDown);
        return this.packBuffer(bf);
    }
}

