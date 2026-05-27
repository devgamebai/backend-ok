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

public class DepositRequest {
    private String operator_code;
    private String member_account;
    private int product_code;
    private String currency;
    private String game_type;
    private List<Transaction> transactions;
    private String sign;
    private String request_time;
    private List<BatchRequest> batch_requests;

    public static class BatchRequest {
        private String game_type;
        private String member_account;
        private int product_code;
        private List<Transaction> transactions;
        public String getMember_account() { return member_account; }
        public int getProduct_code() { return product_code; }
        public List<Transaction> getTransactions() { return transactions; }
    }

    public List<BatchRequest> getBatch_requests() { return batch_requests; }

    public String resolveMemberAccount() {
        if (member_account != null) return member_account;
        if (batch_requests != null && !batch_requests.isEmpty()) return batch_requests.get(0).getMember_account();
        return null;
    }
    public int resolveProductCode() {
        if (product_code > 0) return product_code;
        if (batch_requests != null && !batch_requests.isEmpty()) return batch_requests.get(0).getProduct_code();
        return 0;
    }
    public List<Transaction> resolveTransactions() {
        if (transactions != null && !transactions.isEmpty()) return transactions;
        if (batch_requests != null && !batch_requests.isEmpty()) return batch_requests.get(0).getTransactions();
        return null;
    }

    public String getOperator_code() {
        return this.operator_code;
    }

    public void setOperator_code(String operator_code) {
        this.operator_code = operator_code;
    }

    public String getMember_account() {
        return this.member_account;
    }

    public void setMember_account(String member_account) {
        this.member_account = member_account;
    }

    public int getProduct_code() {
        return this.product_code;
    }

    public void setProduct_code(int product_code) {
        this.product_code = product_code;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGame_type() {
        return this.game_type;
    }

    public void setGame_type(String game_type) {
        this.game_type = game_type;
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

    public static DepositRequest fromJson(String json) {
        Gson gson = new Gson();
        try {
            return (DepositRequest)gson.fromJson(json, DepositRequest.class);
        }
        catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Transaction
    extends game.third.hooks.gscSeamless.request.Transaction {
    }
}

