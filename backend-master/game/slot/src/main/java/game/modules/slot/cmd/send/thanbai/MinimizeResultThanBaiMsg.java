package game.modules.slot.cmd.send.thanbai;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class MinimizeResultThanBaiMsg extends BaseMsg {
     public byte result;
     public long prize;
     public long curretMoney;

     public MinimizeResultThanBaiMsg() {
          super((short)17014);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.result);
          this.putLong(bf, this.prize);
          this.putLong(bf, this.curretMoney);
          return this.packBuffer(bf);
     }
}
