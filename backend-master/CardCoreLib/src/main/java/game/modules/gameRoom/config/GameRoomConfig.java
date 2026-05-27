/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.gameRoom.config;

import bitzero.util.common.business.CommonHandle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.vinplay.vbee.common.config.VBeePath;
import org.json.JSONException;
import org.json.JSONObject;

public class GameRoomConfig {
    public JSONObject config;
    private static GameRoomConfig gameRoom = null;

    private GameRoomConfig() {
        this.initconfig();
    }

    public static GameRoomConfig instance() {
        if (gameRoom == null) {
            gameRoom = new GameRoomConfig();
        }
        return gameRoom;
    }

    public long getJoinRoomIntervalTime() {
        try {
            return this.config.getLong("join_room_interval_time");
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
            return 10000;
        }
    }

    public long getBigWin() {
        try {
            return this.config.getLong("big_win");
        }
        catch (JSONException e) {
            CommonHandle.writeErrLog((Throwable)e);
            return 100000;
        }
    }

    public void initconfig() {
        // String path = System.getProperty("user.dir");
        String path = VBeePath.basePath;
        File file = new File(path + "/conf/gameroom.json");
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

