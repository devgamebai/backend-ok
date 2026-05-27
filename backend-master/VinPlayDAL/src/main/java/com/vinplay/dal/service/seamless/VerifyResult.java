package com.vinplay.dal.service.seamless;

/**
 * Result of {@link SeamlessWalletAggregator#verifySignature}: did the inbound
 * provider request authenticate?
 *
 * <p>Immutable value object. Construct via the static factories so the meaning
 * of each instance is unambiguous at the call site:
 * <pre>
 *   return VerifyResult.ok();
 *   return VerifyResult.fail("signature mismatch");
 * </pre>
 *
 * <p>The {@code message} field is intentionally a free-form string — each
 * provider's signature scheme has its own diagnostic vocabulary (AWC's "key !=
 * cert", GSC's "md5 mismatch", etc). It is logged but never exposed back to
 * the provider verbatim — the subclass's {@code serializeResponse} flattens
 * any {@link SeamlessOutcome} with status SIGNATURE_ERROR into the provider's
 * canonical "auth failed" code.
 */
public final class VerifyResult {

    public final boolean ok;
    public final String message;

    private VerifyResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    /** Successful verification. {@code message} is null. */
    public static VerifyResult ok() {
        return new VerifyResult(true, null);
    }

    /**
     * Verification failed.
     *
     * @param reason short diagnostic for logs (not surfaced to the provider).
     *               Null is permitted but discouraged — pass an empty string
     *               rather than null if no detail is available.
     */
    public static VerifyResult fail(String reason) {
        return new VerifyResult(false, reason);
    }
}
