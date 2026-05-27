package game.coup.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevDongYHoa extends BaseCmd {
   public boolean dongYHoa = false;

   public RevDongYHoa(DataCmd data) {
      super(data);
      this.unpackData();
   }

   public void unpackData() {
      ByteBuffer bf = this.makeBuffer();
      this.dongYHoa = this.readBoolean(bf);
   }
}
