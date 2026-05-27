package game.modules.slot.cmd.send.ndv;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class MinimizeResultNDVMsg extends BaseMsg {
     public byte result;
     public long prize;
     public long curretMoney;

     public MinimizeResultNDVMsg() {
          super((short)3014);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.result);
          this.putLong(bf, this.prize);
          this.putLong(bf, this.curretMoney);
          return this.packBuffer(bf);
     }
}
