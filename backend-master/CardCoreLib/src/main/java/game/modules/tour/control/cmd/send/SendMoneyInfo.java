/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendMoneyInfo
extends BaseMsg {
    public long money;

    public SendMoneyInfo() {
        super((short)5216);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putLong(bf, this.money);
        return this.packBuffer(bf);
    }
}

