package game.modules.slot.cmd.send.candy;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ResultCandyMsg extends BaseMsg {
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
     public byte ratioFree = 0;

     public ResultCandyMsg() {
          super((short)9001);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          // Client (Slot3x3) expects: result(1) + matrix(str) + linesWin(str) + prize(8) + currentMoney(8)
          bf.put(this.result);
          this.putStr(bf, this.matrix);
          this.putStr(bf, this.linesWin);
          bf.putLong(this.prize);
          bf.putLong(this.currentMoney);
          return this.packBuffer(bf);
     }
}
