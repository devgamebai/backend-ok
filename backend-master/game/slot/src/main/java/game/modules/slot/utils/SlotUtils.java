package game.modules.slot.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.vinplay.dal.service.impl.CacheServiceImpl;
import com.vinplay.vbee.common.exceptions.KeyNotFoundException;

import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import game.modules.slot.entities.slot.khobau.KhoBauItem;
import game.util.ConfigGame;

public class SlotUtils {
    private static Logger loggerKhoBau = Logger.getLogger("csvKhoBau");
    private static Logger loggerAvengers = Logger.getLogger("csvAvengers");
    private static Logger loggerSpartan = Logger.getLogger("csvSpartan");
    private static Logger loggerMyNhanNgu = Logger.getLogger("csvMyNhanNgu");
    private static Logger loggerNuDiepVien = Logger.getLogger("csvNuDiepVien");
    private static Logger loggerAudition = Logger.getLogger("csvAudition");
    private static Logger loggerVQV = Logger.getLogger("csvVQV");
    private static String FORMAT_PLAY_SLOT = ", %10d, %15s, %8d, %20s, %20s, %5d, %10d, %15s, %20s";
    private static Logger loggerSamTruyen = Logger.getLogger("csvSamTruyen");

    public static void logKhoBau(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerKhoBau.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static void logAvengers(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerAvengers.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static void logSpartan(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerSpartan.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static void logSamTruyen(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerSamTruyen.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static void loggerMyNhanNgu(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerMyNhanNgu.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static void logNuDiepVien(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerNuDiepVien.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static void logAudition(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerAudition.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static void logVQV(long referenceId, String username, int betValue, String matrix, String haiSao, short result, long handleTime, String ratioTime, String timeLog) {
        String matrixStr = matrix.replaceAll(",", " ");
        String haiSaoStr = haiSao.replaceAll(",", " ");
        loggerVQV.debug((Object)String.format(FORMAT_PLAY_SLOT, referenceId, username, betValue, matrixStr, haiSaoStr, result, handleTime, ratioTime, timeLog));
    }

    public static int[] getX2Days(String gameName) {
        String x2DaysStr = ConfigGame.getValueString(String.valueOf(gameName) + "_days_x2");
        String[] arr = x2DaysStr.split(",");
        int[] result = new int[arr.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = Integer.parseInt(arr[i]);
        }
        return result;
    }

    public static int getLastDayX2(String gameName) {
        CacheServiceImpl cache = new CacheServiceImpl();
        try {
            return cache.getValueInt(String.valueOf(gameName) + "_last_day_x2");
        }
        catch (KeyNotFoundException e) {
            int lastDay = ConfigGame.getIntValue(String.valueOf(gameName) + "_last_day_gio_vang");
            SlotUtils.saveLastDayX2(gameName, lastDay);
            return lastDay;
        }
    }

    public static void saveLastDayX2(String gameName, int lastDay) {
        CacheServiceImpl cache = new CacheServiceImpl();
        cache.setValue(String.valueOf(gameName) + "_last_day_x2", lastDay);
    }

    public static void main(String[] args) {
        String lines = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";
        KhoBauItem[][] matrix = KhoBauUtils.generateMatrixNoHu("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20".split(","));
        for (int i = 0; i < 3; ++i) {
            StringBuilder b = new StringBuilder();
            for (int j = 0; j < 5; ++j) {
                b.append(" " + matrix[i][j].getId());
            }
            System.out.println(b.toString());
        }
    }

    public static Date getNextGioVang(String gameName, int[] input, int lastDayFinishGioVang) {
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
        cal.set(7, nextDayGioVang);
        if (nextWeek) {
            cal.add(5, 7);
        }
        SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
        String dayX2 = df1.format(cal.getTime());
        String timeGioVang = ConfigGame.getValueString(String.valueOf(gameName) + "_time_x2");
        String str = String.valueOf(timeGioVang) + " " + dayX2;
        SimpleDateFormat df2 = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy");
        Date out = cal.getTime();
        try {
            out = df2.parse(str);
        }
        catch (ParseException parseException) {
            // empty catch block
        }
        return out;
    }

    public static int calculateTimePokeGoX2(String gameName, int[] input, int lastDayFinishGioVang) {
        Date nextDay = SlotUtils.getNextGioVang(gameName, input, lastDayFinishGioVang);
        return (int)(nextDay.getTime() - System.currentTimeMillis()) / 1000;
    }

    public static String calculateTimePokeGoX2AsString(String gameName, int[] input, int lastDayFinishGioVang) {
        Date nextDay = SlotUtils.getNextGioVang(gameName, input, lastDayFinishGioVang);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(nextDay);
    }

    public static void sendMessageToUser(BaseMsg msg, String username) {
        List users = ExtensionUtility.getExtension().getApi().getUserByName(username);
        if (users != null) {
            ExtensionUtility.getExtension().sendUsers(msg, users);
        }
    }

    public static void sendMessageToUser(BaseMsg msg, User user) {
        if (user != null) {
            ExtensionUtility.getExtension().send(msg, user);
        }
    }
    
    public static void sendMessageToAllUser(BaseMsg msg) {
    	List<User> users = BitZeroServer.getInstance().getUserManager().getAllUsers();
        if (users != null && !users.isEmpty()) {
            ExtensionUtility.getExtension().send(msg, users);
        }
    }
    
}

