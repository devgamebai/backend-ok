/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vtc;

public class ResponseCode {
    int status;
    String message;

    public ResponseCode() {
    }

    public ResponseCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toString() {
        return "ResponseCode{status=" + this.status + ", message='" + this.message + '\'' + '}';
    }
}

