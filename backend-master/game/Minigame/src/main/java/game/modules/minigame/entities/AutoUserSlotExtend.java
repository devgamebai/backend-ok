/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.modules.minigame.entities;

import bitzero.server.entities.User;

public class AutoUserSlotExtend {
    private User user;
    private int count;
    private long bet;
    private int maxCount = 6;

    public AutoUserSlotExtend(User user, long bet) {
        this.user = user;
        this.count = 0;
        this.bet = bet;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return this.user;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return this.count;
    }

    public long getBet() {
        return this.bet;
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    public boolean incCount() {
        ++this.count;
        if (this.count == this.maxCount) {
            this.count = 0;
            return true;
        }
        return false;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }
}

