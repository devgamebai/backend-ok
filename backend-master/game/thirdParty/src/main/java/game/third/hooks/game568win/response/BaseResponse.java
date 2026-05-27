/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 */
package game.third.hooks.game568win.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BaseResponse {
    @JsonProperty(value="AccountName")
    protected String accountName;
    @JsonProperty(value="ErrorCode")
    protected int errorCode;
    @JsonProperty(value="ErrorMessage")
    protected String errorMessage;

    public String getAccountName() {
        return this.accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

