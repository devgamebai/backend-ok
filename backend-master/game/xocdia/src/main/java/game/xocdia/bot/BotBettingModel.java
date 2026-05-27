/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.xocdia.bot;

import bitzero.server.entities.User;

public class BotBettingModel {
    public User user;
    public byte potId;
    public long money;
    public int betStartTime;

    public BotBettingModel(User user, byte potId, long money, int betStartTime) {
        this.user = user;
        this.potId = potId;
        this.money = money;
        this.betStartTime = betStartTime;
    }
}

