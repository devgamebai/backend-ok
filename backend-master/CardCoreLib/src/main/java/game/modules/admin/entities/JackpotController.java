/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package game.modules.admin.entities;

import bitzero.server.entities.User;
import com.vinplay.vbee.common.config.VBeePath;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameServer;
import game.utils.GameUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JackpotController {
    private JSONObject config;
    private static JackpotController ins = null;
    public Set<String> waitingJackpotPlayers = new HashSet<String>();
    public Map<String, Integer> jackpotPlayers = new ConcurrentHashMap<String, Integer>();
    private static String basePath = VBeePath.basePath;

    private JackpotController() {
        this.initFromFile();
        this.initWaitingPlayers();
    }

    public static JackpotController instance() {
        if (ins == null) {
            ins = new JackpotController();
        }
        return ins;
    }

    public synchronized boolean checkJackpotPlayers(User user, GameRoom room) {
        int random;
        String nickName = user.getName();
        if (!this.jackpotPlayers.containsKey(nickName) && this.waitingJackpotPlayers.contains(nickName) && (random = GameUtils.rd.nextInt(1)) == 0) {
            this.jackpotPlayers.put(nickName, room.getId());
            room.getGameServer().choNoHu(nickName);
            return true;
        }
        return false;
    }

    private void initWaitingPlayers() {
        try {
            JSONArray arr = this.config.getJSONArray("jackpotPlayers");
            for (int i = 0; i < arr.length(); ++i) {
                String nickName = arr.getString(i);
                this.waitingJackpotPlayers.add(nickName);
            }
        }
        catch (JSONException arr) {
            // empty catch block
        }
    }

    private void initFromFile() {
        String path = System.getProperty("user.dir");
        File file = new File(basePath.concat("/conf/jackpot.json"));
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

