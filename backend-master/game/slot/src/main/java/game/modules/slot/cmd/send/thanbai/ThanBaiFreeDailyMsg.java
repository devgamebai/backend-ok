package game.modules.slot.cmd.send.thanbai;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ThanBaiFreeDailyMsg extends BaseMsg {
     public byte remain = 0;

     public ThanBaiFreeDailyMsg() {
          super((short)17012);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.put(this.remain);
          return this.packBuffer(bf);
     }
}
