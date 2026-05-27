/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.modules.tour.control;

import bitzero.server.entities.User;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.GameRoomManager;
import game.modules.gameRoom.entities.GameRoomSetting;
import game.modules.gameRoom.entities.GameServer;
import game.modules.tour.control.Tour;
import game.modules.tour.control.TourUserInfo;
import game.modules.tour.log.LogEntry;
import game.utils.LoggerUtils;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class RoomTourGroup {
    public Queue<GameRoom> emptyRooms = new LinkedBlockingDeque<GameRoom>();
    public Vector<GameRoom> playingRooms = new Vector();
    public Tour tour;
    public Map<Integer, GameRoom> allGameRoom = new ConcurrentHashMap<Integer, GameRoom>();

    public void endTour() {
        for (Map.Entry<Integer, GameRoom> entry : this.allGameRoom.entrySet()) {
            GameRoom room = entry.getValue();
            room.getGameServer().destroy();
        }
    }

    public synchronized boolean combineRoom(GameRoom room) {
        LoggerUtils.debug("combineRoom", room.getId());
        this.removeGameRoom(room);
        GameRoom otherRoom = null;
        boolean flag = false;
        for (int i = 0; i < this.playingRooms.size(); ++i) {
            otherRoom = this.playingRooms.get(i);
            if (room.getId() == otherRoom.getId()) continue;
            int size1 = room.getUserCount();
            int size2 = otherRoom.getUserCount();
            LoggerUtils.debug("tour", "checkRoomToCombine room", room.getId(), size1, "other", otherRoom.getId(), size2);
            if (size1 == 0 || size2 == 0 || size1 + size2 > room.setting.limitPlayer) continue;
            this.removeGameRoom(otherRoom);
            flag = true;
            break;
        }
        if (otherRoom != null && flag) {
            for (Map.Entry<String, User> entry : room.userManager.entrySet()) {
                User user = entry.getValue();
                TourUserInfo info = this.tour.findTourInfoByUser(user);
                LogEntry logEntry = new LogEntry(LogEntry.ACTION.COMBINE_ROOM);
                if (info != null) {
                    logEntry.initTourInfo(info);
                }
                logEntry.firstRoom = room.getId();
                logEntry.secondRoom = otherRoom.getId();
                this.tour.addLogEntry(logEntry);
                GameRoomManager.instance().leaveRoom(user, room);
                GameRoomManager.instance().joinRoom(user, otherRoom, false);
            }
            this.recycleRoom(otherRoom);
            return true;
        }
        this.recycleRoom(room);
        return false;
    }

    private void removeGameRoom(GameRoom room) {
        this.emptyRooms.remove(room);
        this.playingRooms.remove(room);
    }

    private GameRoom createRoom() {
        GameRoom room = new GameRoom(this.tour.roomSetting);
        room.setProperty("TOUR_INFO", this.tour);
        this.allGameRoom.put(room.getId(), room);
        return room;
    }

    public void putUserToRoom(User user, TourUserInfo tourUserInfo) {
        if (tourUserInfo.timeBuyTicket > tourUserInfo.timeOutTour) {
            boolean isReconnect = true;
            GameRoom room = this.getRoomByUser(user);
            if (room == null) {
                room = this.getAvailableGameRoom();
                isReconnect = false;
            }
            tourUserInfo.outTourTimeStamp = Long.MAX_VALUE;
            user.setProperty((Object)"TOUR_USER_INFO", (Object)tourUserInfo);
            GameRoomManager.instance().joinRoom(user, room, isReconnect);
            LoggerUtils.debug("tour", "RoomTourGroup putUserToRoom", user.getName(), "tour", tourUserInfo.tourId, "room", room.getId());
            LogEntry entry = new LogEntry(LogEntry.ACTION.JOIN_ROOM);
            entry.initTourInfo(tourUserInfo);
            entry.firstRoom = room.getId();
            entry.user = user.getName();
            this.tour.addLogEntry(entry);
            this.recycleRoom(room);
        }
    }

    private synchronized void recycleRoom(GameRoom room) {
        this.removeGameRoom(room);
        if (room.getUserCount() == 0) {
            this.emptyRooms.add(room);
            room.getGameServer().destroy();
        } else {
            this.playingRooms.add(room);
        }
    }

    public synchronized GameRoom getAvailableGameRoom() {
        GameRoom room = null;
        for (int i = 0; i < this.playingRooms.size(); ++i) {
            room = this.playingRooms.get(i);
            boolean isFull = room.isFull();
            if (isFull || room.isCombining) continue;
            return room;
        }
        room = null;
        if (room == null) {
            room = this.emptyRooms.poll();
        }
        if (room == null) {
            room = this.createRoom();
            this.playingRooms.add(room);
        }
        return room;
    }

    public synchronized GameRoom getRoomByUser(User user) {
        for (int i = 0; i < this.playingRooms.size(); ++i) {
            GameRoom room = this.playingRooms.get(i);
            for (Map.Entry<String, User> entry : room.userManager.entrySet()) {
                if (!entry.getKey().equalsIgnoreCase(user.getName())) continue;
                return room;
            }
        }
        return null;
    }

    public void destroy() {
        this.emptyRooms.clear();
        this.playingRooms.clear();
        this.allGameRoom.clear();
    }
}

