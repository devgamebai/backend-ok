package game.modules.slot.cmd.send.dragonball;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class DragonBallFreeDailyMsg extends BaseMsg {
    public byte remain = 0;

    public DragonBallFreeDailyMsg() {
        super((short) 9012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}
