package game.modules.slot.cmd.send.audition;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class AuditionFreeDailyMsg extends BaseMsg {
    public byte remain = 0;

    public AuditionFreeDailyMsg() {
        super((short) 11012);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.remain);
        return this.packBuffer(bf);
    }
}
