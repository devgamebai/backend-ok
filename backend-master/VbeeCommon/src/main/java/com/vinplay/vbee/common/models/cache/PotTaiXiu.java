/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import com.vinplay.vbee.common.models.cache.TransactionBetTX;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PotTaiXiu
implements Serializable {
    private static final long serialVersionUID = 1L;
    private long totalMoney;
    private List<TransactionBetTX> transBetList = new ArrayList<TransactionBetTX>();
    private Map<String, Long> usersBet = new HashMap<String, Long>();

    public long getTotalMoney() {
        return this.totalMoney;
    }

    public void setTotalMoney(long totalMoney) {
        this.totalMoney = totalMoney;
    }

    public List<TransactionBetTX> getTransBetList() {
        return this.transBetList;
    }

    public void setTransBetList(List<TransactionBetTX> transBetList) {
        this.transBetList = transBetList;
    }

    public Map<String, Long> getUsersBet() {
        return this.usersBet;
    }

    public void addBet(TransactionBetTX tran) {
        this.totalMoney += tran.getBetValue();
        this.transBetList.add(tran);
        if (!this.usersBet.containsKey(tran.getNickname())) {
            this.usersBet.put(tran.getNickname(), tran.getBetValue());
        } else {
            long totalBet = this.usersBet.get(tran.getNickname());
            this.usersBet.put(tran.getNickname(), totalBet + tran.getBetValue());
        }
    }

    public long getTotalBetByNickname(String nickname) {
        if (this.usersBet.containsKey(nickname)) {
            return this.usersBet.get(nickname);
        }
        return 0L;
    }
}

