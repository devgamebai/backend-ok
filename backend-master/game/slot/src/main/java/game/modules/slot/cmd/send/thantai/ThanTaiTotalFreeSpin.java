package game.modules.slot.cmd.send.thantai;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ThanTaiTotalFreeSpin extends BaseMsg {
    public int prize;
    public byte ratio;

    public ThanTaiTotalFreeSpin() {
        super((short) 7011);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.put(this.ratio);
        return super.createData();
    }
}
