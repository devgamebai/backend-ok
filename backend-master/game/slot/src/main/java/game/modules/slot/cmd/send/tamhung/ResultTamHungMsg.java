package game.modules.slot.cmd.send.tamhung;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ResultTamHungMsg extends BaseMsg {
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

     public ResultTamHungMsg() {
          super((short)15001);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putLong(this.referenceId);
          bf.put(this.result);
          this.putStr(bf, this.matrix);
          this.putStr(bf, this.linesWin);
          this.putStr(bf, this.haiSao);
          bf.putLong(this.prize);
          bf.putLong(this.currentMoney);
          bf.put(this.freeSpin);
          this.putBoolean(bf, this.isFreeSpin);
          this.putStr(bf, this.itemsWild);
          bf.put(this.ratioFree);
          return this.packBuffer(bf);
     }
}
