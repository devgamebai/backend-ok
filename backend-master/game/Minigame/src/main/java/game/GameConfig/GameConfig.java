/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  com.google.gson.Gson
 *  com.vinplay.dailyQuest.DailyQuestConfig
 */
package game.GameConfig;

import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import com.vinplay.dailyQuest.DailyQuestConfig;
import game.GameConfig.ConfigGame.BotJackpotConfig.GalaxyBotConfig;
import game.GameConfig.ConfigGame.BotJackpotConfig.MiniPokerBotConfig;
import game.GameConfig.ConfigGame.BotJackpotConfig.Slot3x3BotConfig;
import game.GameConfig.ConfigGame.MinigameConfig.MinipokerGameConfig;
import game.GameConfig.ConfigGame.MinigameConfig.Slot3x3GameConfig;
import game.GameConfig.ConfigGame.SlotMultiJackpotConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class GameConfig {
    private static GameConfig instance = null;
    public static Gson gson = new Gson();
    public MiniPokerBotConfig miniPokerBotConfig = new MiniPokerBotConfig();
    public MinipokerGameConfig minipokerGameConfig = new MinipokerGameConfig();
    public Slot3x3BotConfig slot3x3BotConfig = new Slot3x3BotConfig();
    public Slot3x3GameConfig slot3x3GameConfig = new Slot3x3GameConfig();
    public GalaxyBotConfig galaxyBotConfig = new GalaxyBotConfig();
    public SlotMultiJackpotConfig slotMultiJackpotConfig = new SlotMultiJackpotConfig();
    public DailyQuestConfig dailyQuestConfig = new DailyQuestConfig();

    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }

    public void init() {
        String json = "";
        json = GameConfig.loadConfig("MinipokerBotConfig.json");
        this.miniPokerBotConfig = (MiniPokerBotConfig)gson.fromJson(json, MiniPokerBotConfig.class);
        json = GameConfig.loadConfig("MinipokerGameConfig.json");
        this.minipokerGameConfig = (MinipokerGameConfig)gson.fromJson(json, MinipokerGameConfig.class);
        json = GameConfig.loadConfig("Slot3x3BotConfig.json");
        this.slot3x3BotConfig = (Slot3x3BotConfig)gson.fromJson(json, Slot3x3BotConfig.class);
        json = GameConfig.loadConfig("Slot3x3GameConfig.json");
        this.slot3x3GameConfig = (Slot3x3GameConfig)gson.fromJson(json, Slot3x3GameConfig.class);
        json = GameConfig.loadConfig("GalaxyBotConfig.json");
        this.galaxyBotConfig = (GalaxyBotConfig)gson.fromJson(json, GalaxyBotConfig.class);
        json = GameConfig.loadConfig("SlotMultiJackpotConfig.json");
        this.slotMultiJackpotConfig = (SlotMultiJackpotConfig)gson.fromJson(json, SlotMultiJackpotConfig.class);
    }

    public static String loadConfig(String fileName) {
        String path = System.getProperty("user.dir");
        File file = new File(path + "/game/Minigame/config/game/" + fileName);
        StringBuffer contents = new StringBuffer();
        BufferedReader bufferedReader = null;
        try {
            InputStreamReader r = new InputStreamReader((InputStream)new FileInputStream(file), "UTF-8");
            bufferedReader = new BufferedReader(r);
            String text = null;
            while ((text = bufferedReader.readLine()) != null) {
                contents.append(text).append(System.getProperty("line.separator"));
            }
        }
        catch (UnsupportedEncodingException e) {
            Debug.trace((Object[])new Object[]{e});
        }
        catch (FileNotFoundException e) {
            Debug.trace((Object[])new Object[]{e});
        }
        catch (IOException e) {
            Debug.trace((Object[])new Object[]{e});
        }
        return contents.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public synchronized void setFileConfig(String fileName, Object instance) {
        String path = System.getProperty("user.dir");
        File file = new File(path + "/game/Minigame/config/game/" + fileName);
        String json = gson.toJson(instance);
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file), "utf-8"));
            writer.write(json);
        }
        catch (Exception e) {
            Debug.trace((Object[])new Object[]{e});
        }
        finally {
            try {
                writer.close();
            }
            catch (Exception exception) {}
        }
    }
}

