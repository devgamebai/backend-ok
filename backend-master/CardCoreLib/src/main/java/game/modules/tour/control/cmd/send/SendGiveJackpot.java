/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;
import java.util.List;

public class SendGiveJackpot
extends BaseMsg {
    public List<String> nickNames;
    public int eachPrize;
    public int currentJackpot;

    public SendGiveJackpot() {
        super((short)5210);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.eachPrize);
        bf.putInt(this.currentJackpot);
        int size = this.nickNames.size();
        bf.putShort((short)size);
        for (int i = 0; i < size; ++i) {
            this.putStr(bf, this.nickNames.get(i));
        }
        return this.packBuffer(bf);
    }
}

