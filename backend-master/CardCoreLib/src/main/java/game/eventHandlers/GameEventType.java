/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.IBZEventType
 */
package game.eventHandlers;

import bitzero.server.core.IBZEventType;

public enum GameEventType implements IBZEventType
{
    GAME_ROOM_USER_JOIN,
    GAME_ROOM_USER_LEAVE,
    EVENT_ADD_SCORE,
    THANG_LON,
    OUT_TOUR;
    

    private GameEventType() {
    }
}

