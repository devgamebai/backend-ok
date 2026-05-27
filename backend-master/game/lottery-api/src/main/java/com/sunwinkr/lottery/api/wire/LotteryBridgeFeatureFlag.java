package com.sunwinkr.lottery.api.wire;

/**
 * Public static feature-flag accessor for the legacy
 * {@code game.modules.minigame.LotteryModule}. Importing the bridge class
 * directly would force the legacy module to take on a hard runtime
 * dependency on every Spring bean wiring — instead the legacy module
 * checks this single-purpose flag class and falls through to the legacy
 * path if the flag is off.
 *
 * <p>Plan §6 / §7.
 */
public final class LotteryBridgeFeatureFlag {

    private LotteryBridgeFeatureFlag() {
    }

    /** {@link LotteryModuleBridge#isEnabled} delegate. */
    public static boolean isEnabled() {
        return LotteryModuleBridge.isEnabled();
    }
}
