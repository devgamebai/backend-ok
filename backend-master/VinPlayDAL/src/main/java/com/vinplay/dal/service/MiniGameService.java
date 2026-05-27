/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BonusFundResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.response.BonusFundResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public interface MiniGameService {
    public long getPot(String var1) throws SQLException;

    public long[] getPots(String var1) throws SQLException;

    public long getFund(String var1) throws SQLException;

    public long[] getFunds(String var1) throws SQLException;

    public List<BonusFundResponse> getFunds() throws SQLException;

    public void saveFund(String var1, long var2) throws IOException, TimeoutException, InterruptedException;

    public void savePot(String var1, long var2, boolean var4) throws IOException, TimeoutException, InterruptedException;

    public long getReferenceId(int var1) throws SQLException;

    public boolean saveReferenceId(long var1, int var3) throws SQLException;
}

