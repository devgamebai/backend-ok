/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.google.gson.Gson
 *  com.google.gson.JsonSyntaxException
 */
package game.third.hooks.gscSeamless.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.List;

public class WithdrawRequest {
    private String member_account;
    private String operator_code;
    private String product_code;
    private String currency;
    private List<Transaction> transactions;
    private String sign;
    private String request_time;
    private List<BatchRequest> batch_requests;

    public static class BatchRequest {
        private String game_type;
        private String member_account;
        private int product_code;
        private List<Transaction> transactions;
        public String getGame_type() { return game_type; }
        public String getMember_account() { return member_account; }
        public int getProduct_code() { return product_code; }
        public List<Transaction> getTransactions() { return transactions; }
    }

    public List<BatchRequest> getBatch_requests() { return batch_requests; }
    public void setBatch_requests(List<BatchRequest> batch_requests) { this.batch_requests = batch_requests; }

    /** Resolve member_account from top-level or first batch_request */
    public String resolveMemberAccount() {
        if (member_account != null) return member_account;
        if (batch_requests != null && !batch_requests.isEmpty()) return batch_requests.get(0).getMember_account();
        return null;
    }
    /** Resolve product_code from top-level or first batch_request */
    public String resolveProductCode() {
        if (product_code != null) return product_code;
        if (batch_requests != null && !batch_requests.isEmpty()) return String.valueOf(batch_requests.get(0).getProduct_code());
        return null;
    }
    /** Resolve transactions from top-level or first batch_request */
    public List<Transaction> resolveTransactions() {
        if (transactions != null && !transactions.isEmpty()) return transactions;
        if (batch_requests != null && !batch_requests.isEmpty()) return batch_requests.get(0).getTransactions();
        return null;
    }

    public WithdrawRequest(String member_account, String operator_code, String product_code, String currency, List<Transaction> transactions, String sign, String request_time) {
        this.member_account = member_account;
        this.operator_code = operator_code;
        this.product_code = product_code;
        this.currency = currency;
        this.transactions = transactions;
        this.sign = sign;
        this.request_time = request_time;
    }

    public WithdrawRequest() {
    }

    public String getMember_account() {
        return this.member_account;
    }

    public void setMember_account(String member_account) {
        this.member_account = member_account;
    }

    public String getOperator_code() {
        return this.operator_code;
    }

    public void setOperator_code(String operator_code) {
        this.operator_code = operator_code;
    }

    public String getProduct_code() {
        return this.product_code;
    }

    public void setProduct_code(String product_code) {
        this.product_code = product_code;
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

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WithdrawRequest.class);

    /**
     * SUN-1174: parse incoming GSC webhook body. Re-throws JsonSyntaxException
     * as RuntimeException with the underlying message preserved, so the
     * outer try/catch in WithdrawProcess.execute writes the cause into
     * gsc_event_log.last_error. Previously this swallowed the exception and
     * returned null, leaving the audit row with empty last_error and the
     * vendor with a generic "Invalid JSON format" — a schema mismatch
     * (like the live-casino payload type breaking JILI slots) had no
     * trace and only surfaced as a vendor "MSG 304" much later.
     */
    public static WithdrawRequest fromJson(String json) {
        Gson gson = new Gson();
        try {
            return (WithdrawRequest) gson.fromJson(json, WithdrawRequest.class);
        } catch (JsonSyntaxException e) {
            logger.error("[SUN-1174] WithdrawRequest.fromJson failed: {} — body head=\"{}\"",
                    e.getMessage(), json == null ? "null" : json.substring(0, Math.min(json.length(), 200)));
            throw new RuntimeException("WithdrawRequest parse failed: " + e.getMessage(), e);
        }
    }

    public static class Transaction {
        private String id;
        private String action;
        private String wager_code;
        private String wager_status;
        private double amount;
        private double bet_amount;
        private double valid_bet_amount;
        private double prize_amount;
        private double tip_amount;
        private long settled_at;
        private String game_code;
        // SUN-1188: vendor sends `round_id` and a `payload` whose shape
        // varies by provider. Live-casino (Blackjack, Casino Hold'em…) wraps
        // hand-level metadata in a nested map keyed by "D<round_id>":
        //   { "D<round_id>": { "game_id": "...", "session_id": "..." } }
        // Slot games (JILI, PG Soft, Pragmatic…) send a FLAT map of strings:
        //   { "token": "...", "bet_id": "...", "parent_bet_id": null }
        // Decode as Object (post SUN-1174) so Gson never barfs on the slot
        // shape. resolveVendorGameId() narrows defensively at read time.
        private String round_id;
        private Object payload;

        public Transaction() {
        }

        public Transaction(String id, String action, String wager_code, String wager_status, double amount, double bet_amount, double valid_bet_amount, double prize_amount, double tip_amount, long settled_at, String game_code) {
            this.id = id;
            this.action = action;
            this.wager_code = wager_code;
            this.wager_status = wager_status;
            this.amount = amount;
            this.bet_amount = bet_amount;
            this.valid_bet_amount = valid_bet_amount;
            this.prize_amount = prize_amount;
            this.tip_amount = tip_amount;
            this.settled_at = settled_at;
            this.game_code = game_code;
        }

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

        public double getAmount() {
            return this.amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
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

        public long getSettled_at() {
            return this.settled_at;
        }

        public void setSettled_at(long settled_at) {
            this.settled_at = settled_at;
        }

        public String getGame_code() {
            return this.game_code;
        }

        public void setGame_code(String game_code) {
            this.game_code = game_code;
        }

        public String getRound_id() { return this.round_id; }
        public void setRound_id(String round_id) { this.round_id = round_id; }

        public Object getPayload() { return this.payload; }
        public void setPayload(Object payload) { this.payload = payload; }

        /**
         * SUN-1188 / SUN-1174: extract the vendor's hand-level game_id.
         *
         * Live-casino webhooks wrap it in a nested map keyed by "D&lt;round_id&gt;":
         *   { "D<round_id>": { "game_id": "...", "session_id": "..." } }
         * Slot webhooks send a flat map (token / bet_id / parent_bet_id) with
         * NO `game_id` field — those return null and fall through to the
         * default group key in GameHistoryService.
         *
         * Decoded as Object so Gson never throws on shape mismatch — narrowed
         * defensively here at read time. SUN-1174: the previous strict
         * Map<String, Map<String, Object>> declaration broke fromJson() for
         * every JILI / PG Soft / Pragmatic slot bet.
         */
        // resolveVendorGameId() removed in the provider-strategy refactor.
        // Per-vendor linkage extraction now lives in
        // game.third.hooks.gscSeamless.provider.ProviderAdapter.resolveLinkId
        // — looked up by product_code in WithdrawProcess. See
        // EvolutionProvider (game_id), PragmaticProvider / PgSoftProvider /
        // JiliProvider (parent_round_id) for the per-provider extraction.
    }
}

