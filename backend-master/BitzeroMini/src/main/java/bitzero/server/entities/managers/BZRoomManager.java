package bitzero.server.entities.managers;

import bitzero.server.BitZeroServer;
import bitzero.server.api.CreateRoomSettings;
import bitzero.server.core.BaseCoreService;
import bitzero.server.entities.Room;
import bitzero.server.entities.User;
import bitzero.server.entities.Zone;
import bitzero.server.exceptions.BZCreateRoomException;
import bitzero.server.exceptions.BZErrorCode;
import bitzero.server.exceptions.BZErrorData;
import bitzero.server.exceptions.BZRoomException;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.util.DefaultPlayerIdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BZRoomManager extends BaseCoreService implements IRoomManager {
     private final Map roomsById = new ConcurrentHashMap();
     private final Map roomsByName = new ConcurrentHashMap();
     private final Map roomsByGroup = new ConcurrentHashMap();
     private final List groups = new ArrayList();
     private Logger logger = LoggerFactory.getLogger(this.getClass());
     private BitZeroServer bz = BitZeroServer.getInstance();
     private Zone ownerZone;
     private Class playerIdGeneratorClass = DefaultPlayerIdGenerator.class;
     private static int[] $SWITCH_TABLE$com$BitZeroServer$v2$entities$BZRoomRemoveMode;

     public Room createRoom(CreateRoomSettings params) throws BZCreateRoomException {
          return this.createRoom(params, (User)null);
     }

     public Room createRoom(CreateRoomSettings params, User owner) throws BZCreateRoomException {
          String roomName = params.getName();

          try {
               this.validateRoomName(roomName);
          } catch (BZRoomException var6) {
               throw new BZCreateRoomException(var6.getMessage(), var6.getErrorData());
          }

          Room newRoom = new Room(roomName);
          newRoom.setZone(this.ownerZone);
          newRoom.setGroupId(params.getGroupId());
          newRoom.setPassword(params.getPassword());
          newRoom.setDynamic(params.isDynamic());
          newRoom.setHidden(params.isHidden());
          newRoom.setMaxUsers(params.getMaxUsers());
          if (params.isGame()) {
               newRoom.setMaxSpectators(params.getMaxSpectators());
          } else {
               newRoom.setMaxSpectators(0);
          }

          newRoom.setGame(params.isGame(), params.getCustomPlayerIdGeneratorClass() != null ? params.getCustomPlayerIdGeneratorClass() : this.playerIdGeneratorClass);
          newRoom.setOwner(owner);
          newRoom.setAutoRemoveMode(params.getAutoRemoveMode());
          if (this.roomsById.size() >= this.ownerZone.getMaxAllowedRooms()) {
               BZErrorData errorData = new BZErrorData(BZErrorCode.CREATE_ROOM_ZONE_FULL);
               throw new BZCreateRoomException("Zone is full. Can't add any more rooms.", errorData);
          } else {
               this.addRoom(newRoom);
               newRoom.setActive(true);
               return newRoom;
          }
     }

     public Class getDefaultRoomPlayerIdGenerator() {
          return this.playerIdGeneratorClass;
     }

     public void setDefaultRoomPlayerIdGeneratorClass(Class customIdGeneratorClass) {
          this.playerIdGeneratorClass = customIdGeneratorClass;
     }

     public void addGroup(String groupId) {
          synchronized(this.groups) {
               this.groups.add(groupId);
          }
     }

     public void addRoom(Room room) {
          this.roomsById.put(room.getId(), room);
          this.roomsByName.put(room.getName(), room);
          synchronized(this.groups) {
               if (!this.groups.contains(room.getGroupId())) {
                    this.groups.add(room.getGroupId());
               }
          }

          this.addRoomToGroup(room);
     }

     public boolean containsGroup(String groupId) {
          boolean flag = false;
          synchronized(this.groups) {
               flag = this.groups.contains(groupId);
               return flag;
          }
     }

     public List getGroups() {
          List groupsCopy = null;
          synchronized(this.groups) {
               groupsCopy = new ArrayList(this.groups);
               return groupsCopy;
          }
     }

     public Room getRoomById(int id) {
          return (Room)this.roomsById.get(id);
     }

     public Room getRoomByName(String name) {
          return (Room)this.roomsByName.get(name);
     }

     public List getRoomList() {
          return new ArrayList(this.roomsById.values());
     }

     public List getRoomListFromGroup(String groupId) {
          List roomList = (List)this.roomsByGroup.get(groupId);
          List copyOfRoomList = null;
          if (roomList != null) {
               synchronized(roomList) {
                    copyOfRoomList = new ArrayList(roomList);
               }
          } else {
               copyOfRoomList = new ArrayList();
          }

          return copyOfRoomList;
     }

     public void removeGroup(String groupId) {
          synchronized(this.groups) {
               this.groups.remove(groupId);
          }
     }

     public void removeRoom(int roomId) {
          Room room = (Room)this.roomsById.get(roomId);
          if (room == null) {
               this.logger.warn("Can't remove requested room. ID = " + roomId + ". Room was not found.");
          } else {
               this.removeRoom(room);
          }

     }

     public void removeRoom(String name) {
          Room room = (Room)this.roomsByName.get(name);
          if (room == null) {
               this.logger.warn("Can't remove requested room. Name = " + name + ". Room was not found.");
          } else {
               this.removeRoom(room);
          }

     }

     public void removeRoom(Room room) {
          room.setActive(false);
          this.roomsById.remove(room.getId());
          this.roomsByName.remove(room.getName());
          this.removeRoomFromGroup(room);
          this.logger.info("Removed: " + room);
     }

     public boolean containsRoom(int id, String groupId) {
          Room room = (Room)this.roomsById.get(id);
          return this.isRoomContainedInGroup(room, groupId);
     }

     public boolean containsRoom(int id) {
          return this.roomsById.containsKey(id);
     }

     public boolean containsRoom(Room room, String groupId) {
          return this.isRoomContainedInGroup(room, groupId);
     }

     public boolean containsRoom(Room room) {
          return this.roomsById.containsValue(room);
     }

     public boolean containsRoom(String name, String groupId) {
          Room room = (Room)this.roomsByName.get(name);
          return this.isRoomContainedInGroup(room, groupId);
     }

     public boolean containsRoom(String name) {
          return this.roomsByName.containsKey(name);
     }

     public boolean containsRoomId(int id) {
          return this.roomsById.containsKey(id);
     }

     public Zone getOwnerZone() {
          return this.ownerZone;
     }

     public void setOwnerZone(Zone zone) {
          this.ownerZone = zone;
     }

     public void removeUser(User user) {
          if (user.getJoinedRoom() != null) {
               user.getJoinedRoom().removeUser(user);
          }

     }

     public void removeUser(User user, Room room) {
          if (room.containsUser(user)) {
               room.removeUser(user);
               this.logger.debug("User: " + user.getName() + " removed from Room: " + room.getName());
               this.handleAutoRemove(room);
          } else {
               throw new BZRuntimeException("Can't remove user: " + user + ", from: " + room);
          }
     }

     public void checkAndRemove(Room room) {
          this.handleAutoRemove(room);
     }

     public void changeRoomName(Room room, String newName) throws BZRoomException {
          if (room == null) {
               throw new IllegalArgumentException("Can't change name. Room is Null!");
          } else if (!this.containsRoom(room)) {
               throw new IllegalArgumentException(room + " is not managed by this Zone: " + this.ownerZone);
          } else {
               this.validateRoomName(newName);
               String oldName = room.getName();
               room.setName(newName);
               this.roomsByName.put(newName, room);
               this.roomsByName.remove(oldName);
          }
     }

     public void changeRoomPasswordState(Room room, String password) {
          if (room == null) {
               throw new IllegalArgumentException("Can't change password. Room is Null!");
          } else if (!this.containsRoom(room)) {
               throw new IllegalArgumentException(room + " is not managed by this Zone: " + this.ownerZone);
          } else {
               room.setPassword(password);
          }
     }

     public void changeRoomCapacity(Room room, int newMaxUsers, int newMaxSpect) {
          if (room == null) {
               throw new IllegalArgumentException("Can't change password. Room is Null!");
          } else if (!this.containsRoom(room)) {
               throw new IllegalArgumentException(room + " is not managed by this Zone: " + this.ownerZone);
          } else {
               if (newMaxUsers > 0) {
                    room.setMaxUsers(newMaxUsers);
               }

               if (newMaxSpect >= 0) {
                    room.setMaxSpectators(newMaxSpect);
               }

          }
     }

     private void handleAutoRemove(Room room) {
          if (room.isEmpty() && room.isDynamic()) {
               switch(room.getAutoRemoveMode().ordinal() + 1) {
               case 1:
                    if (room.isGame()) {
                         this.removeWhenEmpty(room);
                    } else {
                         this.removeWhenEmptyAndCreatorIsGone(room);
                    }
                    break;
               case 2:
                    this.removeWhenEmpty(room);
                    break;
               case 3:
                    this.removeWhenEmptyAndCreatorIsGone(room);
               case 4:
               }
          }

     }

     private void removeWhenEmpty(Room room) {
          if (room.isEmpty()) {
               this.bz.getAPIManager().getBzApi().removeRoom(room);
          }

     }

     private void removeWhenEmptyAndCreatorIsGone(Room room) {
          User owner = room.getOwner();
          if (owner != null && !owner.isConnected()) {
               this.bz.getAPIManager().getBzApi().removeRoom(room);
          }

     }

     private boolean isRoomContainedInGroup(Room room, String groupId) {
          boolean flag = false;
          if (room != null && room.getGroupId().equals(groupId) && this.containsGroup(groupId)) {
               flag = true;
          }

          return flag;
     }

     private void addRoomToGroup(Room room) {
          String groupId = room.getGroupId();
          List roomList = (List)this.roomsByGroup.get(groupId);
          if (roomList == null) {
               roomList = new ArrayList();
               this.roomsByGroup.put(groupId, roomList);
          }

          synchronized(roomList) {
               ((List)roomList).add(room);
          }
     }

     private void removeRoomFromGroup(Room room) {
          List roomList = (List)this.roomsByGroup.get(room.getGroupId());
          if (roomList != null) {
               synchronized(roomList) {
                    roomList.remove(room);
               }
          } else {
               this.logger.info("Cannot remove room: " + room.getName() + " from it's group: " + room.getGroupId() + ". The group was not found.");
          }

     }

     private void validateRoomName(String roomName) throws BZRoomException {
          if (roomName.length() <= 0 || roomName.length() >= 100) {
               ;
          }
     }

     private void populateTransientFields() {
          this.bz = BitZeroServer.getInstance();
          this.logger = LoggerFactory.getLogger(this.getClass());
     }
}
