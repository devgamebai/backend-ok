/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.json.JSONObject
 */
package game.modules.minigame.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.json.JSONObject;

public class MiniGameConfig {
    public JSONObject config;
    private static MiniGameConfig miniGameConfig = null;

    private MiniGameConfig() {
        this.initconfig();
    }

    public static MiniGameConfig instance() {
        if (miniGameConfig == null) {
            miniGameConfig = new MiniGameConfig();
        }
        return miniGameConfig;
    }

    public void initconfig() {
        String path = System.getProperty("user.dir");
        File file = new File(path + "/conf/minigame.json");
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
        }
    }
}

