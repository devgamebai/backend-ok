/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.messages.LogMoneyUserMessage
 */
package com.gamebase.mission;

import com.vinplay.vbee.common.messages.LogMoneyUserMessage;

public interface MissionProcess {
    public String getMissionType();

    public String getMissionId();

    public void checkMission(LogMoneyUserMessage var1) throws Exception;

    public boolean rewardMission(String var1) throws Exception;

    public void scanMission(String var1);
}

