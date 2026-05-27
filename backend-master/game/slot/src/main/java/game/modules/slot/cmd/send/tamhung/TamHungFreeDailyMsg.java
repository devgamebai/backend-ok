package game.modules.slot.cmd.send.tamhung;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class TamHungFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public TamHungFreeDailyMsg() {
          super((short)15012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
