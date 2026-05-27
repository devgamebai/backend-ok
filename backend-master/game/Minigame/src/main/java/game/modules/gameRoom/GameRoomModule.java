/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.common.business.Debug
 *  com.vinplay.vbee.common.hazelcast.HazelcastLoader
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package game.modules.gameRoom;

import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import com.vinplay.vbee.common.hazelcast.HazelcastLoader;
import com.vinplay.vbee.common.rmq.RMQApi;
import game.entities.PlayerInfo;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.gameRoom.cmd.rev.JoinGameRoomCmd;
import game.modules.gameRoom.cmd.send.JoinGameRoomFailMsg;
import game.modules.gameRoom.cmd.send.ReconnectGameRoomFailMsg;
import game.modules.gameRoom.cmd.send.SendGameRoomConfig;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomGroup;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.utils.GameUtils;

public class GameRoomModule
extends BaseClientRequestHandler {
    public void init() {
        try {
            HazelcastLoader.start();
            RMQApi.start((String)"config/rmq.properties");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        GameRoomManager.instance();
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        this.getParentExtension().addEventListener((IBZEventType)GameEventType.GAME_ROOM_USER_JOIN, (IBZEventListener)this);
        this.getParentExtension().addEventListener((IBZEventType)GameEventType.GAME_ROOM_USER_LEAVE, (IBZEventListener)this);
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        User user;
        GameRoom room;
        if (ibzevent.getType() == GameEventType.GAME_ROOM_USER_JOIN) {
            user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            room = (GameRoom)ibzevent.getParameter((IBZEventParam)GameEventParam.GAMEROOM);
            Boolean isReconnect = (Boolean)ibzevent.getParameter((IBZEventParam)GameEventParam.IS_RECONNECT);
            this.userJoinRoomSuccess(user, room, isReconnect);
        }
        if (ibzevent.getType() == GameEventType.GAME_ROOM_USER_LEAVE) {
            user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            room = (GameRoom)ibzevent.getParameter((IBZEventParam)GameEventParam.GAMEROOM);
            Debug.trace((Object[])new Object[]{"Event leave room", user.getName()});
            this.userLeaveRoom(user, room);
        }
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDisconnected(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 3001: {
                this.joinGameRoom(user, dataCmd);
                break;
            }
            case 3002: {
                this.reconnectGameRoom(user, dataCmd);
                break;
            }
            case 3003: {
                this.requestConfig(user, dataCmd);
                break;
            }
            default: {
                GameRoom room = (GameRoom)user.getProperty("GAME_ROOM");
                if (room != null) {
                    GameServer gs = room.getGameServer();
                    gs.onGameMessage(user, dataCmd);
                    break;
                }
                Debug.trace((Object[])new Object[]{"Room null ", user.getName()});
            }
        }
    }

    public void joinGameRoom(User user, DataCmd dataCmd) {
        JoinGameRoomCmd cmd = new JoinGameRoomCmd(dataCmd);
        boolean check = GameUtils.infoCheck(user);
        if (!check) {
            JoinGameRoomFailMsg msg = new JoinGameRoomFailMsg();
            msg.Error = 1;
            this.send((BaseMsg)msg, user);
            return;
        }
        GameRoomGroup group = GameRoomManager.instance().getGroup(cmd);
        if (group == null) {
            Debug.trace((Object[])new Object[]{"Khong ton tai ban choi thoa man", cmd.moneyType, cmd.maxUserPerRoom, cmd.moneyBet});
            JoinGameRoomFailMsg msg = new JoinGameRoomFailMsg();
            msg.Error = 2;
            this.send((BaseMsg)msg, user);
            return;
        }
        boolean result = this.preJoinRoom(user, group.setting);
        if (!result) {
            Debug.trace("User khong du dieu kien vao ban choi");
            JoinGameRoomFailMsg msg = new JoinGameRoomFailMsg();
            msg.Error = 3;
            this.send((BaseMsg)msg, user);
            return;
        }
        int res = group.joinRoom(user);
        if (res != 0) {
            JoinGameRoomFailMsg msg = new JoinGameRoomFailMsg();
            msg.Error = 4;
            this.send((BaseMsg)msg, user);
        }
    }

    public boolean preJoinRoom(User user, GameRoomSetting setting) {
        GameMoneyInfo moneyInfo = new GameMoneyInfo(user, setting);
        boolean result = moneyInfo.startGameUpdateMoney();
        if (result) {
            user.setProperty("GAME_MONEY_INFO", moneyInfo);
            return true;
        }
        return false;
    }

    public void userJoinRoomSuccess(User user, GameRoom room, boolean isReconnect) {
        GameServer gs = room.getGameServer();
        if (isReconnect) {
            gs.onGameUserReturn(user);
        } else {
            gs.onGameUserEnter(user);
        }
    }

    public void userLeaveRoom(User user, GameRoom room) {
        GameServer gs = room.getGameServer();
        gs.onGameUserExit(user);
        Debug.trace((Object[])new Object[]{"userLeaveRoom", user.getName(), room.getId()});
    }

    public void userDisconnected(User user) {
        GameRoom room = (GameRoom)user.getProperty("GAME_ROOM");
        if (room != null) {
            GameServer gs = room.getGameServer();
            gs.onGameUserExit(user);
            Debug.trace((Object[])new Object[]{"userDisconnected", user.getName(), room.getId()});
        }
    }

    private void reconnectGameRoom(User user, DataCmd dataCmd) {
        boolean isHold = PlayerInfo.getIsHold(user.getId());
        if (isHold) {
            int roomId = PlayerInfo.getHoldRoom(user.getId());
            GameRoom room = GameRoomManager.instance().getGameRoomById(roomId);
            if (room != null) {
                room.group.joinRoom(user, room, true);
            }
        } else {
            ReconnectGameRoomFailMsg msg = new ReconnectGameRoomFailMsg();
            this.send((BaseMsg)msg, user);
        }
    }

    private void requestConfig(User user, DataCmd dataCmd) {
        SendGameRoomConfig msg = new SendGameRoomConfig();
        this.send((BaseMsg)msg, user);
    }
}

