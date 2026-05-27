/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.annotations.SerializedName
 */
package game.third.hooks.peachTea.response;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class BaseResponse {
    @SerializedName(value="ErrorCode")
    private int errorCode;
    @SerializedName(value="Result")
    private boolean result;
    @SerializedName(value="Message")
    private String message;
    @SerializedName(value="Data")
    private String data;

    public BaseResponse(boolean result, int ErrorCode) {
        this.result = result;
        this.errorCode = ErrorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setDataJson(Object data) {
        Gson gson = new Gson();
        this.data = gson.toJson(data);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson((Object)this);
    }
}

