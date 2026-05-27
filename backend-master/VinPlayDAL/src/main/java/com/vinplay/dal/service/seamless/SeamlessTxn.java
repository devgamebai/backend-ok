package com.vinplay.dal.service.seamless;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalized per-action transaction record built by an aggregator subclass
 * inside {@link SeamlessWalletAggregator#dispatch} and handed to
 * {@link SeamlessWalletAggregator#doDebit} / {@link SeamlessWalletAggregator#doCredit}.
 *
 * <p>Everything in this object is in <strong>wallet-native sub-units</strong>
 * (VND for current providers — see
 * {@link com.vinplay.dal.service.MoneyGateway}). Currency conversion happens
 * <em>before</em> the {@code SeamlessTxn} is built, via the subclass's
 * {@code currencyToInternal} hook. The shared base-class flow primitives never
 * need to know the provider's display currency.
 *
 * <p>Immutable: all fields are {@code final}, the metadata map is wrapped with
 * {@link Collections#unmodifiableMap}, and the only mutator is the constructor.
 * The aggregator base class assumes the value is safe to read concurrently —
 * keep it that way.
 *
 * <p><b>Field contract:</b>
 * <ul>
 *   <li>{@code username} — caller-provided login or platform username. The
 *       subclass is responsible for resolving this to a {@code (userId,
 *       nickname)} pair before the wallet call (see the AWC aggregator's
 *       {@code lookupUser}).
 *   <li>{@code externalRef} — provider's stable per-event id. Becomes the
 *       {@code tx_id} argument to {@code MoneyGateway.creditUser/debitUser};
 *       UNIQUE on {@code (tx_id, source, user_id)} is THE dedup gate. Must be
 *       non-null and stable across provider retries.
 *   <li>{@code action} — provider-specific verb ("bet", "settle", "cancel",
 *       …). Mapped to a {@code MoneyGateway.SOURCE_*} constant by the
 *       subclass's {@code mapActionToSource} hook.
 *   <li>{@code amountSubunit} — must be {@code &gt; 0}. The base-class
 *       primitives select credit vs debit by which method is called, not by
 *       sign. Negative amounts are a programming error.
 *   <li>{@code currency} — provider's display currency code; informational
 *       only past this point (used in audit/debug logs). Conversion has
 *       already happened.
 *   <li>{@code metadata} — optional aggregator-specific extras (round id,
 *       game code, link id) the subclass wants threaded through to its own
 *       audit hook. Nullable; getter returns an empty map in that case so
 *       callers don't need to null-check.
 * </ul>
 */
public final class SeamlessTxn {

    private final String username;
    private final String externalRef;
    private final String action;
    private final long amountSubunit;
    private final String currency;
    private final Map<String, Object> metadata;

    public SeamlessTxn(String username,
                       String externalRef,
                       String action,
                       long amountSubunit,
                       String currency,
                       Map<String, Object> metadata) {
        this.username = username;
        this.externalRef = externalRef;
        this.action = action;
        this.amountSubunit = amountSubunit;
        this.currency = currency;
        this.metadata = (metadata == null)
                ? null
                : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public String getUsername()       { return username; }
    public String getExternalRef()    { return externalRef; }
    public String getAction()         { return action; }
    public long   getAmountSubunit()  { return amountSubunit; }
    public String getCurrency()       { return currency; }

    /**
     * @return the metadata map, or an empty unmodifiable map if none was
     * supplied. Never null.
     */
    public Map<String, Object> getMetadata() {
        return metadata != null ? metadata : Collections.<String, Object>emptyMap();
    }
}
