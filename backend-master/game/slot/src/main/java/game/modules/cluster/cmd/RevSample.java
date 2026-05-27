package game.modules.cluster.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevSample extends BaseCmd {
     public String str;
     public boolean bo;
     public byte by;
     public short sh;
     public int d;
     public long l;

     public RevSample(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.str = this.readString(bf);
          this.bo = this.readBoolean(bf);
          this.by = this.readByte(bf);
          this.sh = this.readShort(bf);
          this.d = this.readInt(bf);
          this.l = this.readLong(bf);
     }
}
