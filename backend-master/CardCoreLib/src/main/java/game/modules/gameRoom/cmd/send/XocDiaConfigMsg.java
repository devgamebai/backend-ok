/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.gameRoom.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class XocDiaConfigMsg
extends BaseMsg {
    public double fundVipMinRegis;

    public XocDiaConfigMsg() {
        super((short)3017);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putDouble(this.fundVipMinRegis);
        return this.packBuffer(bf);
    }
}

