/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.vbee.common.models.userMission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;

public class NumberCompleteMissionObj
implements Serializable {
    private static final long serialVersionUID = 1L;
    private int numCompVin;
    private int numCompXu;

    public NumberCompleteMissionObj() {
    }

    public NumberCompleteMissionObj(int numCompVin, int numCompXu) {
        this.numCompVin = numCompVin;
        this.numCompXu = numCompXu;
    }

    public int getNumCompVin() {
        return this.numCompVin;
    }

    public void setNumCompVin(int numCompVin) {
        this.numCompVin = numCompVin;
    }

    public int getNumCompXu() {
        return this.numCompXu;
    }

    public void setNumCompXu(int numCompXu) {
        this.numCompXu = numCompXu;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString((Object)this);
        }
        catch (JsonProcessingException mapper) {
            return "{\"success\":false,\"errorCode\":\"1001\"}";
        }
    }
}

