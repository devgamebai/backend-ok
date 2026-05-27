/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.jboss.netty.util.internal.ConcurrentHashMap
 *  org.json.JSONObject
 */
package game.xocdia.entities;

import game.xocdia.entities.BettingModel;
import game.xocdia.entities.RefundModel;
import game.xocdia.utils.MsgUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

public class GamePot {
    public byte id;
    public double ratio;
    public String name;
    public long maxMoneyBet;
    public BlockingDeque<BettingModel> betList;
    public Map<String, Long> betMap;
    public Map<String, RefundModel> refundMap;
    public long moneyRefund;
    public long totalMoney;
    public boolean isWin;
    public boolean isLock;
    public boolean regisChangeLock;
    public long totalMoneyBotBet;
    public long totalMoneyUserBet;
    public long totalMoneyBotBuy;
    public long totalMoneyUserBuy;

    public GamePot(byte id, double ratio, String name, long maxMoneyBet) {
        this.id = id;
        this.ratio = ratio;
        this.name = name;
        this.maxMoneyBet = maxMoneyBet;
        this.betList = new LinkedBlockingDeque<BettingModel>();
        this.betMap = new ConcurrentHashMap();
        this.refundMap = new HashMap<String, RefundModel>();
        this.moneyRefund = 0L;
        this.totalMoney = 0L;
        this.isWin = false;
        this.isLock = false;
        this.regisChangeLock = false;
        this.totalMoneyBotBet = 0L;
        this.totalMoneyUserBet = 0L;
        this.totalMoneyBotBuy = 0L;
        this.totalMoneyUserBuy = 0L;
    }

    public void reset() {
        this.betList.clear();
        this.betMap.clear();
        this.refundMap.clear();
        this.moneyRefund = 0L;
        this.totalMoney = 0L;
        this.isWin = false;
        if (this.regisChangeLock) {
            this.isLock = !this.isLock;
        }
        this.regisChangeLock = false;
        this.totalMoneyBotBet = 0L;
        this.totalMoneyUserBet = 0L;
        this.totalMoneyBotBuy = 0L;
        this.totalMoneyUserBuy = 0L;
    }

    public boolean checkBetLimitUser(String nickname, boolean bLimit) {
        if (bLimit) {
            long moneyUserBet;
            long l = moneyUserBet = this.betMap.containsKey(nickname) ? this.betMap.get(nickname) : 0L;
            if (moneyUserBet >= this.maxMoneyBet) {
                return true;
            }
        }
        return false;
    }

    public long bet(String nickname, long money, boolean bLimit, int moneyType, boolean isBot) {
        try {
            if (bLimit) {
                long moneyUserBet;
                long l = moneyUserBet = this.betMap.containsKey(nickname) ? this.betMap.get(nickname) : 0L;
                if (moneyUserBet < this.maxMoneyBet) {
                    if (moneyUserBet + money > this.maxMoneyBet) {
                        money = this.maxMoneyBet - moneyUserBet;
                    }
                } else {
                    money = 0L;
                }
            }
            if (money > 0L) {
                this.betList.offer(new BettingModel(nickname, money, isBot));
                if (this.betMap.containsKey(nickname)) {
                    this.betMap.put(nickname, this.betMap.get(nickname) + money);
                } else {
                    this.betMap.put(nickname, money);
                }
                this.totalMoney += money;
                if (isBot) {
                    this.totalMoneyBotBet += money;
                } else {
                    this.totalMoneyUserBet += money;
                }
            }
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: GamePot.bet()";
            MsgUtils.alertServer(content, false, true);
        }
        return money;
    }

    public long getMoneyBet(String nickname) {
        if (this.betMap.containsKey(nickname)) {
            return this.betMap.get(nickname);
        }
        return 0L;
    }

