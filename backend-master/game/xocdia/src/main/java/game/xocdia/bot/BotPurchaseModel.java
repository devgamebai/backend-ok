/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.xocdia.bot;

import bitzero.server.entities.User;

public class BotPurchaseModel {
    public User user;
    public double moneyBuy;
    public int buyStartTime;

    public BotPurchaseModel() {
    }

    public BotPurchaseModel(User user, double moneyBuy, int buyStartTime) {
        this.user = user;
        this.moneyBuy = moneyBuy;
        this.buyStartTime = buyStartTime;
    }
}

