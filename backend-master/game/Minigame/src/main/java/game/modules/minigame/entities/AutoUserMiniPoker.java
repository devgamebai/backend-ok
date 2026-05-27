/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.modules.minigame.entities;

import bitzero.server.entities.User;

public class AutoUserMiniPoker {
    private User user;
    private int count;

    public AutoUserMiniPoker(User user, int count) {
        this.user = user;
        this.count = count;
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

    public boolean incCount() {
        ++this.count;
        if (this.count == 6) {
            this.count = 0;
            return true;
        }
        return false;
    }
}

