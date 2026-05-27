/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.service;

import com.vinplay.usercore.entities.VPResponse;
import com.vinplay.usercore.entities.VippointResponse;
import java.util.List;

public interface VippointService {
    public VippointResponse cashoutVP(String var1);

    public byte checkCashoutVP(String var1);

    public VPResponse getVippoint(String var1);

    public List<String> subVippointEvent();

    public List<String> addVippointEvent();

    public int updateVippointEvent(String var1, int var2, String var3);

    public boolean updateVippointAgent(String var1, String var2, long var3, long var5, int var7);

    public boolean resetEvent();
}

