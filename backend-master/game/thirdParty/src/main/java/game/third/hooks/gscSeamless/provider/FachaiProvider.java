package game.third.hooks.gscSeamless.provider;

/**
 * Fachai Gaming — product 1009. All 6 cataloged games are Fishing.
 * Fish-amount substitution comes from
 * {@link ProviderAdapter#resolveBetAmount default resolveBetAmount}
 * so behaviour is uniform across every fish provider.
 */
public final class FachaiProvider implements ProviderAdapter {
    public static final FachaiProvider INSTANCE = new FachaiProvider();
    private FachaiProvider() {}

    @Override public int productCode() { return 1009; }
    @Override public String displayName() { return "Fachai"; }
}
