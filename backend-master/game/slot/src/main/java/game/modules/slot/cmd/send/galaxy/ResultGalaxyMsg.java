package game.modules.slot.cmd.send.galaxy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ResultGalaxyMsg extends BaseMsg {
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

    public ResultGalaxyMsg() {
        super((short) 8001);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        // Client (Slot3x3Gem) expects: result(1) + matrix(str) + linesWin(str) + prize(8) + currentMoney(8)
        // Do NOT send referenceId, haiSao, freeSpin, isFreeSpin, itemsWild, ratio, currentNumberFreeSpin
        bf.put(this.result);
        this.putStr(bf, this.matrix);
        this.putStr(bf, this.linesWin);
        bf.putLong(this.prize);
        bf.putLong(this.currentMoney);
        return this.packBuffer(bf);
    }
}
