package bitzero.server.util;

import bitzero.server.entities.Room;

public interface IPlayerIdGenerator {
     void init();

     int getPlayerSlot();

     void freePlayerSlot(int var1);

     boolean takeSlot(int var1);

     void onRoomResize();

     void setParentRoom(Room var1);

     Room getParentRoom();
}
