/*
 * Decompiled with CFR 0.152.
 */
package game.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

public class GameUtil {
    public static AtomicLong lastTimeTx = new AtomicLong();
    private static SplittableRandom rd = new SplittableRandom();
    protected static byte[] randomNumber = new byte[4];
    protected static SecureRandom random = new SecureRandom();

    public static int randomBetween(int a, int b) {
        return a + rd.nextInt(b - a);
    }

    public static long getTimeStampInSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    public static int randomMax(int b) {
        return GameUtil.randomBetween(0, b);
    }

    public static int getDayNumber() {
        Calendar cal = Calendar.getInstance();
        return cal.get(7);
    }

    public static long getTimeStampInDay() {
        Calendar time = Calendar.getInstance();
        time.add(14, time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTimeInMillis() / 86400000L;
    }

    public static int NextByte(int value) throws Exception {
        if (value == 0) {
            throw new Exception();
        }
        random.nextBytes(randomNumber);
        return Math.abs(randomNumber[0] % value);
    }

    public static int NextInt(int value) throws Exception {
        if (value == 0) {
            throw new Exception();
        }
        random.nextBytes(randomNumber);
        return Math.abs(new BigInteger(randomNumber).intValue() % value);
    }
}

