/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.PokegoResponse
 */
package com.vinplay.dal.dao;

import com.vinplay.vbee.common.response.PokegoResponse;
import java.sql.SQLException;
import java.util.List;

public interface LogPoKeGoDAO {
    public List<PokegoResponse> listLogPokego(String var1, String var2, String var3, String var4, String var5, String var6, int var7) throws SQLException;

    public long countLogPokego(String var1, String var2, String var3, String var4, String var5, String var6) throws SQLException;
}

