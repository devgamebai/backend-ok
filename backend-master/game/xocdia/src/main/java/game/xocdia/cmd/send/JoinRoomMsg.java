/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 *  game.entities.PlayerInfo
 *  game.modules.gameRoom.entities.GameMoneyInfo
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.entities.PlayerInfo;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.xocdia.entities.GamePlayer;
import game.xocdia.entities.GamePot;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class JoinRoomMsg
extends BaseMsg {
    public int moneyBet;
    public int roomId;
    public int gameId;
    public byte moneyType;
    public byte gameState;
    public int countTime;
    public Vector<GamePot> potList;
    public Map<String, GamePlayer> playerList;
    public GamePlayer me;
    public int purchaseStatus;
    public int potPurchase;
    public long moneyPurchaseEven;
    public long moneyPurchaseOdd;
    public long moneyRemain;
    public Map<String, Long> subBankerList;
    public boolean bankerReqDestroy;
    public boolean bossReqDestroy;
    public int roomType;

    public JoinRoomMsg() {
        super((short)3101);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.moneyBet);
        bf.putInt(this.roomId);
        bf.putInt(this.gameId);
        bf.put(this.moneyType);
        bf.put(this.gameState);
        bf.putInt(this.countTime);
        bf.put((byte)(this.playerList.size() - 1));
        for (GamePot pot : this.potList) {
            bf.put(pot.id);
            bf.putInt((int)Math.round(pot.ratio * 100.0));
            bf.putLong(pot.maxMoneyBet);
            bf.putLong(pot.totalMoney);
            long moneyBet = pot.betMap.containsKey(this.me.gameMoneyInfo.nickName) ? pot.betMap.get(this.me.gameMoneyInfo.nickName) : 0L;
            bf.putLong(moneyBet);
            this.putBoolean(bf, Boolean.valueOf(pot.isLock));
        }
        for (Map.Entry entry : this.playerList.entrySet()) {
            if (((String)entry.getKey()).equals(this.me.gameMoneyInfo.nickName)) continue;
            GamePlayer gp = (GamePlayer)entry.getValue();
            PlayerInfo pInfo = gp.pInfo;
            this.putStr(bf, pInfo.nickName);
            this.putStr(bf, pInfo.avatarUrl);
            bf.putLong(gp.getMoneyUseInGame());
            this.putBoolean(bf, Boolean.valueOf(gp.isBanker));
            this.putBoolean(bf, Boolean.valueOf(gp.isSubBanker));
            this.putBoolean(bf, Boolean.valueOf(gp.reqKickRoom));
        }
        bf.putLong(this.me.getMoneyUseInGame());
        this.putBoolean(bf, Boolean.valueOf(this.me.isBanker));
        this.putBoolean(bf, Boolean.valueOf(this.me.isSubBanker));
        bf.putInt(this.purchaseStatus);
        bf.putInt(this.potPurchase);
        bf.putLong(this.moneyPurchaseEven);
        bf.putLong(this.moneyPurchaseOdd);
        bf.putLong(this.moneyRemain);
        bf.putInt(this.subBankerList.size());
        for (Map.Entry entry : this.subBankerList.entrySet()) {
            this.putStr(bf, (String)entry.getKey());
            bf.putLong((Long)entry.getValue());
        }
        this.putBoolean(bf, Boolean.valueOf(this.bankerReqDestroy));
        this.putBoolean(bf, Boolean.valueOf(this.bossReqDestroy));
        bf.putInt(this.roomType);
        return this.packBuffer(bf);
    }
}

