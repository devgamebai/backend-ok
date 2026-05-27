package game.GameConfig;

import bitzero.util.common.business.Debug;
import com.google.gson.Gson;
import game.GameConfig.BotJackpotConfig.*;
import game.GameConfig.RollLoseConfig.Slot11IconWildLienTucRollLoseConfig;
import game.GameConfig.RollLoseConfig.Slot7IconRollLoseConfig;
import game.GameConfig.RollLoseConfig.Slot7IconWildRollLoseConfig;
import game.GameConfig.RollLoseConfig.Slot9IconRollLoseConfig;
import game.GameConfig.RollLoseConfig.ThanTaiRollLoseConfig;
import game.GameConfig.SlotConfig.*;

import java.io.*;

public class GameConfig {

    private static GameConfig instance = null;

    public static Gson gson = new Gson();

    public SlotAuditionBotConfig slotAuditionBotConfig = new SlotAuditionBotConfig();
    public SlotBentleyBotConfig slotBentleyBotConfig = new SlotBentleyBotConfig();
    public SlotMayBachBotConfig slotMayBachBotConfig = new SlotMayBachBotConfig();
    public SlotRangeRoverBotConfig slotRangeRoverBotConfig = new SlotRangeRoverBotConfig();
    public SlotRollRoyceBotConfig slotRollRoyceBotConfig = new SlotRollRoyceBotConfig();
    public SlotSpartanBotConfig slotSpartanBotConfig = new SlotSpartanBotConfig();
    public SlotTamHungBotConfig slotTamHungBotConfig = new SlotTamHungBotConfig();
    public SlotChiemtinhBotConfig slotChiemtinhBotConfig = new SlotChiemtinhBotConfig();
    public SlotSexyGirlBotConfig slotSexyGirlBotConfig = new SlotSexyGirlBotConfig();
    public SlotBikiniBotConfig slotBikiniBotConfig = new SlotBikiniBotConfig();

    public Slot7IconConfig slot7IconConfig = new Slot7IconConfig();
    public Slot7IconWildConfig slot7IconWildConfig = new Slot7IconWildConfig();
    public Slot9IconConfig slot9IconConfig = new Slot9IconConfig();
    public Slot11IconWildLienTucConfig slot11IconWildLienTucConfig = new Slot11IconWildLienTucConfig();
    public ThanTaiConfig thanTaiConfig = new ThanTaiConfig();

    public Slot11IconWildLienTucRollLoseConfig slot11IconWildLienTucRollLoseConfig = new Slot11IconWildLienTucRollLoseConfig();
    public ThanTaiRollLoseConfig thanTaiRollLoseConfig = new ThanTaiRollLoseConfig();
    public Slot7IconRollLoseConfig slot7IconRollLoseConfig = new Slot7IconRollLoseConfig();
    public Slot7IconWildRollLoseConfig slot7IconWildRollLoseConfig = new Slot7IconWildRollLoseConfig();
    public Slot9IconRollLoseConfig slot9IconRollLoseConfig = new Slot9IconRollLoseConfig();

