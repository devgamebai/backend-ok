package bitzero.server.entities.managers;

import bitzero.engine.sessions.ISession;
import bitzero.server.core.ICoreService;
import bitzero.server.entities.Room;
import bitzero.server.entities.User;
import bitzero.server.entities.Zone;
import java.util.List;

public interface IUserManager extends ICoreService {
     List getUserByName(String var1);

     User getUserById(int var1);

     User getUserBySession(ISession var1);

     List getAllUsers();

     List getAllSessions();

     void addUser(User var1);

     void removeUser(User var1);

     void removeUser(String var1);

     void removeUser(int var1);

     void removeUser(ISession var1);

     void disconnectUser(User var1);

     void disconnectUser(String var1);

     void disconnectUser(int var1);

     void disconnectUser(ISession var1);

     boolean containsId(int var1);

     boolean containsName(String var1);

     boolean containsSessions(ISession var1);

     boolean containsUser(User var1);

     Zone getOwnerZone();

     void setOwnerZone(Zone var1);

     Room getOwnerRoom();

     void setOwnerRoom(Room var1);

     int getUserCount();

     int getUserCountByName();

     int getHighestCCU();
}
