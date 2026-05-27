package game.third.hooks.gscSeamless.provider;

/** Fallback for unknown product codes — pure interface defaults. */
public final class DefaultProvider implements ProviderAdapter {
    public static final DefaultProvider INSTANCE = new DefaultProvider();
    private DefaultProvider() {}
    @Override public int productCode() { return -1; }
    @Override public String displayName() { return "Default"; }
}
