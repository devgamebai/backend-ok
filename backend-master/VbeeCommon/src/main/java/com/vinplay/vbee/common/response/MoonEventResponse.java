/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.vbee.common.response;

import java.io.Serializable;

public class MoonEventResponse
implements Serializable {
    public int idEvent;
    public String nameEvent;

    public int getIdEvent() {
        return this.idEvent;
    }

    public void setIdEvent(int idEvent) {
        this.idEvent = idEvent;
    }

    public String getNameEvent() {
        return this.nameEvent;
    }

    public void setNameEvent(String nameEvent) {
        this.nameEvent = nameEvent;
    }
}

