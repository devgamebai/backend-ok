package game.modules.cluster.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class SampleLogin extends BaseCmd {
     public String ip;
     public int port;
     public int userId;

     public SampleLogin(DataCmd dataCmd) {
          super(dataCmd);
          this.unpackData();
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();
          this.ip = this.readString(bf);
          this.port = this.readInt(bf);
          this.userId = this.readInt(bf);
     }
}
