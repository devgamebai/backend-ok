/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.VQMMResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.VQMMResponse;
import java.util.List;

public interface LogVQMMDAO {
    public List<VQMMResponse> searchVQMM(String var1, String var2, String var3, int var4);

    public int countSearchVQMM(String var1, String var2, String var3);
}

