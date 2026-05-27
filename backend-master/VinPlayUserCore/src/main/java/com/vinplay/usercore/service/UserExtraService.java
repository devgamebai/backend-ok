/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.cache.UserExtraInfoModel
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.models.cache.UserExtraInfoModel;

public interface UserExtraService {
    public UserExtraInfoModel getModelFromToken(String var1);

    public String getPlatformFromToken(String var1);
}

