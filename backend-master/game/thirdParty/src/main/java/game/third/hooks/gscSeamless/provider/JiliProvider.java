package game.third.hooks.gscSeamless.provider;

import java.util.Map;

/**
 * JILI Gaming — product 1091.
 *
 * 145 slot games + 15 fishing games + 1 game-show + 1 poker. SUN-1174
 * caught us hardcoding "1091 = fish" which mis-routed every JILI slot
 * (Devil Fire 2, Mayan Empire, Lucky Tiger, …) through the seamless-
 * transfer skip path. The {@link #isFishGame} default already reads
 * {@code gsc_game_catalog.category} so it correctly distinguishes
 * 1091/258 (Devil Fire 2 / Slot) from 1091/1 (Royal Fishing / Fishing).
 *
 * JILI also emits {@code parent_round_id} on freespin chains (same shape
 * as PG Soft / Pragmatic); inherits the link extraction from there.
 */
public final class JiliProvider implements ProviderAdapter {
    public static final JiliProvider INSTANCE = new JiliProvider();
    private JiliProvider() {}

    @Override public int productCode() { return 1091; }
    @Override public String displayName() { return "JILI"; }

    @Override
    public String resolveLinkId(Object payload, String roundId) {
        Map<?, ?> inner = ProviderAdapter.firstNestedMap(payload);
        return ProviderAdapter.mapGetStr(inner, "parent_round_id");
    }
}
