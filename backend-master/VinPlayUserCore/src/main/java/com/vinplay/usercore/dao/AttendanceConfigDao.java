/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.entities.AttendanceConfig;
import java.sql.SQLException;

public interface AttendanceConfigDao {
    public String insert(String var1, long var2) throws SQLException;

    public String insert(AttendanceConfig var1) throws SQLException;

    public AttendanceConfig getLastest() throws SQLException;

    public boolean isCheckSameIP();
}

