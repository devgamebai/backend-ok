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
import com.vinplay.vbee.common.models.userMission.MissionObj;
import java.io.Serializable;
import java.util.List;

public class UserMissionObj
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nickName;
    private List<MissionObj> listMissionVin;
    private List<MissionObj> listMissionXu;

    public UserMissionObj() {
    }

    public UserMissionObj(String nickName, List<MissionObj> listMissionVin, List<MissionObj> listMissionXu) {
        this.nickName = nickName;
        this.listMissionVin = listMissionVin;
        this.listMissionXu = listMissionXu;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public List<MissionObj> getListMissionVin() {
        return this.listMissionVin;
    }

    public void setListMissionVin(List<MissionObj> listMissionVin) {
        this.listMissionVin = listMissionVin;
    }

    public List<MissionObj> getListMissionXu() {
        return this.listMissionXu;
    }

    public void setListMissionXu(List<MissionObj> listMissionXu) {
        this.listMissionXu = listMissionXu;
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

