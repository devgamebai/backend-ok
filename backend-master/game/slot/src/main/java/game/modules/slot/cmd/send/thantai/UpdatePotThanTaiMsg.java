package game.modules.slot.cmd.send.thantai;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdatePotThanTaiMsg extends BaseMsg {
    public long value;
    public byte x2 = 0;

    public UpdatePotThanTaiMsg() {
        super((short) 7002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        bf.put(this.x2);
        return this.packBuffer(bf);
    }
}
