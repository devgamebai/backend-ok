/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.models.cache;

import java.io.Serializable;

public class RutLocCacheModel
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int soLuotRut;

    public RutLocCacheModel(int soLuotRut) {
        this.soLuotRut = soLuotRut;
    }

    public int getSoLuotRut() {
        return this.soLuotRut;
    }

    public void setSoLuotRut(int soLuotRut) {
        this.soLuotRut = soLuotRut;
    }

    public int addSoLuotRut(int soLuotThem) {
        this.soLuotRut += soLuotThem;
        return this.soLuotRut;
    }
}

