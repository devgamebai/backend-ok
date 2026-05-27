package com.vinplay.api.processors.gsc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Centralized env-driven GSC configuration.
 *
 * All values come from environment variables so staging ↔ production can be
 * switched by editing .env — no code changes, no rebuilds.
 *
 * Fallback values target staging so dev environments keep working if env is
 * unset, but production must set every var explicitly via docker-compose.
 */
final class GscEnv {

    private GscEnv() {}

    static String operatorUrl() {
        return getenv("GSC_OPERATOR_URL", "https://staging.gsimw.com");
    }

    static String operatorCode() {
        return getenv("GSC_OPERATOR_CODE", "G7A1");
    }

    static String secretKey() {
        return getenv("GSC_SECRET_KEY", "abYVbCrLT2VwpASotZGmCT");
    }

    static String lobbyUrl() {
        return getenv("GSC_LOBBY_URL", "https://staging-play.sunkr.bet");
    }

    static String currency() {
        return getenv("GSC_CURRENCY", "IDR2");
    }

    /**
     * Per-product currency overrides, parsed from GSC_CURRENCY_PER_PRODUCT.
     *
     * Some providers on a given operator account use a different currency
     * from the account's default. Example on operator G7A1 (KRW): PG Soft
     * (product 1007) is only available as KRW2 (1000× scaled Won), not KRW.
     *
     * Format: "PRODUCT_CODE:CURRENCY,PRODUCT_CODE:CURRENCY,..."
     * Example: "1007:KRW2"
     *          "1007:KRW2,1006:VND"
     *
     * If a product_code is not in the map, {@link #currency()} is used.
     */
    static Map<Integer, String> currencyPerProduct() {
        String raw = getenv("GSC_CURRENCY_PER_PRODUCT", "");
        if (raw.isEmpty()) return Collections.emptyMap();
        Map<Integer, String> out = new HashMap<>();
        for (String entry : raw.split(",")) {
            String[] kv = entry.split(":", 2);
            if (kv.length != 2) continue;
            String pc = kv[0].trim();
            String cur = kv[1].trim();
            if (pc.isEmpty() || cur.isEmpty()) continue;
            try { out.put(Integer.parseInt(pc), cur); } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    /** Resolve the currency to use for a given product_code — per-product override wins. */
    static String currencyFor(int productCode) {
        Map<Integer, String> map = currencyPerProduct();
        String override = map.get(productCode);
        return (override != null && !override.isEmpty()) ? override : currency();
    }

    /** "strict" hides non-whitelisted games; "feature" keeps them but marks flag. */
    static String whitelistMode() {
        return getenv("GSC_WHITELIST_MODE", "feature");
    }

    static Set<Integer> whitelistProducts() {
        String raw = getenv("GSC_WHITELIST_PRODUCTS", "");
        if (raw.isEmpty()) return Collections.emptySet();
        Set<Integer> out = new HashSet<>();
        for (String p : raw.split(",")) {
            p = p.trim();
            if (p.isEmpty()) continue;
            try { out.add(Integer.parseInt(p)); } catch (NumberFormatException ignore) {}
        }
        return out;
    }

    static Set<String> whitelistGames() {
        String raw = getenv("GSC_WHITELIST_GAMES", "");
        if (raw.isEmpty()) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        for (String g : raw.split(",")) {
            g = g.trim();
            if (!g.isEmpty()) out.add(g);
        }
        return out;
    }

    private static String getenv(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }
}
