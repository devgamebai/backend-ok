/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.gameRoom.config;

import com.vinplay.usercore.utils.GameCommon;
import game.utils.GameUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HuVangConfig {
    private JSONObject config = null;
    private JSONObject currentGameConfig = null;
    private static HuVangConfig huVangConfig = null;

    private HuVangConfig() {
        this.initconfig();
    }

    public static HuVangConfig instance() {
        if (huVangConfig == null) {
            huVangConfig = new HuVangConfig();
        }
        return huVangConfig;
    }

    public void initconfig() {
        try {
            String configHu = GameCommon.getHuVangGameBai();
            this.config = new JSONObject(configHu);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initconfigFromFile() {
        String path = System.getProperty("user.dir");
        File file = new File(path + "/conf/huvang.json");
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = null;
        try {
            InputStreamReader r = new InputStreamReader((InputStream)new FileInputStream(file), "UTF-8");
            reader = new BufferedReader(r);
            String text = null;
            while ((text = reader.readLine()) != null) {
                contents.append(text).append(System.getProperty("line.separator"));
            }
            this.config = new JSONObject(contents.toString());
        }
        catch (Exception e) {
            this.initconfig();
            e.printStackTrace();
        }
    }

    public int kiemTraThoiGianHuVangGameHienTai() {
        return this.kiemTraHuVangTheoThoiGian(this.getConfigCurrentGame());
    }

    public JSONObject timHuVangDangChay() {
        try {
            JSONArray array = this.config.getJSONArray("huvang");
            JSONObject game = null;
            int remain = 86400;
            for (int i = 0; i < array.length(); ++i) {
                JSONObject g = array.getJSONObject(i);
                int time = this.kiemTraHuVangTheoThoiGian(g);
                if (time < 0) {
                    return g;
                }
                if (time <= 0 || time >= remain) continue;
                game = g;
                remain = time;
            }
            return game;
        }
        catch (Exception e) {
            return null;
        }
    }

    public int kiemTraHuVangTheoThoiGian(JSONObject gameConfig) {
        try {
            int startMinute;
            int endMinute;
            if (gameConfig == null) {
                return 0;
            }
            Calendar today = Calendar.getInstance();
            int dayInWeek = today.get(7);
            int hour = today.get(11);
            int minute = today.get(12);
            int second = today.get(13);
            int flag = -1;
            JSONArray arr = gameConfig.getJSONArray("dayInWeek");
            for (int i = 0; i < arr.length(); ++i) {
                int day = arr.getInt(i);
                if (day != dayInWeek) continue;
                flag = 0;
                break;
            }
            if (flag == -1) {
                return 0;
            }
            int startHour = gameConfig.getJSONObject("startTime").getInt("h");
            int remain1 = this.subTimeGetMinute(hour, minute, startHour, startMinute = gameConfig.getJSONObject("startTime").getInt("m"));
            if (remain1 > 0) {
                return remain1 * 60 - second;
            }
            int endHour = gameConfig.getJSONObject("endTime").getInt("h");
            int remain2 = this.subTimeGetMinute(hour, minute, endHour, endMinute = gameConfig.getJSONObject("endTime").getInt("m"));
            if (remain2 > 0) {
                return - remain2 * 60 - second;
            }
            return 0;
        }
        catch (Exception e) {
            return 0;
        }
    }

    public int subTimeGetMinute(int h1, int m1, int h2, int m2) {
        int minute1 = h1 * 60 + m1;
        int minute2 = h2 * 60 + m2;
        return minute2 - minute1;
    }

    public JSONObject getGameConfig(String gameName) {
        try {
            JSONArray array = this.config.getJSONArray("huvang");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject game = array.getJSONObject(i);
                String name = game.getString("gameName");
                if (!gameName.equalsIgnoreCase(name)) continue;
                return game;
            }
            return null;
        }
        catch (JSONException e) {
            return null;
        }
    }

    public JSONObject getConfigCurrentGame() {
        if (this.currentGameConfig != null) {
            return this.currentGameConfig;
        }
        this.currentGameConfig = this.getGameConfig(GameUtils.gameName);
        return this.currentGameConfig;
    }

    public double getRate(String game, long moneyBet) {
        try {
            JSONObject rate = this.getGameConfig(game).getJSONObject("rate");
            return (double)rate.getInt(String.valueOf(moneyBet)) / 1000.0;
        }
        catch (Exception e) {
            return 0.0;
        }
    }
}

