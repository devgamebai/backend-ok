package bitzero.server.api;

import bitzero.engine.io.IResponse;
import bitzero.engine.io.Response;
import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.api.cmd.GenericMessageMsg;
import bitzero.server.config.DefaultConstants;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.core.BZEvent;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.entities.Room;
import bitzero.server.entities.User;
import bitzero.server.entities.Zone;
import bitzero.server.entities.data.ISFSObject;
import bitzero.server.entities.data.SFSObject;
import bitzero.server.entities.managers.BanMode;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.exceptions.BZCreateRoomException;
import bitzero.server.exceptions.BZErrorCode;
import bitzero.server.exceptions.BZErrorData;
import bitzero.server.exceptions.BZJoinRoomException;
import bitzero.server.exceptions.BZLoginException;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.util.ClientDisconnectionReason;
import bitzero.server.util.CryptoUtils;
import bitzero.server.util.IDisconnectionReason;
import bitzero.server.util.UsersUtil;
import bitzero.util.config.bean.ConstantMercury;
import bitzero.util.datacontroller.business.DataController;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BZApi implements IBZApi {
     protected final BitZeroServer bz;
     protected final Logger log = LoggerFactory.getLogger(this.getClass());
     protected IUserManager globalUserManager;
     private final LoginErrorHandler loginErrorHandler = new LoginErrorHandler();

     public BZApi(BitZeroServer bz) {
          this.bz = bz;
          this.globalUserManager = bz.getUserManager();
     }

     public User getUserById(int userId) {
          return this.globalUserManager.getUserById(userId);
     }

     public User getUserByName(String name) {
          return this.globalUserManager.getUserByName(name);
     }

     public User getUserBySession(ISession session) {
          return this.globalUserManager.getUserBySession(session);
     }

     public void kickUser(User userToKick, User modUser, String kickMessage, int delaySeconds) {
          this.log.info("Kicking user: " + userToKick);
          this.bz.getBannedUserManager().kickUser(userToKick, modUser, kickMessage, delaySeconds);
     }

     public void banUser(User userToBan, User modUser, String banMessage, BanMode mode, int durationMinutes, int delaySeconds) {
          this.bz.getBannedUserManager().banUser(userToBan, modUser, durationMinutes, mode, banMessage, banMessage, delaySeconds);
     }

     public void disconnect(ISession session) {
          if (session == null) {
               throw new BZRuntimeException("Unexpected, cannot disconnect session. Session object is null.");
          } else {
               User lostUser = this.globalUserManager.getUserBySession(session);
               if (lostUser != null) {
                    this.disconnectUser(lostUser);
               } else if (session.isConnected()) {
                    try {
                         session.close();
                    } catch (IOException var4) {
                         throw new BZRuntimeException(var4);
                    }
               }

          }
     }

     public void disconnectUser(User user, IDisconnectionReason reason) {
          user.getSession().setSystemProperty("disconnectionReason", reason);
          this.bz.getAPIManager().getResApi().notifyClientSideDisconnection(user, reason);
          user.getSession().setMarkedForEviction();
     }

     public void disconnectUser(User user) {
          this.disconnectUser(user, false);
     }

     public void disconnectUser(User user, Boolean forceLogin) {
          if (user == null) {
               throw new BZRuntimeException("Cannot disconnect user, User object is null.");
          } else {
               Zone zone = user.getZone();
               Room room = user.getJoinedRoom();
               Integer playerId = user.getPlayerId();

               try {
                    if (user.getSession().isConnected() && !forceLogin) {
                         user.getSession().close();
                    }

                    user.setConnected(false);
               } catch (Exception var10) {
                    var10.printStackTrace();
               } finally {
                    if (zone != null) {
                         zone.removeUser(user);
                    }

                    this.globalUserManager.removeUser(user);
                    user.setConnected(false);
                    if (!forceLogin) {
                         this.removeUserServerLoginAddress(user.getId());
                         this.removeUserServerAddress(user.getId());
                    }

               }

               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.USER, user);
               evtParams.put(BZEventParam.ZONE, zone);
               evtParams.put(BZEventParam.JOINED_ROOMS, room);
               evtParams.put(BZEventParam.PLAYER_ID, playerId);
               IDisconnectionReason disconnectionReason = (IDisconnectionReason)user.getSession().getSystemProperty("disconnectionReason");
               evtParams.put(BZEventParam.DISCONNECTION_REASON, disconnectionReason != null ? disconnectionReason : ClientDisconnectionReason.UNKNOWN);
               if (forceLogin) {
                    this.bz.getEventManager().dispatchImmediateEvent(new BZEvent(BZEventType.USER_DISCONNECT, evtParams));
               } else {
                    this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.USER_DISCONNECT, evtParams));
               }

               if (room != null) {
                    zone.checkAndRemove(room);
               }

               this.log.info("User disconnected: " + user);
          }
     }

     private void removeUserServerLoginAddress(int userId) {
          String key = ConstantMercury.PREFIX_SNSGAME_GENERAL + "server_address_login_cached" + "|" + userId;

          try {
               DataController.getController().deleteCache(key);
          } catch (Exception var4) {
          }

     }

     private void removeUserServerAddress(int userId) {
          String key = ConstantMercury.PREFIX_SNSGAME_GENERAL + "server_address_cached" + "|" + userId;

          try {
               DataController.getController().deleteCache(key);
          } catch (Exception var4) {
          }

     }

     public void removeRoom(Room room) {
          this.removeRoom(room, true, true);
     }

     public void removeRoom(Room room, boolean fireClientEvent, boolean fireServerEvent) {
          room.getZone().removeRoom(room);
          if (fireServerEvent) {
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.ZONE, room.getZone());
               evtParams.put(BZEventParam.ROOM, room);
               this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.ROOM_REMOVED, evtParams));
          }

     }

     public boolean checkSecurePassword(ISession session, String originalPass, String encryptedPass) {
          if (originalPass != null && originalPass.length() >= 1) {
               return encryptedPass != null && encryptedPass.length() >= 1 ? encryptedPass.equals(CryptoUtils.getClientPassword(session, originalPass)) : false;
          } else {
               return false;
          }
     }

     public void login(ISession sender, byte loginOK, User user) {
          if (!this.bz.getSessionManager().containsSession(sender)) {
               this.log.warn("Login failed: " + user.getName() + " , session is already expired!");
          } else {
               IResponse response = new Response();
               response.setId(SystemRequest.Login.getId());
               response.setTargetController(DefaultConstants.CORE_EXTENSIONS_CONTROLLER_ID);
               response.setContent(new byte[]{loginOK});
               response.setRecipients(sender);
               if (loginOK == 0) {
                    BZLoginException ex = null;
                    BZErrorData errorData;
                    if (this.bz.getBannedUserManager().isNameBanned(user.getName())) {
                         errorData = new BZErrorData(BZErrorCode.LOGIN_BANNED_USER);
                         errorData.addParameter(String.valueOf(this.bz.getBannedUserManager().getBanDuration(user.getName(), BanMode.BY_NAME)) + 's');
                         ex = new BZLoginException("This user name is banned: " + user.getName(), errorData);
                    }

                    if (this.bz.getBannedUserManager().isIpBanned(sender.getAddress())) {
                         errorData = new BZErrorData(BZErrorCode.LOGIN_BANNED_IP);
                         errorData.addParameter(sender.getAddress());
                         errorData.addParameter(String.valueOf(this.bz.getBannedUserManager().getBanDuration(sender.getAddress(), BanMode.BY_ADDRESS)));
                         ex = new BZLoginException("This IP address is banned: " + sender.getAddress(), errorData);
                    }

                    if (ex != null) {
                         this.loginErrorHandler.execute(sender, ex);
                         return;
                    }

                    user.setReconnectionSeconds(this.bz.getConfigurator().getServerSettings().userReconnectionSeconds);
                    user.setLastLoginTime(System.currentTimeMillis());
                    user.getSession().setMaxLoggedInIdleTime(this.bz.getConfigurator().getServerSettings().userMaxIdleTime);
                    user.setConnected(true);
                    sender.setLoggedIn(true);
                    this.globalUserManager.addUser(user);
                    this.log.info("Login in, " + user.getName() + ", " + user.toString());
                    user.updateLastRequestTime();
                    Map evtParams = new HashMap();
                    evtParams.put(BZEventParam.USER, user);
                    this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.USER_LOGIN, evtParams));
               } else {
                    this.log.info("Login error: " + loginOK + ". Requested by: " + sender);
                    response.write();
               }

          }
     }

     public void loginWebsocket(ISession sender, byte loginOK, User user) {
          if (!this.bz.getSessionManager().containsSession(sender)) {
               this.log.warn("Login failed: " + user.getName() + " , session is already expired!");
          } else {
               ISFSObject resObj = SFSObject.newInstance();
               IResponse response = new Response();
               response.setId(SystemRequest.LoginWebsocket.getId());
               response.setTargetController(DefaultConstants.CORE_SYSTEM_CONTROLLER_ID);
               response.setContent(resObj);
               response.setRecipients(sender);
               if (loginOK == 0) {
                    BZLoginException ex = null;
                    BZErrorData errorData;
                    if (this.bz.getBannedUserManager().isNameBanned(user.getName())) {
                         errorData = new BZErrorData(BZErrorCode.LOGIN_BANNED_USER);
                         errorData.addParameter(String.valueOf(this.bz.getBannedUserManager().getBanDuration(user.getName(), BanMode.BY_NAME)) + 's');
                         ex = new BZLoginException("This user name is banned: " + user.getName(), errorData);
                    }

                    if (this.bz.getBannedUserManager().isIpBanned(sender.getAddress())) {
                         errorData = new BZErrorData(BZErrorCode.LOGIN_BANNED_IP);
                         errorData.addParameter(sender.getAddress());
                         errorData.addParameter(String.valueOf(this.bz.getBannedUserManager().getBanDuration(sender.getAddress(), BanMode.BY_ADDRESS)));
                         ex = new BZLoginException("This IP address is banned: " + sender.getAddress(), errorData);
                    }

                    if (ex != null) {
                         this.loginErrorHandler.execute(sender, ex);
                         return;
                    }

                    user.setReconnectionSeconds(this.bz.getConfigurator().getServerSettings().userReconnectionSeconds);
                    user.setLastLoginTime(System.currentTimeMillis());
                    user.getSession().setMaxLoggedInIdleTime(this.bz.getConfigurator().getServerSettings().userMaxIdleTime);
                    user.setConnected(true);
                    sender.setLoggedIn(true);
                    this.globalUserManager.addUser(user);
                    this.log.info("Login in, " + user.getName() + ", " + user.toString() + user.getSession().getSystemProperty("ClientType"));
                    user.updateLastRequestTime();
                    Map evtParams = new HashMap();
                    evtParams.put(BZEventParam.USER, user);
                    this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.USER_LOGIN, evtParams));
                    resObj.putInt("id", user.getId());
                    resObj.putUtfString("zn", "");
                    resObj.putUtfString("un", user.getName());
                    resObj.putShort("rs", (short)999);
                    resObj.putShort("pi", user.getPrivilegeId());
                    resObj.putNull("rl");
                    response.write();
               } else {
                    this.log.info("Login error: " + loginOK + ". Requested by: " + sender);
                    response.write();
               }

          }
     }

     public void logout(User user) {
          if (user == null) {
               throw new BZRuntimeException("Cannot logout null user.");
          } else {
               this.disconnectUser(user);
               user.setConnected(false);
               this.globalUserManager.removeUser(user);
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.USER, user);
               this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.USER_LOGOUT, evtParams));
               this.log.info("User logout: " + user);
          }
     }

     public Room createRoom(Zone zone, CreateRoomSettings params, User owner) throws BZCreateRoomException {
          return this.createRoom(zone, params, owner, true, (Room)null, false, true);
     }

     public Room createRoom(Zone zone, CreateRoomSettings params, User owner, boolean joinIt, Room roomToLeave) throws BZCreateRoomException {
          return this.createRoom(zone, params, owner, joinIt, roomToLeave, true, true);
     }

     public Room createRoom(Zone zone, CreateRoomSettings params, User owner, boolean joinIt, Room roomToLeave, boolean fireClientEvent, boolean fireServerEvent) throws BZCreateRoomException {
          Room theRoom = null;

          try {
               String groupId = params.getGroupId();
               if (groupId == null || groupId.length() == 0) {
                    params.setGroupId("default");
               }

               theRoom = zone.createRoom(params, owner);
               if (owner != null) {
                    owner.updateLastRequestTime();
               }

               this.log.info("Room created: " + theRoom);
               if (fireClientEvent) {
                    throw new BZCreateRoomException("aaa");
               }

               if (fireServerEvent) {
                    Map eventParams = new HashMap();
                    eventParams.put(BZEventParam.ZONE, zone);
                    eventParams.put(BZEventParam.ROOM, theRoom);
                    this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.ROOM_ADDED, eventParams));
               }
          } catch (BZCreateRoomException var12) {
               String message = String.format("Room creation error. %s, %s, %s", var12.getMessage(), zone, owner);
               throw new BZCreateRoomException(message);
          }

          if (theRoom != null && owner != null && joinIt) {
               try {
                    this.joinRoom(owner, theRoom, theRoom.getPassword(), false, roomToLeave, true, fireClientEvent);
               } catch (BZJoinRoomException var11) {
                    this.log.warn("Unable to join the just created Room: " + theRoom + ", reason: " + var11.getMessage());
               }
          }

          return theRoom;
     }

     public void joinRoom(User user, Room roomToJoin, int nChair) throws BZJoinRoomException {
          this.joinRoom(user, roomToJoin, "", false, user.getJoinedRoom(), true, true, nChair, false);
     }

     public void joinRoom(User user, Room room) throws BZJoinRoomException {
          this.joinRoom(user, room, "", false, user.getJoinedRoom());
     }

     public void joinRoom(User user, Room roomToJoin, String password, boolean asSpectator, Room roomToLeave) throws BZJoinRoomException {
          this.joinRoom(user, roomToJoin, password, asSpectator, roomToLeave, true, true);
     }

     public void joinRoom(User user, Room roomToJoin, String password, boolean asSpectator, Room roomToLeave, int nChair, boolean isHolding) throws BZJoinRoomException {
          this.joinRoom(user, roomToJoin, password, asSpectator, roomToLeave, true, true, nChair, isHolding);
     }

     public void joinRoom(User user, Room roomToJoin, String password, boolean asSpectator, Room roomToLeave, int nChair) throws BZJoinRoomException {
          this.joinRoom(user, roomToJoin, password, asSpectator, roomToLeave, nChair, false);
     }

     public void joinRoom(User user, Room roomToJoin, String password, boolean asSpectator, Room roomToLeave, boolean fireClientEvent, boolean fireServerEvent) throws BZJoinRoomException {
          try {
               String message;
               try {
                    if (user.isJoining()) {
                         throw new BZRuntimeException("Join request discarded. User is already in a join transaction: " + user);
                    }

                    if (roomToJoin == null) {
                         throw new BZJoinRoomException("Requested room doesn't exist", new BZErrorData(BZErrorCode.JOIN_BAD_ROOM));
                    }

                    if (!roomToJoin.isActive()) {
                          message = String.format("Room is currently locked, %s", roomToJoin);
                         BZErrorData errData = new BZErrorData(BZErrorCode.JOIN_ROOM_LOCKED);
                         errData.addParameter(roomToJoin.getName());
                         throw new BZJoinRoomException(message, errData);
                    }

                    boolean doorIsOpen = true;
                    if (roomToJoin.isPasswordProtected()) {
                         doorIsOpen = roomToJoin.getPassword().equals(password);
                    }

                    if (!doorIsOpen) {
                         message = String.format("Room password is wrong, %s", roomToJoin);
                         BZErrorData data = new BZErrorData(BZErrorCode.JOIN_BAD_PASSWORD);
                         data.addParameter(roomToJoin.getName());
                         throw new BZJoinRoomException(message, data);
                    }

                    user.setJoining(true);
                    roomToJoin.addUser(user, asSpectator);
                    user.updateLastRequestTime();
                    if (fireClientEvent) {
                    }

                    if (fireServerEvent) {
                         Map evtParams = new HashMap();
                         evtParams.put(BZEventParam.ROOM, roomToJoin);
                         evtParams.put(BZEventParam.USER, user);
                         this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.USER_JOIN_ROOM, evtParams));
                    }

                    if (roomToLeave != null) {
                         this.leaveRoom(user, roomToLeave);
                    }
               } catch (BZJoinRoomException var14) {
                    message = String.format("Join Error - %s", var14.getMessage());
                    throw new BZJoinRoomException(message, var14.getErrorData());
               }
          } finally {
               user.setJoining(false);
          }

     }

     public void joinRoom(User user, Room roomToJoin, String password, boolean asSpectator, Room roomToLeave, boolean fireClientEvent, boolean fireServerEvent, int nChair, boolean isHolding) throws BZJoinRoomException {
          try {
               String message;
               try {
                    this.log.debug("XU LI JOIN ROOM ******  ");
                    if (user.isJoining()) {
                         throw new BZRuntimeException("Join request discarded. User is already in a join transaction: " + user);
                    }

                    if (roomToJoin == null) {
                         throw new BZJoinRoomException("Requested room doesn't exist", new BZErrorData(BZErrorCode.JOIN_BAD_ROOM));
                    }

                    if (!roomToJoin.isActive()) {
                          message = String.format("Room is currently locked, %s", roomToJoin);
                         BZErrorData errData = new BZErrorData(BZErrorCode.JOIN_ROOM_LOCKED);
                         errData.addParameter(roomToJoin.getName());
                         throw new BZJoinRoomException(message, errData);
                    }

                    boolean doorIsOpen = true;
                    if (roomToJoin.isPasswordProtected()) {
                         doorIsOpen = roomToJoin.getPassword().equals(password);
                    }

                    if (!doorIsOpen) {
                         message = String.format("Room password is wrong, %s", roomToJoin);
                         BZErrorData data = new BZErrorData(BZErrorCode.JOIN_BAD_PASSWORD);
                         data.addParameter(roomToJoin.getName());
                         throw new BZJoinRoomException(message, data);
                    }

                    user.setJoining(true);
                    roomToJoin.addUser(user, asSpectator, nChair, isHolding);
                    user.updateLastRequestTime();
                    if (fireClientEvent) {
                    }

                    if (fireServerEvent) {
                         this.log.debug("JOIN ROOOM SUCCESS NE ******  ");
                         Map evtParams = new HashMap();
                         evtParams.put(BZEventParam.ROOM, roomToJoin);
                         evtParams.put(BZEventParam.USER, user);
                         this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.USER_JOIN_ROOM, evtParams));
                    }

                    if (roomToLeave != null) {
                         this.leaveRoom(user, roomToLeave);
                    }
               } catch (BZJoinRoomException var16) {
                    message = String.format("Join Error - %s", var16.getMessage());
                    throw new BZJoinRoomException(message, var16.getErrorData());
               }
          } finally {
               user.setJoining(false);
          }

     }

     public void joinRoom(User user, Room roomToJoin, String password, boolean asSpectator, Room roomToLeave, boolean fireClientEvent, boolean fireServerEvent, int nChair) throws BZJoinRoomException {
          this.joinRoom(user, roomToJoin, password, asSpectator, roomToLeave, fireClientEvent, fireServerEvent, nChair, false);
     }

     public void leaveRoom(User user, Room room) {
          this.leaveRoom(user, room, true, true);
     }

     public void leaveRoom(User user, Room room, boolean fireClientEvent, boolean fireServerEvent) {
          if (room == null) {
               room = user.getJoinedRoom();
               if (room == null) {
                    throw new BZRuntimeException("LeaveRoom failed: user is not joined in any room. " + user);
               }
          }

          int playerId = user.getPlayerId();
          if (user.getZone() != null) {
               user.getZone().removeUserFromRoom(user, room);
          }

          user.updateLastRequestTime();
          if (fireServerEvent) {
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.ZONE, user.getZone());
               evtParams.put(BZEventParam.ROOM, room);
               evtParams.put(BZEventParam.USER, user);
               evtParams.put(BZEventParam.PLAYER_ID, playerId);
               this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.USER_LEAVE_ROOM, evtParams));
          } else {
               this.log.info("fireServerEvent fall");
          }

     }

     public void sendPublicMessage(Room targetRoom, User sender, BaseMsg msg) {
          if (targetRoom == null) {
               throw new IllegalArgumentException("The target Room is null");
          } else if (sender.getJoinedRoom().getId() != targetRoom.getId()) {
               throw new IllegalStateException("Sender " + sender + " is not joined the target room " + targetRoom);
          } else if (msg == null) {
               this.log.info("Empty public message request (len == 0) discarded");
          } else {
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.ROOM, targetRoom);
               evtParams.put(BZEventParam.USER, sender);
               evtParams.put(BZEventParam.MESSAGE, msg);
               this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.PUBLIC_MESSAGE, evtParams));
          }
     }

     public void sendPrivateMessage(User sender, User recipient, BaseMsg msg) {
          if (sender == null) {
               throw new IllegalArgumentException("PM sender is null.");
          } else if (recipient == null) {
               throw new IllegalArgumentException("PM recipient is null");
          } else if (sender == recipient) {
               throw new IllegalStateException("PM sender and receiver are the same. Why?");
          } else if (msg == null) {
               this.log.info("Empty private message request (len == 0) discarded");
          } else {
               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.USER, sender);
               evtParams.put(BZEventParam.RECIPIENT, recipient);
               evtParams.put(BZEventParam.MESSAGE, msg);
               this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.PRIVATE_MESSAGE, evtParams));
          }
     }

     public void sendModeratorMessage(User sender, String message, String[] msg, Collection recipients) {
          this.sendSuperUserMessage(GenericMessageType.MODERATOR_MSG, sender, message, msg, recipients);
     }

     public void sendAdminMessage(User sender, String message, String[] msg, Collection recipients) {
          this.sendSuperUserMessage(GenericMessageType.ADMING_MSG, sender, message, msg, recipients);
     }

     private void sendSuperUserMessage(GenericMessageType type, User sender, String message, String[] msg, Collection recipients) {
          if (recipients.size() == 0) {
               throw new IllegalStateException("Mod Message discarded. No recipients");
          } else if (message.length() == 0) {
               throw new IllegalStateException("Mod Message discarded. Empty message");
          } else {
               if (sender == null) {
                    switch(type.getId()) {
                    case 3:
                         sender = UsersUtil.getServerAdmin();
                         break;
                    default:
                         sender = UsersUtil.getServerModerator();
                    }
               }

               this.sendGenericMessage(type, sender, -1, message, msg, recipients);
          }
     }

     private void sendGenericMessage(GenericMessageType type, User sender, int targetRoomId, String message, String[] params, Collection recipients) {
          if (sender != null) {
               sender.updateLastRequestTime();
          }

          GenericMessageMsg msg = new GenericMessageMsg((short)type.getId());
          msg.sender = sender.getName();
          msg.message = message;
          if (params != null) {
               msg.params = params;
          }

          IResponse response = new Response();
          response.setId(SystemRequest.GenericMessage.getId());
          response.setTargetController(DefaultConstants.CORE_SYSTEM_CONTROLLER_ID);
          response.setContent(msg.createData());
          response.setRecipients(recipients);
          response.write();
     }
}
