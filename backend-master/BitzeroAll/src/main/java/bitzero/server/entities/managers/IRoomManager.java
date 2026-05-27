package bitzero.server.entities.managers;

import bitzero.server.api.CreateRoomSettings;
import bitzero.server.core.ICoreService;
import bitzero.server.entities.Room;
import bitzero.server.entities.User;
import bitzero.server.entities.Zone;
import bitzero.server.exceptions.BZCreateRoomException;
import bitzero.server.exceptions.BZRoomException;
import java.util.List;

public interface IRoomManager extends ICoreService {
     void addRoom(Room var1);

     Room createRoom(CreateRoomSettings var1) throws BZCreateRoomException;

     Room createRoom(CreateRoomSettings var1, User var2) throws BZCreateRoomException;

     List getGroups();

     void addGroup(String var1);

     void removeGroup(String var1);

     boolean containsGroup(String var1);

     boolean containsRoom(int var1);

     boolean containsRoom(String var1);

     boolean containsRoom(Room var1);

     boolean containsRoom(int var1, String var2);

     boolean containsRoom(String var1, String var2);

     boolean containsRoom(Room var1, String var2);

     Room getRoomById(int var1);

     Room getRoomByName(String var1);

     List getRoomList();

     List getRoomListFromGroup(String var1);

     void checkAndRemove(Room var1);

     void removeRoom(Room var1);

     void removeRoom(int var1);

     void removeRoom(String var1);

     Zone getOwnerZone();

     void setOwnerZone(Zone var1);

     void removeUser(User var1);

     void removeUser(User var1, Room var2);

     void changeRoomName(Room var1, String var2) throws BZRoomException;

     void changeRoomPasswordState(Room var1, String var2);

     void changeRoomCapacity(Room var1, int var2, int var3);

     void setDefaultRoomPlayerIdGeneratorClass(Class var1);
}
