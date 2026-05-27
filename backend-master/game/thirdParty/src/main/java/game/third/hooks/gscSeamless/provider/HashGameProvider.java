package game.third.hooks.gscSeamless.provider;

/**
 * HashGame / AI Live — product 1149.
 *
 * Sends cancel as a regular {@code /deposit} with {@code action="CANCEL"}
 * (or {@code wager_status} in {CANCEL,REFUND,VOID}) instead of the
 * dedicated {@code /cancel} endpoint. The default
 * {@link ProviderAdapter#isCancelLikeDeposit(String,String)} already
 * handles that, so this adapter is just a registry placeholder until
 * Hash Game grows other quirks.
 */
public final class HashGameProvider implements ProviderAdapter {
    public static final HashGameProvider INSTANCE = new HashGameProvider();
    private HashGameProvider() {}

    @Override public int productCode() { return 1149; }
    @Override public String displayName() { return "HashGame"; }
}
