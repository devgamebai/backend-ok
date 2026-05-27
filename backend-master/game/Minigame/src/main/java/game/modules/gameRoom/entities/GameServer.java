/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.config.ConfigHandle
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.data.DataCmd
 */
package game.modules.gameRoom.entities;

import bitzero.server.config.ConfigHandle;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.DataCmd;
import game.modules.gameRoom.entities.GameRoom;
import game.sam.server.SamGameServer;

public abstract class GameServer {
    public static final String GAME_NAME = ConfigHandle.instance().get("games");
    public GameRoom room;

    static GameServer createNewGameServer(GameRoom room) {
        if (GAME_NAME.equalsIgnoreCase("Sam")) {
            return new SamGameServer(room);
        }
        return null;
    }

    public abstract void onGameUserReturn(User var1);

    public abstract void onGameUserEnter(User var1);

    public abstract void onGameUserExit(User var1);

    public abstract void onGameMessage(User var1, DataCmd var2);
}

