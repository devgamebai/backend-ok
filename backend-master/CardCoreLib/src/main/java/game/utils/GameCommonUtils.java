/*
 * Decompiled with CFR 0_116.
 */
package game.utils;

import game.utils.GameUtils;
import java.util.Arrays;
import java.util.List;

public class GameCommonUtils {
    private static final List<String> listGameLockCreate = Arrays.asList("XocDia", "Sam", "Tlmn", "Binh");

    public static boolean canCreateRoom() {
        if (listGameLockCreate.contains(GameUtils.gameName)) {
            return false;
        }
        return true;
    }
}

