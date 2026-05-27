package game.modules.slot.cmd.send.galaxy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class GalaxyTotalFreeSpin extends BaseMsg {
    public int prize;
    public byte ratio;

    public GalaxyTotalFreeSpin() {
        super((short) 8011);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.put(this.ratio);
        return super.createData();
    }
}