    public SlotMultiJackpotConfig slotMultiJackpotConfig = new SlotMultiJackpotConfig();
    public game.GameConfig.ConfigGame.MinigameConfig.Slot3x3GameConfig slot3x3GameConfig = new game.GameConfig.ConfigGame.MinigameConfig.Slot3x3GameConfig();

    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }

    public void init() {
        String json = "";
        json = loadConfig("SlotAuditionBotConfig.json");
        this.slotAuditionBotConfig = gson.fromJson(json, SlotAuditionBotConfig.class);

        json = loadConfig("SlotBentleyBotConfig.json");
        this.slotBentleyBotConfig = gson.fromJson(json, SlotBentleyBotConfig.class);
        
        json = loadConfig("SlotChiemtinhBotConfig.json");
        this.slotChiemtinhBotConfig = gson.fromJson(json, SlotChiemtinhBotConfig.class);

        json = loadConfig("SlotBikiniBotConfig.json");
        this.slotBikiniBotConfig = gson.fromJson(json, SlotBikiniBotConfig.class);

        json = loadConfig("SlotMayBachBotConfig.json");
        this.slotMayBachBotConfig = gson.fromJson(json, SlotMayBachBotConfig.class);

        json = loadConfig("SlotRangeRoverBotConfig.json");
        this.slotRangeRoverBotConfig = gson.fromJson(json, SlotRangeRoverBotConfig.class);

        json = loadConfig("SlotRollRoyceBotConfig.json");
        this.slotRollRoyceBotConfig = gson.fromJson(json, SlotRollRoyceBotConfig.class);

        json = loadConfig("SlotSpartanBotConfig.json");
        this.slotSpartanBotConfig = gson.fromJson(json, SlotSpartanBotConfig.class);

        json = loadConfig("SlotTamHungBotConfig.json");
        this.slotTamHungBotConfig = gson.fromJson(json, SlotTamHungBotConfig.class);

        json = loadConfig("Slot7IconConfig.json");
        this.slot7IconConfig = gson.fromJson(json, Slot7IconConfig.class);

        json = loadConfig("Slot7IconWildConfig.json");
        this.slot7IconWildConfig = gson.fromJson(json, Slot7IconWildConfig.class);

        json = loadConfig("Slot9IconConfig.json");
        this.slot9IconConfig = gson.fromJson(json, Slot9IconConfig.class);

        json = loadConfig("Slot11IconWildLienTucConfig.json");
        this.slot11IconWildLienTucConfig = gson.fromJson(json, Slot11IconWildLienTucConfig.class);

        json = loadConfig("ThanTaiConfig.json");
        if (json != null && !json.trim().isEmpty()) {
            this.thanTaiConfig = gson.fromJson(json, ThanTaiConfig.class);
        }

        json = loadConfig("Slot11IconWildLienTucRollLoseConfig.json");
        this.slot11IconWildLienTucRollLoseConfig = gson.fromJson(json, Slot11IconWildLienTucRollLoseConfig.class);

        json = loadConfig("ThanTaiRollLoseConfig.json");
        if (json != null && !json.trim().isEmpty()) {
            this.thanTaiRollLoseConfig = gson.fromJson(json, ThanTaiRollLoseConfig.class);
        }

        json = loadConfig("Slot9IconRollLoseConfig.json");
        this.slot9IconRollLoseConfig = gson.fromJson(json, Slot9IconRollLoseConfig.class);

        json = loadConfig("Slot7IconRollLoseConfig.json");
        this.slot7IconRollLoseConfig = gson.fromJson(json, Slot7IconRollLoseConfig.class);

        json = loadConfig("Slot7IconWildRollLoseConfig.json");
        this.slot7IconWildRollLoseConfig = gson.fromJson(json, Slot7IconWildRollLoseConfig.class);

        json = loadConfig("SlotMultiJackpotConfig.json");
        this.slotMultiJackpotConfig = gson.fromJson(json, SlotMultiJackpotConfig.class);
    }

    public static String loadConfig(String fileName) {
        String path = System.getProperty("user.dir");
        File file = new File(path + "/game/slot/config/game/" + fileName);
        StringBuffer contents = new StringBuffer();
        BufferedReader bufferedReader = null;

        try {
            Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
            bufferedReader = new BufferedReader(r);
            String text = null;
            while ((text = bufferedReader.readLine()) != null) {
                contents.append(text).append(System.getProperty("line.separator"));
            }
        } catch (UnsupportedEncodingException e) {
            Debug.trace(e);
        } catch (FileNotFoundException e) {
            Debug.trace(e);
        } catch (IOException e) {
            Debug.trace(e);
        }finally {
        	if(bufferedReader!=null) {
        		try {
    				bufferedReader.close();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        	}
		}
        return contents.toString();
    }

    public synchronized void setFileConfig(String fileName, Object instance) {
        String path = System.getProperty("user.dir");
        File file = new File(path + "/game/slot/config/game/" + fileName);
        String json = gson.toJson(instance);
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "utf-8"));
            writer.write(json);
        } catch (Exception e) {
            Debug.trace(e);
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {

            }
        }
    }
}
