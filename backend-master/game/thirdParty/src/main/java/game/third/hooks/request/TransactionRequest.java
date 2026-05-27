/*
 * Decompiled with CFR 0.152.
 */
package game.third.hooks.request;

import java.util.List;

public class TransactionRequest {
    private String memberAccount;
    private String operatorCode;
    private String productCode;
    private String currency;
    private String rollbackTxId;
    private List<Transaction> transactions;
    private String sign;
    private String requestTime;

    public String getMemberAccount() {
        return this.memberAccount;
    }

    public void setMemberAccount(String memberAccount) {
        this.memberAccount = memberAccount;
    }

    public String getOperatorCode() {
        return this.operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getProductCode() {
        return this.productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRollbackTxId() {
        return this.rollbackTxId;
    }

    public void setRollbackTxId(String rollbackTxId) {
        this.rollbackTxId = rollbackTxId;
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getRequestTime() {
        return this.requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    public static class Transaction {
        private String id;
        private String action;
        private String wagerCode;
        private String wagerStatus;
        private String amount;
        private String betAmount;
        private String validBetAmount;
        private String prizeAmount;
        private String tipAmount;
        private long settledAt;
        private String gameCode;

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAction() {
            return this.action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getWagerCode() {
            return this.wagerCode;
        }

        public void setWagerCode(String wagerCode) {
            this.wagerCode = wagerCode;
        }

        public String getWagerStatus() {
            return this.wagerStatus;
        }

        public void setWagerStatus(String wagerStatus) {
            this.wagerStatus = wagerStatus;
        }

        public String getAmount() {
            return this.amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getBetAmount() {
            return this.betAmount;
        }

        public void setBetAmount(String betAmount) {
            this.betAmount = betAmount;
        }

        public String getValidBetAmount() {
            return this.validBetAmount;
        }

        public void setValidBetAmount(String validBetAmount) {
            this.validBetAmount = validBetAmount;
        }

        public String getPrizeAmount() {
            return this.prizeAmount;
        }

        public void setPrizeAmount(String prizeAmount) {
            this.prizeAmount = prizeAmount;
        }

        public String getTipAmount() {
            return this.tipAmount;
        }

        public void setTipAmount(String tipAmount) {
            this.tipAmount = tipAmount;
        }

        public long getSettledAt() {
            return this.settledAt;
        }

        public void setSettledAt(long settledAt) {
            this.settledAt = settledAt;
        }

        public String getGameCode() {
            return this.gameCode;
        }

        public void setGameCode(String gameCode) {
            this.gameCode = gameCode;
        }
    }
}

