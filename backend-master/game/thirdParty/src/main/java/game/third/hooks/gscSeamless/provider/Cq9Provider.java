package game.third.hooks.gscSeamless.provider;

/**
 * CQ9 — product 1085. The fish-game amount-substitution quirk (SUN-888,
 * webhook {@code amount} = full balance, real bet in
 * {@code valid_bet_amount}/{@code bet_amount}) is now in the
 * {@link ProviderAdapter#resolveBetAmount default resolveBetAmount}
 * implementation so it applies uniformly to every fish provider —
 * CQ9, Fachai and JILI fish behave identically. This class is the
 * registry placeholder until CQ9 grows other quirks.
 */
public final class Cq9Provider implements ProviderAdapter {
    public static final Cq9Provider INSTANCE = new Cq9Provider();
    private Cq9Provider() {}

    @Override public int productCode() { return 1085; }
    @Override public String displayName() { return "CQ9"; }
}
