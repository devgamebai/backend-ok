/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.bot;

import bitzero.util.common.business.CommonHandle;
import com.vinplay.vbee.common.config.VBeePath;
import game.utils.GameUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BotConfig {
    private JSONObject config = null;
    private static BotConfig botConfig = null;

    private BotConfig() {
        this.initconfig();
    }

    public static BotConfig instance() {
        if (botConfig == null) {
            botConfig = new BotConfig();
        }
        return botConfig;
    }

    public List<String> getListBotNames() {
        try {
            JSONArray arr = this.config.getJSONArray("botList");
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject json = arr.getJSONObject(i);
                String name = json.getString("game");
                if (!name.equalsIgnoreCase(GameUtils.gameName)) continue;
                ArrayList<String> list = new ArrayList<String>();
                JSONArray a = json.getJSONArray("bot");
                for (int j = 0; j < a.length(); ++j) {
                    String s = a.getString(j);
                    list.add(s);
                }
                return list;
            }
            return null;
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
            return null;
        }
    }

    public long getMaxWin() {
        try {
            return this.config.getLong("maxWin");
        }
        catch (Exception e) {
            return 1000000;
        }
    }

    public long getMaxLost() {
        try {
            return this.config.getLong("maxLost");
        }
        catch (Exception e) {
            return 1000000;
        }
    }

    public long getMaxInitialRoom() {
        try {
            return this.config.getLong("maxInitialRoom");
        }
        catch (Exception e) {
            return 3;
        }
    }

    public long getMaxNumMoneyBet() {
        try {
            return this.config.getLong("maxMaxNumMoneyBet");
        }
        catch (Exception e) {
            return 5;
        }
    }

    public long getMaxNumBoard() {
        try {
            return this.config.getLong("maxMaxNumBoard");
        }
        catch (Exception e) {
            return 3;
        }
    }

    public String getMoneyBetList() {
        try {
            return this.config.getString("moneyBetList");
        }
        catch (Exception e) {
            return "100,200,500,1000,2000,5000,10000";
        }
    }

    public void initconfig() {
        String path = VBeePath.basePath;
        File file = new File(path + "/conf/bot.json");
        if (file.exists()) {
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
                e.printStackTrace();
                this.config = new JSONObject();
            }
        } else {
            this.config = new JSONObject();
        }
    }
}

