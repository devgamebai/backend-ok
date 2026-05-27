package game.modules.slot.cmd.send.dragonball;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class DragonBallTotalFreeSpin extends BaseMsg {
    public int prize;
    public byte ratio;

    public DragonBallTotalFreeSpin() {
        super((short) 9011);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.prize);
        bf.put(this.ratio);
        return super.createData();
    }
}
