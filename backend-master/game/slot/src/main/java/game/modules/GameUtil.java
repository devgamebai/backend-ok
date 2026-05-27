package game.modules;

import java.util.Calendar;
import java.util.SplittableRandom;

public class GameUtil {
	private static  SplittableRandom rd = new SplittableRandom();
    public static int randomBetween(int a, int b) {
        return a + rd.nextInt(b - a);
    }
    public static long getTimeStampInSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static int randomMax(int b){
        return randomBetween(0,b);
    }


    public static long getTimeStampInDay() {
        Calendar time = Calendar.getInstance();
        time.add(Calendar.MILLISECOND, time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTimeInMillis()/(86400000);
    }

    public static int getDayNumber() {
        Calendar cal = Calendar.getInstance();
        //1 - 7, 1: Sun, 7: Sat
        return cal.get(Calendar.DAY_OF_WEEK);
    }
}
