package game.modules.XocDia;

import bitzero.server.BitZeroServer;
import bitzero.server.api.CreateRoomSettings;
import bitzero.server.entities.Room;
import bitzero.server.entities.Zone;
import bitzero.server.entities.managers.IZoneManager;
import bitzero.util.common.business.Debug;

public class XocDiaRoomManager {
    public static final int ZONE = 0;
    public static final String SEPARATOR = "_";
    public static final String XOC_DIA_NAME = "_";

    public XocDiaRoomManager() {
    }

    public static Room createRoomXocDia() {
        Zone zone = BitZeroServer.getInstance().getZoneManager().getZoneById(ZONE);
        CreateRoomSettings roomSettings = new CreateRoomSettings();
        roomSettings.setName(SEPARATOR + System.currentTimeMillis() + SEPARATOR);
        roomSettings.setGroupId(XOC_DIA_NAME);
        roomSettings.setMaxUsers(100000);
        roomSettings.setMaxSpectators(0);
        roomSettings.setDynamic(false);
        roomSettings.setGame(true);
        try {
            Room room = BitZeroServer.getInstance().getAPIManager().getBzApi().createRoom(zone, roomSettings, null);
            room.setDynamic(false);
            Debug.trace(new Object[]{"createRoom"});
            return room;
        } catch (bitzero.server.exceptions.BZCreateRoomException e) {
            Debug.trace(new Object[]{"createRoom error", e.getMessage()});
            return null;
        }
    }

    public static synchronized Room getRoomToJoin() {
        Zone zone = BitZeroServer.getInstance().getZoneManager().getZoneById(ZONE);
        Room room = (Room) zone.getRoomListFromGroup(XOC_DIA_NAME).get(0);
        return room;
    }
}
