package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.coup.server.GamePlayer;
import game.entities.PlayerInfo;
import java.nio.ByteBuffer;

public class SendReconnectInfo extends BaseMsg {
   public int maxUserPerRoom;
   public byte chair;
   public String[][] map;
   public byte[][] statusMap;
   public int gameState;
   public int gameAction;
   public int currentChair;
   public int countdownTime;
   public int moneyType;
   public long roomBet;
   public int gameId;
   public int roomId;
   public byte[] lastMove = new byte[2];
   public String[] diePieces;
   public boolean[] hasInfoAtChair = new boolean[20];
   public GamePlayer[] pInfos = new GamePlayer[20];

   public SendReconnectInfo() {
      super((short)3124);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      bf.put((byte)this.maxUserPerRoom);
      bf.put(this.chair);
      bf.putShort((short)this.map.length);

      int i;
      for(i = 0; i < this.map.length; ++i) {
         this.putStringArray(bf, this.map[i]);
      }

      bf.put((byte)this.gameState);
      bf.put((byte)this.gameAction);
      bf.put((byte)this.countdownTime);
      bf.put((byte)this.currentChair);
      bf.put((byte)this.moneyType);
      this.putLong(bf, this.roomBet);
      bf.putInt(this.gameId);
      bf.putInt(this.roomId);
      this.putBooleanArray(bf, this.hasInfoAtChair);

      for(i = 0; i < 20; ++i) {
         if (this.hasInfoAtChair[i]) {
            GamePlayer gp = this.pInfos[i];
            bf.put((byte)gp.playerStatus);
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.avatarUrl);
            this.putStr(bf, pInfo.nickName);
            if (gp.gameMoneyInfo != null) {
               this.putLong(bf, gp.gameMoneyInfo.currentMoney);
            } else {
               this.putLong(bf, 0L);
            }

            bf.put((byte)gp.gameChair);
            bf.put((byte)gp.spInfo.chessColor);
            bf.putInt(gp.spInfo.gameTime);
         }
      }

      this.putByteArray(bf, this.lastMove);
      bf.putShort((short)this.statusMap.length);

      for(i = 0; i < this.statusMap.length; ++i) {
         this.putByteArray(bf, this.statusMap[i]);
      }

      this.putStringArray(bf, this.diePieces);
      return this.packBuffer(bf);
   }

   public void initPrivateInfo(GamePlayer gp) {
      this.chair = (byte)gp.chair;
   }
}
