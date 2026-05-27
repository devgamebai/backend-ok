package game.modules.slot.cmd.send.thuycung;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class UpdatePotThuyCungMsg extends BaseMsg {
     public long value;
     public byte x2 = 0;

     public UpdatePotThuyCungMsg() {
          super((short)18002);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putLong(this.value);
          bf.put(this.x2);
          return this.packBuffer(bf);
     }
}
