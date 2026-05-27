/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.api.IBZApi
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dal.service.impl.CacheServiceImpl
 *  com.vinplay.vbee.common.exceptions.KeyNotFoundException
 */
package game.modules.minigame.utils;

import bitzero.server.api.IBZApi;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.Debug;
import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;
import game.utils.ConfigGame;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MiniGameUtils {
    public static int calculateTimeRewardOnNextDay(String nextTimeRewarded) throws ParseException {
        if (nextTimeRewarded.isEmpty()) {
            nextTimeRewarded = "00:30:00";
        }
        long startTime = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd " + nextTimeRewarded);
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.add(5, 1);
        String rewardTimeStr = format.format(cal.getTime());
        Date rewardTimeD = format2.parse(rewardTimeStr);
        long rewardTime = rewardTimeD.getTime();
        long remainTime = rewardTime - startTime;
        long seconds = remainTime / 1000L;
        return (int)seconds;
    }

    public static Date getNextGioVang(int[] input, int lastDayFinishGioVang, String configTimeX2) {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(7);
        int nextDayGioVang = input[0];
        boolean nextWeek = true;
        for (int i = 0; i < input.length; ++i) {
            if (dayOfWeek < input[i]) {
                nextDayGioVang = input[i];
                nextWeek = false;
                break;
            }
            if (dayOfWeek != input[i] || lastDayFinishGioVang == input[i]) continue;
            nextDayGioVang = input[i];
            nextWeek = false;
            break;
        }
        Debug.trace(("Next day gio vang: " + nextDayGioVang + ", nextWeek= " + nextWeek));
        cal.set(7, nextDayGioVang);
        if (nextWeek) {
            cal.add(5, 7);
        }
        SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
        String dayX2 = df1.format(cal.getTime());
        String timeGioVang = ConfigGame.getValueString(configTimeX2);
        String str = timeGioVang + " " + dayX2;
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        Date out = cal.getTime();
        try {
            out = df.parse(str);
        }
        catch (ParseException parseException) {
            // empty catch block
        }
        return out;
    }

    public static int calculateTimeX2(int[] input, int lastDayFinishGioVang, String configTimeX2) {
        Date nextDay = MiniGameUtils.getNextGioVang(input, lastDayFinishGioVang, configTimeX2);
        return (int)(nextDay.getTime() - System.currentTimeMillis()) / 1000;
    }

    public static String calculateTimeX2AsString(int[] input, int lastDayFinishGioVang, String configTimeX2) {
        Date nextDay = MiniGameUtils.getNextGioVang(input, lastDayFinishGioVang, configTimeX2);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(nextDay);
    }

    public static int[] getX2Days(String configName) {
        String x2DaysStr = ConfigGame.getValueString(configName);
        String[] arr = x2DaysStr.split(",");
        int[] result = new int[arr.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = Integer.parseInt(arr[i]);
        }
        return result;
    }

    public static int getLastDayX2(String configName) {
        CacheServiceImpl cache = new CacheServiceImpl();
        try {
            return cache.getValueInt(configName);
        }
        catch (KeyNotFoundException e) {
            int lastDay = ConfigGame.getIntValue(configName);
            MiniGameUtils.saveLastDayX2(configName, lastDay);
            return lastDay;
        }
    }

    public static void saveLastDayX2(String configName, int lastDay) {
        CacheServiceImpl cache = new CacheServiceImpl();
        cache.setValue(configName, lastDay);
    }

    public static void sendMessageToUser(BaseMsg msg, String username) {
////        List<User> users = new ArrayList<>();
        List<User> users = ExtensionUtility.getExtension().getApi().getUserByName(username);

//        List<User> users = new ArrayList<>();
//        users.add(ExtensionUtility.getExtension().getApi().getUserByName(username));
        if (users != null) {
            ExtensionUtility.getExtension().sendUsers(msg, users);
        }
    }
}

