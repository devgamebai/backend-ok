package game.modules.slot.cmd.send.ndv;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ResultNDVMsg extends BaseMsg {
     public long referenceId;
     public byte result;
     public String matrix = "";
     public String linesWin = "";
     public String haiSao = "";
     public long prize;
     public long currentMoney;

     public ResultNDVMsg() {
          super((short)3001);
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
          return this.packBuffer(bf);
     }
}
