/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.test;

import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Test {
    public static void main(String[] args) throws ParseException {
        long startTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd 00:30:00");
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.add(5, 1);
        String rewardTimeStr = format.format(cal.getTime());
        System.out.println(rewardTimeStr);
        Date rewardTimeD = format2.parse(rewardTimeStr);
        System.out.println(format2.format(rewardTimeD));
        long rewardTime = rewardTimeD.getTime();
        long remainTime = rewardTime - startTime;
        long seconds = remainTime / 1000L;
        System.out.println(seconds);
    }
}

