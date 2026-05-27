package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import java.nio.ByteBuffer;

public class SendNewUserJoin extends BaseMsg {
   public long money;
   public String uName;
   public String avtUrl;
   public int uChair;
   public int uStatus;
   public int gameChair;

   public SendNewUserJoin() {
      super((short)3121);
   }

   public void setBaseInfo(PlayerInfo pInfo) {
      this.uName = pInfo.nickName;
      this.avtUrl = pInfo.avatarUrl;
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      this.putStr(bf, this.uName);
      this.putStr(bf, this.avtUrl);
      this.putLong(bf, this.money);
      bf.put((byte)this.gameChair);
      bf.put((byte)this.uChair);
      bf.put((byte)this.uStatus);
      return this.packBuffer(bf);
   }
}
