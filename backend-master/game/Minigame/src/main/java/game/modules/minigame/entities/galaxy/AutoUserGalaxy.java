/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 */
package game.modules.minigame.entities.galaxy;

import bitzero.server.entities.User;

public class AutoUserGalaxy {
    private User user;
    private int count;
    private String lines;
    private int maxCount = 6;

    public AutoUserGalaxy(User user, String lines) {
        this.user = user;
        this.count = 0;
        this.lines = lines;
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

    public void setLines(String lines) {
        this.lines = lines;
    }

    public String getLines() {
        return this.lines;
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

