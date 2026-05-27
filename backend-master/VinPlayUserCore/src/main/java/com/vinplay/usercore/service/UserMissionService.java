/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.userMission.CompleteMissionObj
 *  com.vinplay.vbee.common.models.userMission.NumberCompleteMissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionResponse
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.models.userMission.CompleteMissionObj;
import com.vinplay.vbee.common.models.userMission.NumberCompleteMissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionResponse;

public interface UserMissionService {
    public UserMissionResponse getUserMission(String var1) throws Exception;

    public CompleteMissionObj completeMission(String var1, String var2, String var3) throws Exception;

    public NumberCompleteMissionObj getNumberCompleteMission(String var1) throws Exception;
}

