/*
 * Decompiled with CFR 0.152.
 */
package com.payment.model;

import com.payment.model.Code;
import com.payment.provider.Provider;

public class Result<T> {
    private Code code;
    private T data;
    private String dataRaw;

    public Code getCode() {
        return this.code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public Result(Code code) {
        this.code = code;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<T>(Code.SUCCESS);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<T>(Code.ERROR);
        result.setDataRaw(msg);
        return result;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static <T> Result<T> success(Provider provider, String data) {
        Result<T> result = new Result<T>(Code.SUCCESS);
        result.setDataRaw(provider.resultSuccess(Code.SUCCESS, data));
        return result;
    }

    public static Result<String> error(Provider provider, String msg) {
        Result<String> result = new Result<String>(Code.ERROR);
        result.setData(provider.resultError(Code.ERROR, msg));
        return result;
    }

    public String getDataRaw() {
        return this.dataRaw;
    }

    public void setDataRaw(String dataRaw) {
        this.dataRaw = dataRaw;
    }
}

