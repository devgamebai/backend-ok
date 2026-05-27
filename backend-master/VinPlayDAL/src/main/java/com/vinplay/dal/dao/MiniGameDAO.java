/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.BonusFundResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.BonusFundResponse;
import java.sql.SQLException;
import java.util.List;

public interface MiniGameDAO {
    public long getReferenceId(int var1) throws SQLException;

    public long getPot(String var1) throws SQLException;

    public long[] getPots(String var1) throws SQLException;

    public long getFund(String var1) throws SQLException;

    public long[] getFunds(String var1) throws SQLException;

    public List<BonusFundResponse> getFunds() throws SQLException;

    public boolean saveReferenceId(long var1, int var3) throws SQLException;
}

