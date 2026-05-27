package game.modules.slot.cmd.send.dragonball;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ResultDragonBallMsg extends BaseMsg {
    public long referenceId;
    public byte result;
    public String matrix = "";
    public String linesWin = "";
    public String haiSao = "";
    public long prize;
    public long currentMoney;
    public byte freeSpin = 0;
    public boolean isFreeSpin = false;
    public String itemsWild = "";
    public byte ratio = 0;
    public byte currentNumberFreeSpin = 0;

    public ResultDragonBallMsg() {
        super((short) 9001);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put(this.result);
        this.putStr(bf, this.matrix);
        this.putStr(bf, this.linesWin);
        bf.putLong(this.prize);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}
