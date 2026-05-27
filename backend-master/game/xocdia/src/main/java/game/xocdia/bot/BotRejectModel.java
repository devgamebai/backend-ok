/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.xocdia.bot;

import bitzero.server.entities.User;

public class BotRejectModel {
    public User user;
    public byte action;
    public int rejectStartTime;

    public BotRejectModel(User user, byte action, int rejectStartTime) {
        this.user = user;
        this.action = action;
        this.rejectStartTime = rejectStartTime;
    }
}