    public long getMoneyX2(String nickname, boolean bLimit) {
        if (this.betMap.containsKey(nickname)) {
            if (bLimit) {
                if (this.betMap.get(nickname) < this.maxMoneyBet) {
                    if (this.betMap.get(nickname) * 2L > this.maxMoneyBet) {
                        return this.maxMoneyBet - this.betMap.get(nickname);
                    }
                    return this.betMap.get(nickname);
                }
            } else {
                return this.betMap.get(nickname);
            }
        }
        return 0L;
    }

    public void refund(long moneyDif, int moneyType) {
        try {
            long money = 0L;
            int size = this.betList.size();
            for (int i = 0; i < size; ++i) {
                BettingModel model = (BettingModel)this.betList.pollLast();
                if (model == null) continue;
                if ((money += model.money) < moneyDif) {
                    this.refundPot(model.nickname, model.money, moneyType, model.isBot);
                    continue;
                }
                if (money == moneyDif) {
                    this.refundPot(model.nickname, model.money, moneyType, model.isBot);
                    break;
                }
                long moneyRefund = moneyDif - (money -= model.money);
                long moneyBetNew = model.money - moneyRefund;
                this.betList.offer(new BettingModel(model.nickname, moneyBetNew, model.isBot));
                this.refundPot(model.nickname, moneyRefund, moneyType, model.isBot);
                break;
            }
            this.totalMoney -= moneyDif;
            this.moneyRefund = moneyDif;
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: GamePot.refund()";
            MsgUtils.alertServer(content, false, true);
        }
    }

    public void refundPot(String nickname, long money, int moneyType, boolean isBot) {
        try {
            if (isBot) {
                this.totalMoneyBotBet -= money;
            } else {
                this.totalMoneyUserBet -= money;
            }
            if (this.betMap.containsKey(nickname)) {
                this.betMap.put(nickname, this.betMap.get(nickname) - money);
            }
            RefundModel model = null;
            if (this.refundMap.containsKey(nickname)) {
                model = this.refundMap.get(nickname);
                model.moneyRefund += money;
            } else {
                model = new RefundModel();
                model.moneyRefund = money;
            }
            this.refundMap.put(nickname, model);
        }
        catch (Exception e) {
            String content = "Xoc Dia exception: " + e.getMessage() + ", function: GamePot.refundPot()";
            MsgUtils.alertServer(content, false, true);
        }
    }

    public void buyPot(long moneyBuy, boolean isBot) {
        if (isBot) {
            this.totalMoneyBotBuy += moneyBuy;
        } else {
            this.totalMoneyUserBuy += moneyBuy;
        }
    }

    public long getMoneyBuyRemain(boolean isBankerIsBot, int roomId, int gameId) {
        long moneyBuyRemain = 0L;
        if (isBankerIsBot) {
            if (this.totalMoneyUserBet >= this.totalMoneyUserBuy) {
                this.totalMoneyUserBet -= this.totalMoneyUserBuy;
            } else {
                this.totalMoneyUserBet = 0L;
                moneyBuyRemain = this.totalMoneyUserBuy - this.totalMoneyUserBet;
            }
        } else if (this.totalMoneyBotBet >= this.totalMoneyBotBuy) {
            this.totalMoneyBotBet -= this.totalMoneyBotBuy;
        } else {
            this.totalMoneyBotBet = 0L;
            moneyBuyRemain = this.totalMoneyBotBuy - this.totalMoneyBotBet;
        }
        return moneyBuyRemain;
    }

    public void addBetMoney(long money, boolean isBankerIsBot, int roomId, int gameId) {
        if (!isBankerIsBot) {
            this.totalMoneyBotBet += money;
        } else {
            this.totalMoneyUserBet += money;
        }
    }

    public String toString() {
        try {
            JSONObject json = this.toJSONObject();
            if (json != null) {
                return json.toString();
            }
            return "{}";
        }
        catch (Exception e) {
            return "{}";
        }
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject json = new JSONObject();
            json.put("ratio", this.ratio);
            json.put("maxMoneyBet", this.maxMoneyBet);
            json.put("moneyRefund", this.moneyRefund);
            json.put("totalMoney", this.totalMoney);
            json.put("isWin", this.isWin);
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}

