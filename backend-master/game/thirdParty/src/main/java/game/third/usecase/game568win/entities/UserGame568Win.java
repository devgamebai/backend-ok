/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.entities;

import java.io.Serializable;

public class UserGame568Win
implements Serializable {
    private String serverId;
    private String username;
    private String agent;
    private String userGroup;
    private String displayName;

    public String getServerId() {
        return this.serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAgent() {
        return this.agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getUserGroup() {
        return this.userGroup;
    }

    public void setUserGroup(String userGroup) {
        this.userGroup = userGroup;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

