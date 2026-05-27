/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.xocdia.bot;

import bitzero.server.entities.User;

public class BotRequestBankerModel {
    public User user;
    public int reqStartTime;

    public BotRequestBankerModel(User user, int reqStartTime) {
        this.user = user;
        this.reqStartTime = reqStartTime;
    }
}

