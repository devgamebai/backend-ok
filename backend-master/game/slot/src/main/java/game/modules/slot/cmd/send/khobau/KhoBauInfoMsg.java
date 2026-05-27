package game.modules.slot.cmd.send.khobau;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class KhoBauInfoMsg extends BaseMsg {
     public String ngayX2;
     public byte remain;
     public long currentMoney;

     public KhoBauInfoMsg() {
          super((short)2011);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          bf.putLong(this.currentMoney);
          this.putStr(bf, this.ngayX2);
          return this.packBuffer(bf);
     }
}
