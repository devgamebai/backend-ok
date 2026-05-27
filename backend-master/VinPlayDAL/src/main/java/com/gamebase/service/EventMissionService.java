/*
 * Decompiled with CFR 0.152.
 */
package com.gamebase.service;

import com.gamebase.entities.EventMission;
import java.sql.SQLException;
import java.util.List;

public interface EventMissionService {
    public List<EventMission> getEventMissionListUser() throws SQLException;

    public EventMission getEventMissionById(int var1) throws SQLException;

    public void createEventMission(EventMission var1) throws SQLException;

    public void updateEventMission(EventMission var1) throws SQLException;

    public void deleteEventMission(int var1) throws SQLException;

    public void loadToCache() throws SQLException;

    public List<EventMission> getListEventExpired() throws SQLException;
}

