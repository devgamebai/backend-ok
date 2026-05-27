/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.xocdia.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.xocdia.entities.GamePot;
import game.xocdia.entities.RewardModel;
import game.xocdia.entities.SubBanker;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class ResultMsg
extends BaseMsg {
    public Vector<GamePot> potList;
    public List<Integer> dinces;
    public long moneyBankerBefore;
    public long moneyBankerAfter;
    public long moneyBankerExchange;
    public Map<String, RewardModel> rewardMap;
    public List<SubBanker> subListMsg;
    public long jackpot;

    public ResultMsg() {
        super((short)3112);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        for (GamePot pot : this.potList) {
            bf.put(pot.id);
            bf.putLong(pot.totalMoney);
            this.putBoolean(bf, Boolean.valueOf(pot.isWin));
        }
        Iterator<Integer> iterator = this.dinces.iterator();
        while (iterator.hasNext()) {
            int i = (Integer)iterator.next();
            bf.putInt(i);
        }
        bf.putLong(this.moneyBankerBefore);
        bf.putLong(this.moneyBankerAfter);
        bf.putLong(this.moneyBankerExchange);
        bf.putInt(this.rewardMap.size());
        for (Map.Entry entry : this.rewardMap.entrySet()) {
            this.putStr(bf, (String)entry.getKey());
            bf.putLong(((RewardModel)entry.getValue()).moneyWin);
            bf.putLong(((RewardModel)entry.getValue()).currentMoney);
            this.putStr(bf, ((RewardModel)entry.getValue()).potsWin);
            this.putStr(bf, ((RewardModel)entry.getValue()).moneyWinPots);
        }
        bf.putInt(this.subListMsg.size());
        for (SubBanker subBanker : this.subListMsg) {
            this.putStr(bf, subBanker.nickname);
            bf.put(subBanker.potId);
            bf.putLong(subBanker.money);
            bf.putLong(subBanker.moneyNoFee);
            bf.putLong(subBanker.currentMoney);
        }
        bf.putLong(this.jackpot);
        return this.packBuffer(bf);
    }
}

