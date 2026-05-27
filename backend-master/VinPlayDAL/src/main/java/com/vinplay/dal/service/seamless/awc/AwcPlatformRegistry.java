package com.vinplay.dal.service.seamless.awc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Lookup table from AWC platform code to {@link AwcPlatformAdapter}.
 * Sister of GSC's {@code ProviderRegistry.forProduct(int)} pattern.
 *
 * <p>Registrations live in the static initialiser so every read-path
 * caller goes through {@link #forPlatform(String)} and never instantiates
 * an adapter directly. Unknown platform codes fall back to
 * {@link AwcPlatformAdapter#DEFAULT} which keeps the legacy
 * {@code platform_tx_id} grouping — safe for any slot-style platform
 * (one ptxn = one spin).
 */
public final class AwcPlatformRegistry {

    private static final Map<String, AwcPlatformAdapter> ADAPTERS;

    static {
        Map<String, AwcPlatformAdapter> m = new HashMap<>();
        register(m, SexyBcrtAdapter.INSTANCE);
        // Future: JiliAdapter, Cq9Adapter, FachaiAdapter, PgSoftAdapter.
        // Slot-style platforms today use the DEFAULT ptxn grouping which
        // already matches the per-spin grain operators audit against, so
        // no explicit adapter is required until a platform-specific
        // divergence surfaces.
        ADAPTERS = Collections.unmodifiableMap(m);
    }

    private AwcPlatformRegistry() {}

    private static void register(Map<String, AwcPlatformAdapter> m, AwcPlatformAdapter a) {
        m.put(a.platformCode().toUpperCase(), a);
    }

    /**
     * Resolve an adapter for the given platform code. {@code null} /
     * empty / unknown codes return {@link AwcPlatformAdapter#DEFAULT}.
     */
    public static AwcPlatformAdapter forPlatform(String platformCode) {
        if (platformCode == null || platformCode.isEmpty()) return AwcPlatformAdapter.DEFAULT;
        AwcPlatformAdapter a = ADAPTERS.get(platformCode.toUpperCase());
        return a != null ? a : AwcPlatformAdapter.DEFAULT;
    }
}
