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
import com.vinplay.vbee.common.models.userMission.MissionResponse;
import java.io.Serializable;
import java.util.List;

public class UserMissionResponse
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nN;
    private List<MissionResponse> lMisVin;
    private List<MissionResponse> lMisXu;
    private String error;
    private int depTaiXiuVin;
    private int depTaiXiuXu;

    public UserMissionResponse() {
    }

    public UserMissionResponse(String nN, List<MissionResponse> lMisVin, List<MissionResponse> lMisXu, String error, int depTaiXiuVin, int depTaiXiuXu) {
        this.nN = nN;
        this.lMisVin = lMisVin;
        this.lMisXu = lMisXu;
        this.error = error;
        this.depTaiXiuVin = depTaiXiuVin;
        this.depTaiXiuXu = depTaiXiuXu;
    }

    public int getDepTaiXiuVin() {
        return this.depTaiXiuVin;
    }

    public void setDepTaiXiuVin(int depTaiXiuVin) {
        this.depTaiXiuVin = depTaiXiuVin;
    }

    public int getDepTaiXiuXu() {
        return this.depTaiXiuXu;
    }

    public void setDepTaiXiuXu(int depTaiXiuXu) {
        this.depTaiXiuXu = depTaiXiuXu;
    }

    public String getnN() {
        return this.nN;
    }

    public void setnN(String nN) {
        this.nN = nN;
    }

    public List<MissionResponse> getlMisVin() {
        return this.lMisVin;
    }

    public void setlMisVin(List<MissionResponse> lMisVin) {
        this.lMisVin = lMisVin;
    }

    public List<MissionResponse> getlMisXu() {
        return this.lMisXu;
    }

    public void setlMisXu(List<MissionResponse> lMisXu) {
        this.lMisXu = lMisXu;
    }

    public String getError() {
        return this.error;
    }

    public void setError(String error) {
        this.error = error;
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

