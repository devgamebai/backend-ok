/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.dao;

import com.vinplay.usercore.entities.UserAttendance;
import java.sql.SQLException;
import java.util.Map;

public interface UserAttendanceDao {
    public String insert(UserAttendance var1) throws SQLException;

    public String delete(int var1) throws SQLException;

    public UserAttendance getLastest(String var1) throws SQLException;

    public UserAttendance getDetail(String var1, int var2, String var3) throws SQLException;

    public Map<String, Object> search(Integer var1, String var2, String var3, String var4, int var5, int var6) throws SQLException;

    public Map<String, Object> search4BO(Integer var1, String var2, String var3, String var4, int var5, int var6) throws SQLException;
}

