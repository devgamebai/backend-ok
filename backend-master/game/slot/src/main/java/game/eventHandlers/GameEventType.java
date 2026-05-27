package game.eventHandlers;

import bitzero.server.core.IBZEventType;

public enum GameEventType implements IBZEventType {
     GAME_ROOM_USER_JOIN("GAME_ROOM_USER_JOIN", 0),
     GAME_ROOM_USER_LEAVE("GAME_ROOM_USER_LEAVE", 1),
     EVENT_ADD_SCORE("EVENT_ADD_SCORE", 2);

     private GameEventType(String s, int n) {
     }
}
