package game.third.hooks.gscSeamless.provider;

import java.util.Map;

/**
 * PG Soft — product 1007. Same payload shape as Pragmatic (parent_round_id
 * inside the first nested map); kept as its own class for registry clarity
 * and future divergence (PG Soft has different freespin / bonus mechanics
 * than Pragmatic; if those start needing per-event handling we add it
 * here without touching Pragmatic).
 */
public final class PgSoftProvider implements ProviderAdapter {
    public static final PgSoftProvider INSTANCE = new PgSoftProvider();
    private PgSoftProvider() {}

    @Override public int productCode() { return 1007; }
    @Override public String displayName() { return "PG Soft"; }

    @Override
    public String resolveLinkId(Object payload, String roundId) {
        Map<?, ?> inner = ProviderAdapter.firstNestedMap(payload);
        return ProviderAdapter.mapGetStr(inner, "parent_round_id");
    }
}
