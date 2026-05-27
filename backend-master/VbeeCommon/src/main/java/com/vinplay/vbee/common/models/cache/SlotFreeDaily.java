/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class SlotFreeDaily
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int rotateFree;
    private long maxWin;

    public SlotFreeDaily(int rotateFree, long maxWin) {
        this.rotateFree = rotateFree;
        this.maxWin = maxWin;
    }

    public int getRotateFree() {
        return this.rotateFree;
    }

    public void setRotateFree(int rotateFree) {
        this.rotateFree = rotateFree;
    }

    public long getMaxWin() {
        return this.maxWin;
    }

    public void setMaxWin(long maxWin) {
        this.maxWin = maxWin;
    }
}

