package game.modules.slot.cmd.send.thanbai;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdatePotThanBaiMsg extends BaseMsg {
     public long value;
     public byte x2 = 0;

     public UpdatePotThanBaiMsg() {
          super((short)17002);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putLong(this.value);
          bf.put(this.x2);
          return this.packBuffer(bf);
     }
}
