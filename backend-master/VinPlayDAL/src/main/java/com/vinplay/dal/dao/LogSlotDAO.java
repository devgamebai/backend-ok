/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.SlotResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.SlotResponse;
import java.sql.SQLException;
import java.util.List;

public interface LogSlotDAO {
    public List<SlotResponse> listLogSlot(String var1, String var2, String var3, String var4, String var5, int var6, String var7) throws SQLException;

    public long countLogKhoBau(String var1, String var2, String var3, String var4, String var5) throws SQLException;
}

