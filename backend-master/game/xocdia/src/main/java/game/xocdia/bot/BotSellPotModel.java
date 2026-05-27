/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.xocdia.bot;

import bitzero.server.entities.User;

public class BotSellPotModel {
    public User user;
    public boolean isSellPot;
    public double moneySell;
    public int sellStartTime;

    public BotSellPotModel() {
    }

    public BotSellPotModel(User user, boolean isSellPot, double moneySell, int sellStartTime) {
        this.user = user;
        this.isSellPot = isSellPot;
        this.moneySell = moneySell;
        this.sellStartTime = sellStartTime;
    }
}

