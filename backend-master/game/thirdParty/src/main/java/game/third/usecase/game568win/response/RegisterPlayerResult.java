/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.response;

import game.third.usecase.game568win.response.ApiError;

public class RegisterPlayerResult {
    private ApiError error;
    private String serverId;

    public ApiError getError() {
        return this.error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public String getServerId() {
        return this.serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}

