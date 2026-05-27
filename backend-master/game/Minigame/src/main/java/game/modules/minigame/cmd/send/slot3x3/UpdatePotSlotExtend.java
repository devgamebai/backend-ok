/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.minigame.cmd.send.slot3x3;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdatePotSlotExtend
extends BaseMsg {
    public long value1;
    public long value2;
    public long value3;

    public UpdatePotSlotExtend() {
        super((short)8006);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value1);
        bf.putLong(this.value2);
        bf.putLong(this.value3);
        return this.packBuffer(bf);
    }
}

