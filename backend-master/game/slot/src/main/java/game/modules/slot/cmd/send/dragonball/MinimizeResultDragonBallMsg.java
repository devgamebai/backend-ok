package game.modules.slot.cmd.send.dragonball;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class MinimizeResultDragonBallMsg extends BaseMsg {
    public byte result;
    public long prize;
    public long curretMoney;

    public MinimizeResultDragonBallMsg() {
        super((short) 9014);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.result);
        this.putLong(bf, this.prize);
        this.putLong(bf, this.curretMoney);
        return this.packBuffer(bf);
    }
}
