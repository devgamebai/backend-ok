package game.modules.slot.cmd.send.galaxy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdatePotGalaxyMsg extends BaseMsg {
    public long value;
    public byte x2 = 0;

    public UpdatePotGalaxyMsg() {
        super((short) 8002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        bf.put(this.x2);
        return this.packBuffer(bf);
    }
}
