/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  org.json.JSONObject
 */
package game.modules.gameRoom.entities;

import bitzero.server.entities.User;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.ThongTinThangLon;
import game.utils.GameUtils;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

public abstract class GameServer {
    public static final String USER_CHAIR = "user_chair";
    public GameRoom room;

    public static GameServer createNewGameServer(GameRoom room) {
        String gameServerClassPath = GameUtils.gameServerClassPath;
        try {
            GameServer mainServer = (GameServer)Class.forName(gameServerClassPath).newInstance();
            mainServer.init(room);
            return mainServer;
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            return null;
        }
    }

    public abstract void init(GameRoom var1);

    public abstract void init();

    public abstract void destroy();

    public abstract void onGameUserReturn(User var1);

    public abstract void onGameUserEnter(User var1);

    public abstract void onGameUserExit(User var1);

    public abstract void onGameUserDis(User var1);

    public abstract void onGameMessage(User var1, DataCmd var2);

    public abstract String toString();

    public abstract JSONObject toJONObject();

    public abstract void onNoHu(ThongTinThangLon var1);

    public abstract void choNoHu(String var1);

    public void sendMsgExceptMe(BaseMsg msg, User user) {
        if (user != null && this.room != null) {
            for (Map.Entry<String, User> entry : this.room.userManager.entrySet()) {
                String userName = entry.getKey();
                User curentUser = entry.getValue();
                if (curentUser == null || userName.equalsIgnoreCase(user.getName())) continue;
                ExtensionUtility.getExtension().send(msg, curentUser);
            }
        }
    }

    public void send(BaseMsg cmd, User user) {
        if (user != null) {
            ExtensionUtility.getExtension().send(cmd, user);
        }
    }

    public int countPlayers() {
        return 0;
    }

    public boolean isPlaying() {
        return false;
    }
}

