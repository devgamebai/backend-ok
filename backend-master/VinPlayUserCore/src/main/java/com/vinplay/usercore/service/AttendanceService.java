/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import com.vinplay.usercore.entities.AttendanceConfig;
import com.vinplay.usercore.entities.UserAttendance;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    public boolean checkIp(String var1, String var2);

    public boolean createConfig(long var1);

    public String createConfig(AttendanceConfig var1) throws SQLException;

    public AttendanceConfig getConfigLastest();

    public AttendanceConfig getConfigLastestFromCached();

    public Map<String, Object> Attendance(String var1, String var2);

    public Map<String, Object> CheckAttendance(String var1, String var2);

    public UserAttendance getAttendLastest(String var1);

    public List<Map<String, Object>> historyInRound(String var1);

    public Map<String, Object> search(Integer var1, String var2, String var3, String var4, int var5, int var6);
}

