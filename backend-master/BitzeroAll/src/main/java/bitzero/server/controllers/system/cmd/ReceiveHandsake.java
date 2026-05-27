package bitzero.server.controllers.system.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class ReceiveHandsake extends BaseCmd {
     public String sessionToken = null;

     public ReceiveHandsake(DataCmd dataCmd) {
          super(dataCmd);
     }

     public void unpackData() {
          ByteBuffer bf = this.makeBuffer();

          try {
               this.sessionToken = this.readString(bf);
          } catch (Exception var3) {
          }

     }
}
