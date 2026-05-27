package game.modules.slot.cmd.send.thanbai;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class ThanBaiTotalFreeSpin extends BaseMsg {
     public int prize;
     public byte ratio;

     public ThanBaiTotalFreeSpin() {
          super((short)17011);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          bf.putInt(this.prize);
          bf.put(this.ratio);
          return super.createData();
     }
}
