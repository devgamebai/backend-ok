package game.modules.slot.cmd.send.audition;

import bitzero.server.extensions.data.BaseMsg;

import java.nio.ByteBuffer;

public class AuditionInfoMsg extends BaseMsg {
    public String ngayX2;
    public byte remain;
    public long currentMoney;
    public byte freeSpin;
    public String lines = "";

    public AuditionInfoMsg() {
        super((short) 11009);
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
