package game.coup.server.cmd.receive;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;

public class RevDongYThachDau extends BaseCmd {
   public boolean dongYThachDau = false;
   public String enemy;
   public int moneyBet;

   public RevDongYThachDau(DataCmd dataCmd) {
      super(dataCmd);
      this.unpackData();
   }

   public void unpackData() {
      ByteBuffer bf = this.makeBuffer();
      this.dongYThachDau = this.readBoolean(bf);
      this.enemy = this.readString(bf);
      this.moneyBet = this.readInt(bf);
   }
}
