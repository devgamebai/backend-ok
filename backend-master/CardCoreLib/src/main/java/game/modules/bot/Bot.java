/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.modules.bot;

import bitzero.server.entities.User;

public class Bot {
    public User user = null;
    public int count = 0;
    public int lastRoomId = -1;
    public boolean isFree = true;

    public Bot(User user) {
        this.user = user;
    }
}

