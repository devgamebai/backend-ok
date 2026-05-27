/*
 * Decompiled with CFR 0.152.
 */
package game.third.processors.response;

import game.third.processors.response.BaseResponse;

public class ListGameResponse
extends BaseResponse {
    private int total;

    public ListGameResponse(boolean success, String errorCode) {
        super(success, errorCode);
    }

    public int getTotal() {
        return this.total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

