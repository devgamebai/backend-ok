/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.tour.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendStandUp
extends BaseMsg {
    public boolean standUp;

    public SendStandUp() {
        super((short)3113);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putBoolean(bf, Boolean.valueOf(this.standUp));
        return this.packBuffer(bf);
    }
}

