/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  org.apache.log4j.Logger
 */
package com.gamebase.service.impl;

import com.gamebase.dao.EventMissionDao;
import com.gamebase.dao.impl.EventMissionDaoImpl;
import com.gamebase.entities.EventMission;
import com.gamebase.service.EventMissionService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class EventMissionServiceImpl
implements EventMissionService {
    private final EventMissionDao eventMissionDao = new EventMissionDaoImpl();
    private static final Logger logger = Logger.getLogger((String)"base_game");

    @Override
    public List<EventMission> getEventMissionListUser() throws SQLException {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMissionCache = client.getMap("eventMissionCache");
        if (userMissionCache.isEmpty()) {
            this.loadToCache();
        }
        return ((java.util.Collection<EventMission>)userMissionCache.values()).stream().filter(eventMission -> !eventMission.isExpired() && !eventMission.isHidden()).collect(Collectors.toList());
    }

    @Override
    public EventMission getEventMissionById(int id) throws SQLException {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMissionCache = client.getMap("eventMissionCache");
        EventMission eventMission = (EventMission)userMissionCache.get(id);
        if (eventMission == null) {
            eventMission = this.eventMissionDao.getEvent(id);
            userMissionCache.put(id, eventMission);
        }
        return eventMission;
    }

    @Override
    public void createEventMission(EventMission eventMission) throws SQLException {
        this.eventMissionDao.createEvent(eventMission);
        IMap userMissionCache = HazelcastClientFactory.getInstance().getMap("eventMissionCache");
        userMissionCache.clear();
    }

    @Override
    public void updateEventMission(EventMission eventMission) throws SQLException {
        this.eventMissionDao.updateEvent(eventMission);
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMissionCache = client.getMap("eventMissionCache");
        userMissionCache.remove(eventMission.getId());
    }

    @Override
    public void deleteEventMission(int id) throws SQLException {
        this.eventMissionDao.deleteEvent(id);
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMissionCache = client.getMap("eventMissionCache");
        userMissionCache.remove(id);
    }

    @Override
    public void loadToCache() throws SQLException {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap userMissionCache = client.getMap("eventMissionCache");
        List<EventMission> eventMissions = this.eventMissionDao.getListEvent();
        eventMissions.stream().filter(eventMission -> !eventMission.isExpired() && !eventMission.isHidden()).forEach(eventMission -> {
            EventMission cfr_ignored_0 = (EventMission)userMissionCache.put(eventMission.getId(), eventMission, 5L, TimeUnit.MINUTES);
        });
    }

    @Override
    public List<EventMission> getListEventExpired() throws SQLException {
        return this.eventMissionDao.getListEventExpired();
    }
}

