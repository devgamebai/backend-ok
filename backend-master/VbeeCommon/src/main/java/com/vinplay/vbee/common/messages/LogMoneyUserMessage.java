/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.messages;

import com.vinplay.vbee.common.messages.BaseMessage;

public class LogMoneyUserMessage
extends BaseMessage {
    private static final long serialVersionUID = 1L;
    private int userId;
    private String nickname;
    private String actionName;
    private String serviceName;
    private long currentMoney;
    private long moneyExchange;
    private String moneyType;
    private String description;
    private long fee;
    private boolean vp;
    private boolean isBot;
    private String referralCode;
    /**
     * SUN-1205/1206: optional override for the commission volume.
     *
     * <p>When > 0, the rebate pipeline ({@code LogMoneyUserExtraProcessor})
     * uses this value as the bet volume instead of {@code abs(moneyExchange)}.
     * Useful for hedge bets (e.g. Baccarat banker+player same-amount) where
     * the player actually committed {@code moneyExchange} in money but only
     * a smaller {@code valid_bet_amount} should pay commission. Money flow
     * (vin deduction) is unaffected — this only changes the volume seen by
     * the rebate distribution chain.
     *
     * <p>Default 0 (= "use abs(moneyExchange)"). All legacy callers leave
     * it at 0 and behave identically to pre-SUN-1205.
     */
    private long validBetAmount;

    /**
     * SUN-1248 — round-level identifier for ledger-style rebate aggregation.
     * For GSC bets, set to the wager_code from the seamless-wallet payload.
     * Multi-bet rounds (Evolution Baccarat where the player adds to their
     * bet during the round) emit one message per sub-bet, all carrying the
     * same {@code wagerCode}. {@code RebateService.queryLogs} GROUPs BY
     * this column so Rolling shows ONE row per round to the operator
     * regardless of how many sub-bets were placed.
     *
     * <p>Default null (legacy callers from non-GSC games leave it unset).
     */
    private String wagerCode;

    public LogMoneyUserMessage(int userId, String nickname, String actionName, String serviceName, long currentMoney, long moneyExchange, String moneyType, String description, long fee, boolean vp, boolean isBot) {
        this.userId = userId;
        this.nickname = nickname;
        this.actionName = actionName;
        this.serviceName = serviceName;
        this.currentMoney = currentMoney;
        this.moneyExchange = moneyExchange;
        this.moneyType = moneyType;
        this.description = description;
        this.fee = fee;
        this.vp = vp;
        this.isBot = isBot;
    }

    public long getValidBetAmount() { return this.validBetAmount; }
    public void setValidBetAmount(long validBetAmount) { this.validBetAmount = validBetAmount; }

    public boolean isBot() {
        return this.isBot;
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public boolean isVp() {
        return this.vp;
    }

    public void setVp(boolean vp) {
        this.vp = vp;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getActionName() {
        return this.actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public long getCurrentMoney() {
        return this.currentMoney;
    }

    public void setCurrentMoney(long currentMoney) {
        this.currentMoney = currentMoney;
    }

    public long getMoneyExchange() {
        return this.moneyExchange;
    }

    public void setMoneyExchange(long moneyExchange) {
        this.moneyExchange = moneyExchange;
    }

    public String getMoneyType() {
        return this.moneyType;
    }

    public void setMoneyType(String moneyType) {
        this.moneyType = moneyType;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getFee() {
        return this.fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getReferralCode() { return this.referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public String getWagerCode() { return this.wagerCode; }
    public void setWagerCode(String wagerCode) { this.wagerCode = wagerCode; }
}

