package com.vinplay.dal.service.seamless.gsc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provider-agnostic value object holding the parsed GSC seamless-wallet
 * request fields used by every GSC endpoint (balance, deposit, withdraw,
 * cancel, …). Phase 2 only populates the subset used by
 * {@link GscBalanceAggregator}; later phases will extend the field set as
 * needed for transactions and per-product flow.
 *
 * <p>Stays in {@code VinPlayDAL/.../seamless/gsc/} (next to the aggregator
 * base) so the aggregator does NOT depend on {@code game.third.usecase.*}.
 * That dependency direction would invert the current module graph
 * (thirdParty → VinPlayDAL) and break the build. Each per-endpoint
 * processor (e.g. legacy {@code BalanceProcess}) is responsible for
 * translating its own request DTO into a {@code GscRequest} OR for
 * delegating raw-body parsing to the aggregator's {@code parseRequest}
 * hook — Phase 2 picks the second route.
 *
 * <p>Immutable. The {@code batchMembers} list is wrapped at construction
 * with {@link Collections#unmodifiableList} so callers cannot mutate it.
 */
public final class GscRequest {

    private final String operatorCode;
    private final String currency;
    private final String sign;
    private final String requestTime;
    /** First (or only) member account at the top level — populated for endpoints that send one. */
    private final String memberAccount;
    private final Integer productCode;
    private final String gameType;
    /**
     * For batch endpoints (balance), the list of (memberAccount, productCode)
     * pairs from {@code batch_requests}. May be empty (never null).
     */
    private final List<BatchMember> batchMembers;

    /**
     * For wager-bearing endpoints (pushbet, withdraw, deposit, cancel,
     * rollback), the top-level list of transactions. Phase 3a's
     * {@link GscPushBetAggregator} populates this from the JSON
     * {@code transactions} array (with fall-back to {@code wagers}
     * mirroring {@code PushBetRequest.resolveTransactions}). Empty list
     * (never null) when the request shape doesn't carry transactions.
     */
    private final List<TransactionItem> transactions;

    /**
     * Phase 2 constructor — kept for backward compatibility with the
     * balance aggregator's existing call site. Delegates to the
     * full constructor with an empty transactions list.
     */
    public GscRequest(String operatorCode,
                      String currency,
                      String sign,
                      String requestTime,
                      String memberAccount,
                      Integer productCode,
                      String gameType,
                      List<BatchMember> batchMembers) {
        this(operatorCode, currency, sign, requestTime, memberAccount,
                productCode, gameType, batchMembers,
                Collections.<TransactionItem>emptyList());
    }

    /**
     * Phase 3 constructor — accepts the {@code transactions} list used by
     * pushbet / withdraw / deposit / cancel / rollback endpoints. The
     * Phase 2 single-arg overload above is preserved so balance-only
     * code paths compile unchanged.
     */
    public GscRequest(String operatorCode,
                      String currency,
                      String sign,
                      String requestTime,
                      String memberAccount,
                      Integer productCode,
                      String gameType,
                      List<BatchMember> batchMembers,
                      List<TransactionItem> transactions) {
        this.operatorCode  = operatorCode;
        this.currency      = currency;
        this.sign          = sign;
        this.requestTime   = requestTime;
        this.memberAccount = memberAccount;
        this.productCode   = productCode;
        this.gameType      = gameType;
        this.batchMembers  = (batchMembers == null)
                ? Collections.<BatchMember>emptyList()
                : Collections.unmodifiableList(new ArrayList<BatchMember>(batchMembers));
        this.transactions  = (transactions == null)
                ? Collections.<TransactionItem>emptyList()
                : Collections.unmodifiableList(new ArrayList<TransactionItem>(transactions));
    }

    public String getOperatorCode()         { return operatorCode; }
    public String getCurrency()             { return currency; }
    public String getSign()                 { return sign; }
    public String getRequestTime()          { return requestTime; }
    public String getMemberAccount()        { return memberAccount; }
    public Integer getProductCode()         { return productCode; }
    public String getGameType()             { return gameType; }
    public List<BatchMember> getBatchMembers() { return batchMembers; }
    public List<TransactionItem> getTransactions() { return transactions; }

    /** One element of {@code batch_requests} — the only fields the balance endpoint reads. */
    public static final class BatchMember {
        private final String memberAccount;
        private final int productCode;

        public BatchMember(String memberAccount, int productCode) {
            this.memberAccount = memberAccount;
            this.productCode   = productCode;
        }

        public String getMemberAccount() { return memberAccount; }
        public int    getProductCode()   { return productCode; }
    }

    /**
     * One element of the {@code transactions} (or {@code wagers}) array
     * used by GSC's wager-bearing endpoints. Provider-side schema is
     * documented in {@code game.third.hooks.gscSeamless.request.Transaction};
     * this DTO captures the subset the aggregators in
     * {@code com.vinplay.dal.service.seamless.gsc} need without binding
     * VinPlayDAL to {@code thirdParty}.
     *
     * <p>Field meanings (verbatim from GSC's spec):
     * <ul>
     *   <li>{@code id} — provider's stable per-event id; becomes the
     *       {@code tx_id} dedup key on wallet-moving handlers.
     *   <li>{@code action} — provider verb ({@code "BET"}, {@code "SETTLED"},
     *       {@code "CANCEL"}, …). PushBet only ever sees {@code "BET"} but
     *       we keep the field on the shared DTO for parity with later phases.
     *   <li>{@code memberAccount} — player's GSC account.
     *   <li>{@code productCode} — provider id (1002 = Evolution, 1091 =
     *       JILI, etc).
     *   <li>{@code gameCode} — provider's game id; nullable.
     *   <li>{@code currency} — per-transaction currency override
     *       (PushBet uses this when the top-level {@code currency} field
     *       is absent — see {@code PushBetProcess.execute} fall-back).
     *   <li>{@code wagerCode} — GSC's wager-grouping id (multiple
     *       transactions can share one wager_code under a buy-freespin
     *       chain). Nullable; not used by PushBet's response shape.
     *   <li>{@code amount} — provider-display-currency string. Float-parsed
     *       on demand; the legacy code accepts both {@code amount} and
     *       {@code bet_amount} (see {@code Transaction.getAmountNumber}).
     *   <li>{@code betAmount} — fall-back when {@code amount} is absent.
     * </ul>
     */
    public static final class TransactionItem {
        private final String id;
        private final String action;
        private final String memberAccount;
        private final int productCode;
        private final String gameCode;
        private final String currency;
        private final String wagerCode;
        private final String amount;
        private final String betAmount;

        // Phase 3e — GSC deposit additionally needs these to preserve the
        // SUN-888 fish-game amount override (prize_amount), the SUN-1196
        // freespin-chain link routing (payload + roundId), the SUN-1182
        // cancel-via-deposit detection (wagerStatus), and the
        // commission-volume hedge case (validBetAmount). Defaulted to
        // null on the legacy constructor so Phase 3a/3b/3c/3d call-sites
        // compile unchanged.
        private final String wagerStatus;
        private final String prizeAmount;
        private final String validBetAmount;
        private final String roundId;
        private final Object payload;

        /**
         * Phase 3a–3d constructor. Leaves the Phase 3e fields
         * ({@code wagerStatus}, {@code prizeAmount}, {@code validBetAmount},
         * {@code roundId}, {@code payload}) null. Use the 14-arg
         * constructor below for deposit / withdraw parsers that read those
         * fields.
         */
        public TransactionItem(String id,
                               String action,
                               String memberAccount,
                               int productCode,
                               String gameCode,
                               String currency,
                               String wagerCode,
                               String amount,
                               String betAmount) {
            this(id, action, memberAccount, productCode, gameCode, currency,
                    wagerCode, amount, betAmount,
                    /*wagerStatus*/ null, /*prizeAmount*/ null,
                    /*validBetAmount*/ null, /*roundId*/ null, /*payload*/ null);
        }

        /**
         * Phase 3e full constructor — accepts the deposit-only fields
         * needed for SUN-888 / SUN-1182 / SUN-1196 preservation.
         */
        public TransactionItem(String id,
                               String action,
                               String memberAccount,
                               int productCode,
                               String gameCode,
                               String currency,
                               String wagerCode,
                               String amount,
                               String betAmount,
                               String wagerStatus,
                               String prizeAmount,
                               String validBetAmount,
                               String roundId,
                               Object payload) {
            this.id             = id;
            this.action         = action;
            this.memberAccount  = memberAccount;
            this.productCode    = productCode;
            this.gameCode       = gameCode;
            this.currency       = currency;
            this.wagerCode      = wagerCode;
            this.amount         = amount;
            this.betAmount      = betAmount;
            this.wagerStatus    = wagerStatus;
            this.prizeAmount    = prizeAmount;
            this.validBetAmount = validBetAmount;
            this.roundId        = roundId;
            this.payload        = payload;
        }

        public String getId()             { return id; }
        public String getAction()         { return action; }
        public String getMemberAccount()  { return memberAccount; }
        public int    getProductCode()    { return productCode; }
        public String getGameCode()       { return gameCode; }
        public String getCurrency()       { return currency; }
        public String getWagerCode()      { return wagerCode; }
        public String getAmount()         { return amount; }
        public String getBetAmount()      { return betAmount; }
        public String getWagerStatus()    { return wagerStatus; }
        public String getPrizeAmount()    { return prizeAmount; }
        public String getValidBetAmount() { return validBetAmount; }
        public String getRoundId()        { return roundId; }
        public Object getPayload()        { return payload; }

        /**
         * Numeric amount in provider-display currency. Mirrors the
         * legacy {@code Transaction.getAmountNumber}: prefer
         * {@code amount}, fall back to {@code bet_amount}, default 0.
         */
        public float getAmountNumber() {
            if (amount != null && !amount.isEmpty()) {
                try { return Float.parseFloat(amount); } catch (NumberFormatException ignored) {}
            }
            if (betAmount != null && !betAmount.isEmpty()) {
                try { return Float.parseFloat(betAmount); } catch (NumberFormatException ignored) {}
            }
            return 0f;
        }
    }
}
