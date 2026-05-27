/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.dao;

import com.gamebase.entities.EventMission;
import java.sql.SQLException;
import java.util.List;

public interface EventMissionDao {
    public void createEvent(EventMission var1) throws SQLException;

    public void updateEvent(EventMission var1) throws SQLException;

    public void deleteEvent(int var1) throws SQLException;

    public EventMission getEvent(int var1) throws SQLException;

    public List<EventMission> getListEvent() throws SQLException;

    public List<EventMission> getListEventExpired() throws SQLException;

    public List<EventMission> getPartitionEvent(String var1, int var2, int var3, int var4, String var5, String var6) throws SQLException;

    public int getCountEvent(String var1, int var2, String var3, String var4) throws SQLException;
}

