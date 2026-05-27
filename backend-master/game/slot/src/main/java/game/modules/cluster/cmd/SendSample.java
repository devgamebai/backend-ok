package game.modules.cluster.cmd;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendSample extends BaseMsg {
     public String str;
     public boolean bo;
     public byte by;
     public short sh;
     public int d;
     public long l;

     public SendSample() {
          super((short)1001);
     }

     public byte[] createData() {
          ByteBuffer bf = this.makeBuffer();
          this.putStr(bf, this.str);
          this.putBoolean(bf, this.bo);
          bf.put(this.by);
          bf.putShort(this.sh);
          bf.putInt(this.d);
          this.putLong(bf, this.l);
          return this.packBuffer(bf);
     }

     public void copy(RevSample cmd) {
          this.str = cmd.str;
          this.bo = cmd.bo;
          this.by = cmd.by;
          this.sh = cmd.sh;
          this.d = cmd.d;
          this.l = cmd.l;
     }
}
