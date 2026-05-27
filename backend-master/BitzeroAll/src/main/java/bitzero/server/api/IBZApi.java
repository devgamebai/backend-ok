package bitzero.server.api;

import bitzero.engine.sessions.ISession;
import bitzero.server.entities.Room;
import bitzero.server.entities.User;
import bitzero.server.entities.Zone;
import bitzero.server.entities.managers.BanMode;
import bitzero.server.exceptions.BZCreateRoomException;
import bitzero.server.exceptions.BZJoinRoomException;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.util.IDisconnectionReason;
import java.util.Collection;

public interface IBZApi {
     boolean checkSecurePassword(ISession var1, String var2, String var3);

     void login(ISession var1, byte var2, User var3);

     void loginWebsocket(ISession var1, byte var2, User var3);

     void logout(User var1);

     void kickUser(User var1, User var2, String var3, int var4);

     void banUser(User var1, User var2, String var3, BanMode var4, int var5, int var6);

     void disconnectUser(User var1);

     void disconnectUser(User var1, Boolean var2);

     void disconnectUser(User var1, IDisconnectionReason var2);

     void disconnect(ISession var1);

     Room createRoom(Zone var1, CreateRoomSettings var2, User var3) throws BZCreateRoomException;

     Room createRoom(Zone var1, CreateRoomSettings var2, User var3, boolean var4, Room var5) throws BZCreateRoomException;

     Room createRoom(Zone var1, CreateRoomSettings var2, User var3, boolean var4, Room var5, boolean var6, boolean var7) throws BZCreateRoomException;

     User getUserById(int var1);

     User getUserByName(String var1);

     User getUserBySession(ISession var1);

     void joinRoom(User var1, Room var2, int var3) throws BZJoinRoomException;

     void joinRoom(User var1, Room var2) throws BZJoinRoomException;

     void joinRoom(User var1, Room var2, String var3, boolean var4, Room var5) throws BZJoinRoomException;

     void joinRoom(User var1, Room var2, String var3, boolean var4, Room var5, int var6, boolean var7) throws BZJoinRoomException;

     void joinRoom(User var1, Room var2, String var3, boolean var4, Room var5, int var6) throws BZJoinRoomException;

     void joinRoom(User var1, Room var2, String var3, boolean var4, Room var5, boolean var6, boolean var7) throws BZJoinRoomException;

     void leaveRoom(User var1, Room var2);

     void leaveRoom(User var1, Room var2, boolean var3, boolean var4);

     void removeRoom(Room var1);

     void removeRoom(Room var1, boolean var2, boolean var3);

     void sendPublicMessage(Room var1, User var2, BaseMsg var3);

     void sendPrivateMessage(User var1, User var2, BaseMsg var3);

     void sendModeratorMessage(User var1, String var2, String[] var3, Collection var4);

     void sendAdminMessage(User var1, String var2, String[] var3, Collection var4);
}
