package game.modules.slot.cmd.send.thantai;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ThanTaiFreeDailyMsg extends BaseMsg {
    public byte remain = 0;

    public ThanTaiFreeDailyMsg() {
        super((short) 7012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}
