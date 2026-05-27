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

public class CompleteMissionObj
implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean updateSuccess;
    private long moneyBonus;
    private long moneyUser;
    private String error;

    public CompleteMissionObj(boolean updateSuccess, long moneyBonus, long moneyUser, String error) {
        this.updateSuccess = updateSuccess;
        this.moneyBonus = moneyBonus;
        this.moneyUser = moneyUser;
        this.error = error;
    }

    public CompleteMissionObj() {
    }

    public String getError() {
        return this.error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isUpdateSuccess() {
        return this.updateSuccess;
    }

    public void setUpdateSuccess(boolean updateSuccess) {
        this.updateSuccess = updateSuccess;
    }

    public long getMoneyBonus() {
        return this.moneyBonus;
    }

    public void setMoneyBonus(long moneyBonus) {
        this.moneyBonus = moneyBonus;
    }

    public long getMoneyUser() {
        return this.moneyUser;
    }

    public void setMoneyUser(long moneyUser) {
        this.moneyUser = moneyUser;
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

