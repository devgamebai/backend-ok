package game.modules.slot.cmd.send.bikini;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class BikiniInfoMsg extends BaseMsg {
    public String ngayX2;
    public byte remain;
    public long currentMoney;
    public byte freeSpin;
    public String lines = "";

    public BikiniInfoMsg() {
        super((short)16009);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putStr(bf, this.ngayX2);
        bf.put(this.remain);
        bf.putLong(this.currentMoney);
        bf.put(this.freeSpin);
        this.putStr(bf, this.lines);
        return this.packBuffer(bf);
    }
}
