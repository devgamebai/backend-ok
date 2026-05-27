/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 */
package game.modules.minigame.entities;

import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import game.modules.minigame.entities.Pot;
import java.util.ArrayList;
import java.util.List;

public class PotTaiXiu
extends Pot {
    private long totalBotBet = 0L;
    private int numBot = 0;
    public List<TransactionTaiXiuDetail> contributors = new ArrayList<TransactionTaiXiuDetail>();
    public List<String> users = new ArrayList<String>();
    public List<String> realUsers = new ArrayList<String>();
    private UserService userService = new UserServiceImpl();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void bet(TransactionTaiXiuDetail trans, boolean isBot) {
        List<TransactionTaiXiuDetail> list2 = this.contributors;
        synchronized (list2) {
            this.contributors.add(trans);
        }
        List<String> list3 = this.users;
        synchronized (list3) {
            if (!this.users.contains(trans.username)) {
                this.users.add(trans.username);
            }
        }
        this.totalValue += trans.betValue;
        if (isBot) {
            this.totalBotBet += trans.betValue;
            ++this.numBot;
        } else {
            this.realUsers.add(trans.username);
        }
    }

    @Override
    public void renew() {
        super.renew();
        this.contributors.clear();
        this.users.clear();
        this.realUsers.clear();
        this.totalBotBet = 0L;
        this.numBot = 0;
    }

    public long getTotalBetByUsername(String username) {
        long totalValue = 0L;
        for (TransactionTaiXiuDetail tran : this.contributors) {
            if (!tran.username.equals(username)) continue;
            totalValue += tran.betValue;
        }
        return totalValue;
    }

    public long getRealUserBet() {
        return this.getTotalValue() - this.getTotalBotBet();
    }

    public short getNumBet() {
        return (short)this.users.size();
    }

    public long getTotalBotBet() {
        return this.totalBotBet;
    }

    public int getNumBotBet() {
        return this.numBot;
    }

    public boolean hasBet(String userName) {
        return this.realUsers.contains(userName);
    }

    public List<TransactionTaiXiuDetail> getRealContributors() {
        ArrayList<TransactionTaiXiuDetail> items = new ArrayList<TransactionTaiXiuDetail>();
        for (TransactionTaiXiuDetail i : this.contributors) {
            if (this.isBot(i.username)) continue;
            items.add(i);
        }
        return items;
    }

    public boolean isBot(String username) {
        // SUN-1xxx (2026-05-11): null-safe — see MGRoomTaiXiu.isBot for rationale.
        // getRealContributors() iterates this on every contributor and any NPE
        // here aborts the whole list, dropping real-player bets silently from
        // settlement-time aggregations.
        try {
            UserCacheModel model = this.userService.getUser(username);
            if (model == null) return false;
            return model.isBot();
        } catch (Throwable t) {
            return false;
        }
    }
}

