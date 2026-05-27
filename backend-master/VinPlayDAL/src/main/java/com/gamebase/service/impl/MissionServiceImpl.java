/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.utils.UniqueIdGenerator
 */
package com.gamebase.service.impl;

import com.gamebase.dao.EventMissionDao;
import com.gamebase.dao.MissionDao;
import com.gamebase.dao.impl.EventMissionDaoImpl;
import com.gamebase.dao.impl.MissionDaoImpl;
import com.gamebase.entities.EventMission;
import com.gamebase.entities.Mission;
import com.gamebase.service.MissionService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.utils.UniqueIdGenerator;
import java.sql.SQLException;
import java.util.List;

public class MissionServiceImpl
implements MissionService {
    private final MissionDao missionDao = new MissionDaoImpl();
    private final EventMissionDao eventMissionDao = new EventMissionDaoImpl();
    private final IMap<String, Mission> missionCache;

    public MissionServiceImpl() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        this.missionCache = client.getMap("missionMap");
    }

    @Override
    public void createMission(Mission mission) throws SQLException {
        mission.setId(UniqueIdGenerator.generateUUID());
        EventMission event = this.eventMissionDao.getEvent(mission.getEvent_id());
        if (event == null) {
            throw new SQLException("Event not found");
        }
        this.missionDao.createMission(mission);
        System.out.println("Mission added to cache");
        this.missionCache.put(mission.getId(), mission);
    }

    @Override
    public void updateMission(Mission mission) throws SQLException {
        this.missionDao.updateMission(mission);
        mission = this.missionDao.getMission(mission.getId());
        this.missionCache.put(mission.getId(), mission);
    }

    @Override
    public void deleteMission(String id) throws SQLException {
        this.missionDao.deleteMission(id);
        this.missionCache.remove(id);
    }

    @Override
    public Mission getMission(String id) throws SQLException {
        Mission mission = (Mission)this.missionCache.get(id);
        if (mission == null && (mission = this.missionDao.getMission(id)) != null) {
            this.missionCache.put(mission.getId(), mission);
        }
        return mission;
    }

    @Override
    public List<Mission> getListMission() throws SQLException {
        return this.missionDao.getListMission();
    }

    @Override
    public List<Mission> getPartitionMission(String search, int limit, int offset, int event_id, int game_id, int status, String type, String start, String end) throws SQLException {
        return this.missionDao.getPartitionMission(search, limit, offset, event_id, game_id, status, type, start, end);
    }

    @Override
    public int getCountMission(String search, int event_id, int game_id, int status, String type, String start, String end) throws SQLException {
        return this.missionDao.getCountMission(search, event_id, game_id, status, type, start, end);
    }
}

