package game.modules.slot.cmd.send.galaxy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class GalaxyFreeDailyMsg extends BaseMsg {
    public byte remain = 0;

    public GalaxyFreeDailyMsg() {
        super((short) 8012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}
