/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonSyntaxException
 */
package game.third.hooks.gscSeamless.request;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.List;

public class CancelRequest {
    private String operator_code;
    private String currency;
    private List<Transaction> transactions;
    private String sign;
    private String request_time;

    public CancelRequest(String operator_code, String currency, List<Transaction> transactions, String sign, String request_time) {
        this.operator_code = operator_code;
        this.currency = currency;
        this.transactions = transactions;
        this.sign = sign;
        this.request_time = request_time;
    }

    public CancelRequest() {
    }

    public String getOperator_code() {
        return this.operator_code;
    }

    public void setOperator_code(String operator_code) {
        this.operator_code = operator_code;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getRequest_time() {
        return this.request_time;
    }

    public void setRequest_time(String request_time) {
        this.request_time = request_time;
    }

    public static CancelRequest fromJson(String json) {
        Gson gson = new Gson();
        try {
            return (CancelRequest)gson.fromJson(json, CancelRequest.class);
        }
        catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Transaction {
        private String member_account;
        private String wager_code;
        private String wager_status;
        private String wager_type;
        private String product_code;
        private double bet_amount;
        private double valid_bet_amount;
        private double prize_amount;
        private double tip_amount;
        private String game_type;
        private long settled_at;

        public Transaction() {
        }

        public Transaction(String member_account, String wager_code, String wager_status, String wager_type, String product_code, double bet_amount, double valid_bet_amount, double prize_amount, double tip_amount, String game_type, long settled_at) {
            this.member_account = member_account;
            this.wager_code = wager_code;
            this.wager_status = wager_status;
            this.wager_type = wager_type;
            this.product_code = product_code;
            this.bet_amount = bet_amount;
            this.valid_bet_amount = valid_bet_amount;
            this.prize_amount = prize_amount;
            this.tip_amount = tip_amount;
            this.game_type = game_type;
            this.settled_at = settled_at;
        }

        public String getMember_account() {
            return this.member_account;
        }

        public void setMember_account(String member_account) {
            this.member_account = member_account;
        }

        public String getWager_code() {
            return this.wager_code;
        }

        public void setWager_code(String wager_code) {
            this.wager_code = wager_code;
        }

        public String getWager_status() {
            return this.wager_status;
        }

        public void setWager_status(String wager_status) {
            this.wager_status = wager_status;
        }

        public String getWager_type() {
            return this.wager_type;
        }

        public void setWager_type(String wager_type) {
            this.wager_type = wager_type;
        }

        public String getProduct_code() {
            return this.product_code;
        }

        public void setProduct_code(String product_code) {
            this.product_code = product_code;
        }

        public double getBet_amount() {
            return this.bet_amount;
        }

        public void setBet_amount(double bet_amount) {
            this.bet_amount = bet_amount;
        }

        public double getValid_bet_amount() {
            return this.valid_bet_amount;
        }

        public void setValid_bet_amount(double valid_bet_amount) {
            this.valid_bet_amount = valid_bet_amount;
        }

        public double getPrize_amount() {
            return this.prize_amount;
        }

        public void setPrize_amount(double prize_amount) {
            this.prize_amount = prize_amount;
        }

        public double getTip_amount() {
            return this.tip_amount;
        }

        public void setTip_amount(double tip_amount) {
            this.tip_amount = tip_amount;
        }

        public String getGame_type() {
            return this.game_type;
        }

        public void setGame_type(String game_type) {
            this.game_type = game_type;
        }

        public long getSettled_at() {
            return this.settled_at;
        }

        public void setSettled_at(long settled_at) {
            this.settled_at = settled_at;
        }
    }
}

