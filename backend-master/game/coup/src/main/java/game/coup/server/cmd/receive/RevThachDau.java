package game.coup.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevThachDau extends BaseCmd {
   public String enemy;
   public int moneyBet;

   public RevThachDau(DataCmd dataCmd) {
      super(dataCmd);
      this.unpackData();
   }

   public void unpackData() {
      ByteBuffer bf = this.makeBuffer();
      this.enemy = this.readString(bf);
      this.moneyBet = this.readInt(bf);
   }
}
