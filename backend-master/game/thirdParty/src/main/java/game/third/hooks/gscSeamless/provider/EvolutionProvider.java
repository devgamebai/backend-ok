package game.third.hooks.gscSeamless.provider;

import java.util.Map;

/**
 * Evolution / SA Gaming live-casino — product 1002.
 *
 * Sub-bets of one logical hand (Blackjack main + insurance, Casino Hold'em
 * splits, …) share the same {@code payload."D<round_id>".game_id} value.
 * SUN-1188 introduced the merge; this adapter is the canonical home for
 * the extraction.
 */
public final class EvolutionProvider implements ProviderAdapter {
    public static final EvolutionProvider INSTANCE = new EvolutionProvider();
    private EvolutionProvider() {}

    @Override public int productCode() { return 1002; }
    @Override public String displayName() { return "Evolution"; }

    @Override
    public String resolveLinkId(Object payload, String roundId) {
        if (!(payload instanceof Map)) return null;
        Map<?, ?> root = (Map<?, ?>) payload;
        if (root.isEmpty()) return null;
        Object inner = null;
        if (roundId != null && !roundId.isEmpty()) {
            inner = root.get("D" + roundId);
        }
        if (!(inner instanceof Map)) {
            inner = ProviderAdapter.firstNestedMap(payload);
        }
        if (!(inner instanceof Map)) return null;
        return ProviderAdapter.mapGetStr((Map<?, ?>) inner, "game_id");
    }
}
