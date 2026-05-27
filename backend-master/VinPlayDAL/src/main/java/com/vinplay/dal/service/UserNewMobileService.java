/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.UserNewMobileResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.UserNewMobileResponse;
import java.util.List;

public interface UserNewMobileService {
    public List<UserNewMobileResponse> searchUserNewMobile(String var1, String var2, String var3, int var4);

    public long countSearchUserNewMobile(String var1, String var2, String var3);
}

