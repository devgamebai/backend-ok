package game.third.hooks.gscSeamless.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of {@link ProviderAdapter} instances keyed by GSC product_code.
 *
 * Adding a new provider:
 *   1. Implement {@link ProviderAdapter} (or extend {@link DefaultProvider}'s
 *      defaults) in this package.
 *   2. Add one line to the static initializer below.
 *
 * That's it — no edits to {@code WithdrawProcess} / {@code DepositProcess} /
 * {@code CancelProcess}. They look up the adapter and delegate.
 */
public final class ProviderRegistry {

    private static final Map<Integer, ProviderAdapter> REGISTRY;

    static {
        Map<Integer, ProviderAdapter> m = new HashMap<>();
        m.put(EvolutionProvider.INSTANCE.productCode(), EvolutionProvider.INSTANCE);
        m.put(PragmaticProvider.INSTANCE.productCode(), PragmaticProvider.INSTANCE);
        m.put(PgSoftProvider.INSTANCE.productCode(),    PgSoftProvider.INSTANCE);
        m.put(FachaiProvider.INSTANCE.productCode(),    FachaiProvider.INSTANCE);
        m.put(Cq9Provider.INSTANCE.productCode(),       Cq9Provider.INSTANCE);
        m.put(JiliProvider.INSTANCE.productCode(),      JiliProvider.INSTANCE);
        m.put(HashGameProvider.INSTANCE.productCode(),  HashGameProvider.INSTANCE);
        // SUN-1205/1206 — Dream Gaming (Asia Gaming Dreaming Studio)
        m.put(DreamGamingProvider.INSTANCE.productCode(), DreamGamingProvider.INSTANCE);
        REGISTRY = Collections.unmodifiableMap(m);
    }

    private ProviderRegistry() {}

    /** Adapter for a given product_code; never null (falls back to DefaultProvider). */
    public static ProviderAdapter forProduct(int productCode) {
        ProviderAdapter a = REGISTRY.get(productCode);
        return a != null ? a : DefaultProvider.INSTANCE;
    }

    /** Adapter for a string product_code (best-effort parse). */
    public static ProviderAdapter forProduct(String productCode) {
        if (productCode == null || productCode.isEmpty()) return DefaultProvider.INSTANCE;
        try { return forProduct(Integer.parseInt(productCode)); }
        catch (NumberFormatException e) { return DefaultProvider.INSTANCE; }
    }
}
