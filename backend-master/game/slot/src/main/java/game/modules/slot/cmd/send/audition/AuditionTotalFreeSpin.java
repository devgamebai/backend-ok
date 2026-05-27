package game.modules.slot.cmd.send.audition;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class AuditionTotalFreeSpin extends BaseMsg {
    public int prize;
    public byte ratio;

    public AuditionTotalFreeSpin() {
        super((short) 11011);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.put(this.ratio);
        return super.createData();
    }
}
