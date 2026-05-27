/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.taixiu;

import java.io.Serializable;

public class LogTaiXiuSieuToc
implements Serializable {
    private static final long serialVersionUID = 1L;
    public long id;
    public String openTime;
    public String endTime;
    public int status;
    public String result;
    public long resultAmount = 0L;
    public long twin = 0L;

    public LogTaiXiuSieuToc() {
    }

    public LogTaiXiuSieuToc(long id, String openTime, String endTime, int status, String result, long resultAmount, long twin) {
        this.id = id;
        this.openTime = openTime;
        this.endTime = endTime;
        this.status = status;
        this.result = result;
        this.resultAmount = resultAmount;
        this.twin = twin;
    }
}

