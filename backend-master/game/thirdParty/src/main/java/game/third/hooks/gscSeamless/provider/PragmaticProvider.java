package game.third.hooks.gscSeamless.provider;

import java.util.Map;

/**
 * Pragmatic Play slots — product 1006.
 *
 * Buy-freespin chains (Wild Bounty Showdown, Sweet Bonanza, etc.) link via
 * {@code payload.<bet_id>.parent_round_id}. The BUY transaction sets that
 * field to its own round_id; every freespin spin within the chain shares
 * the same value. SUN-1196 introduced this merge so the agent view shows
 * one row with the combined bet+prize instead of dropping freespin wins
 * (bet=0 prize&gt;0) via the SUN-1184 read filter.
 */
public final class PragmaticProvider implements ProviderAdapter {
    public static final PragmaticProvider INSTANCE = new PragmaticProvider();
    private PragmaticProvider() {}

    @Override public int productCode() { return 1006; }
    @Override public String displayName() { return "Pragmatic"; }

    @Override
    public String resolveLinkId(Object payload, String roundId) {
        Map<?, ?> inner = ProviderAdapter.firstNestedMap(payload);
        return ProviderAdapter.mapGetStr(inner, "parent_round_id");
    }
}
