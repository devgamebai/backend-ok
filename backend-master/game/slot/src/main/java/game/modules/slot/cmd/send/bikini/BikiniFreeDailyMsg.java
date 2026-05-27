package game.modules.slot.cmd.send.bikini;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class BikiniFreeDailyMsg extends BaseMsg {
    public byte remain = 0;

    public BikiniFreeDailyMsg() {
        super((short)16012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}
