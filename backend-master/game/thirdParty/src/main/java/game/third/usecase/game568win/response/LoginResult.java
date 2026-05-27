/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.response;

import game.third.usecase.game568win.response.ApiError;

public class LoginResult {
    private String url;
    private String serverId;
    private ApiError error;

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServerId() {
        return this.serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public ApiError getError() {
        return this.error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }
}

