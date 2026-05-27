package game.modules.slot.cmd.send.audition;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class UpdatePotAuditionMsg extends BaseMsg {
    public long value;
    public byte x2 = 0;

    public UpdatePotAuditionMsg() {
        super((short) 11002);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putLong(this.value);
        bf.put(this.x2);
        return this.packBuffer(bf);
    }
}
