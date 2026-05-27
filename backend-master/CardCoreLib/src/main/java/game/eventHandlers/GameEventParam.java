/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.IBZEventParam
 */
package game.eventHandlers;

import bitzero.server.core.IBZEventParam;

public enum GameEventParam implements IBZEventParam
{
    USER,
    GAMEROOM,
    IS_RECONNECT,
    USER_SCORE,
    THONG_TIN_THANG_LON;
    

    private GameEventParam() {
    }
}

