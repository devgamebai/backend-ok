package game.modules.slot.cmd.send.bikini;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class UpdatePotBikiniMsg extends BaseMsg {
    public long value;
    public byte x2 = 0;

    public UpdatePotBikiniMsg() {
        super((short)16002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        bf.put(this.x2);
        return this.packBuffer(bf);
    }
}
