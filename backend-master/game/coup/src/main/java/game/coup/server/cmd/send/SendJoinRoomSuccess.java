package game.coup.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.coup.server.GamePlayer;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import java.nio.ByteBuffer;

public class SendJoinRoomSuccess extends BaseMsg {
   public int uChair;
   public int comission;
   public int comissionJackpot;
   public long moneyBet;
   public byte roomOwner;
   public int gameId;
   public int roomId;
   public int moneyType;
   public int rule;
   public byte[] playerStatus = new byte[20];
   public GamePlayer[] playerList = new GamePlayer[20];
   public byte gameState;
   public byte gameAction;
   public byte curentChair;
   public byte countDownTime;
   public GameMoneyInfo[] moneyInfoList = new GameMoneyInfo[20];

   public SendJoinRoomSuccess() {
      super((short)3118);
   }

   public SendJoinRoomSuccess(int i) {
      super((short)3118, i);
   }

   public byte[] createData() {
      ByteBuffer bf = this.makeBuffer();
      bf.put((byte)this.uChair);
      bf.putLong(this.moneyBet);
      bf.put(this.roomOwner);
      bf.putInt(this.roomId);
      bf.putInt(this.gameId);
      bf.put((byte)this.moneyType);
      bf.put((byte)this.rule);
      this.putByteArray(bf, this.playerStatus);
      bf.putShort((short)this.playerList.length);

      for(int i = 0; i < this.playerList.length; ++i) {
         GamePlayer gp = this.playerList[i];
         PlayerInfo pInfo = null;
         if (!gp.hasUser()) {
            pInfo = new PlayerInfo();
            bf.put((byte)-1);
            bf.put((byte)-1);
         } else {
            pInfo = gp.pInfo;
            bf.put((byte)this.playerList[i].gameChair);
            bf.put((byte)this.playerList[i].chair);
         }

         this.putStr(bf, pInfo.avatarUrl);
         this.putStr(bf, pInfo.nickName);
         if (this.moneyInfoList[i] == null) {
            bf.putLong(0L);
         } else {
            bf.putLong(this.moneyInfoList[i].currentMoney);
         }
      }

      bf.put(this.gameAction);
      bf.put(this.curentChair);
      bf.put(this.countDownTime);
      bf.put(this.gameState);
      return this.packBuffer(bf);
   }
}
