/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.EntryAdapter
 *  com.hazelcast.core.EntryEvent
 *  com.hazelcast.core.EntryListener
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  org.apache.log4j.Logger
 */
package com.gamebase.service.impl;

import com.gamebase.dao.LogMoneyUserExtendDao;
import com.gamebase.dao.MissionDao;
import com.gamebase.dao.UserMissionDao;
import com.gamebase.dao.impl.LogMoneyUserExtendDaoImpl;
import com.gamebase.dao.impl.MissionDaoImpl;
import com.gamebase.dao.impl.UserMissionDaoImpl;
import com.gamebase.dao.model.SumUserDepositTimeResult;
import com.gamebase.entities.EventMission;
import com.gamebase.entities.Mission;
import com.gamebase.entities.MissionRule;
import com.gamebase.entities.MissionStatus;
import com.gamebase.entities.UserMission;
import com.gamebase.entities.UserMissionRule;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.UserMissionService;
import com.gamebase.service.impl.EventMissionServiceImpl;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class UserMissionServiceImpl
implements UserMissionService {
    private final UserMissionDao userMissionDao = new UserMissionDaoImpl();
    private final LogMoneyUserExtendDao logMoneyUserExtendDao = new LogMoneyUserExtendDaoImpl();
    private final EventMissionService eventMissionService = new EventMissionServiceImpl();
    private final MissionDao missionDao = new MissionDaoImpl();
    private final IMap<String, List<UserMission>> userMissionCache;
    private final Logger logger = Logger.getLogger((String)"base_game");
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public UserMissionServiceImpl() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        this.userMissionCache = client.getMap("userMissionCache");
    }

    @Override
    public UserMission getMission(String nickname, String missionId) throws Exception {
        UserMission mission;
        List missions = (List)this.userMissionCache.get(nickname);
        if (missions != null) {
            for (Object _m : missions) {
                UserMission mission2 = (UserMission) _m;
                if (!mission2.getMissionId().contains(missionId)) continue;
                return mission2;
            }
        }
        if ((mission = this.userMissionDao.getMission(nickname, missionId)) == null) {
            mission = this.createMissionForUserAndMissionId(nickname, missionId);
        }
        return mission;
    }

    @Override
    public void updateMission(UserMission userMission) throws Exception {
        List missions = (List)this.userMissionCache.get(userMission.getNickname());
        if (missions != null) {
            missions.removeIf(m -> ((UserMission)m).getMissionId().contains(userMission.getMissionId()));
            missions.add(userMission);
            this.pushToCache(userMission.getNickname(), missions);
        }
        this.userMissionDao.updateUserMission(userMission);
    }

    @Override
    public void setStatusMission(String nickName, String missionId, MissionStatus status) throws Exception {
        UserMission mission = this.getMission(nickName, missionId);
        if (mission != null) {
            List missions;
            mission.setStatus(status);
            if (status == MissionStatus.NONE && (missions = (List)this.userMissionCache.get(nickName)) != null) {
                missions.removeIf(m -> ((UserMission)m).getMissionId().contains(missionId));
                this.pushToCache(nickName, missions);
            }
            if (status == MissionStatus.COMPLETE) {
                mission.setProgress(100);
                mission.setWork(mission.getPoint());
            }
            this.updateMission(mission);
        }
    }

    @Override
    public List<UserMission> getAllMission(String nickName) throws Exception {
        List<UserMission> userMissionsHas = this.createMissionForUserBase(nickName);
        List<UserMission> finalUserMissions = (List<UserMission>)this.userMissionCache.get(nickName);
        if (finalUserMissions == null || finalUserMissions.isEmpty()) {
            finalUserMissions = this.userMissionDao.getAllMission(nickName, Arrays.asList(MissionStatus.IN_PROGRESS, MissionStatus.COMPLETE, MissionStatus.REWARD_IN_PROGRESS, MissionStatus.REWARD, MissionStatus.FINISH));
            if (finalUserMissions == null) {
                finalUserMissions = new ArrayList<UserMission>();
            }
            this.pushToCache(nickName, finalUserMissions);
        }
        ArrayList<UserMission> userMissionsResult = new ArrayList<UserMission>();
        for (UserMission userMission : userMissionsHas) {
            boolean isExist = false;
            for (UserMission userMission1 : finalUserMissions) {
                if (!userMission.getMissionId().equals(userMission1.getMissionId())) continue;
                isExist = true;
                userMissionsResult.add(userMission1);
                break;
            }
            if (isExist) continue;
            userMissionsResult.add(userMission);
        }
        return userMissionsResult;
    }

    @Override
    public List<UserMission> getListMissionByEvent(String nickName, int eventId) throws Exception {
        List<UserMission> missions = this.getAllMission(nickName);
        return missions.stream().filter(mission -> mission.getEventId() == eventId).collect(Collectors.toList());
    }

    private List<UserMission> createMissionForUserBase(String nickName) throws Exception {
        List<Mission> missions = this.missionDao.getListMission();
        ArrayList<UserMission> userMissions = new ArrayList<UserMission>();
        for (Mission mission : missions) {
            SumUserDepositTimeResult depositTimeResult;
            EventMission eventMission;
            UserMission userMission = new UserMission();
            userMission.setUId(String.format("%s-%s", mission.getId(), nickName));
            userMission.setNickname(nickName);
            userMission.setEventId(mission.getEvent_id());
            userMission.setMissionId(mission.getId());
            userMission.setStatus(MissionStatus.IN_PROGRESS);
            userMission.setWork(0L);
            userMission.setProgress(0);
            userMission.setPoint(mission.getPoint());
            userMission.setCreated_at(new Date());
            userMission.setUpdated_at(new Date());
            if (!mission.getRules().isEmpty()) {
                for (MissionRule rule : mission.getRules().values()) {
                    UserMissionRule userMissionRule = new UserMissionRule();
                    userMissionRule.setId(rule.getId());
                    userMissionRule.setGame_id(rule.getGame_id());
                    userMissionRule.setProgress(0);
                    userMissionRule.setCompleted(false);
                    userMissionRule.setPoint(rule.getPoint());
                    userMissionRule.setType(rule.getType());
                    userMissionRule.setWork(0L);
                    userMission.getRules().put(rule.getId(), userMissionRule);
                }
            }
            if (mission.getType().contains("first_deposit_complex") && (eventMission = this.eventMissionService.getEventMissionById(mission.getEvent_id())) != null && (depositTimeResult = this.logMoneyUserExtendDao.sumUserDepositTime(nickName, "2020-01-01 00:00:00", this.format.format(eventMission.getCreatedAt()), "")) != null) {
                userMission.setMetaDataDeposit("deposit", depositTimeResult.getSum());
            }
            userMissions.add(userMission);
        }
        return userMissions;
    }

    @Override
    public List<UserMission> createMissionForUser(String nickName) throws Exception {
        List<UserMission> userMissions = this.createMissionForUserBase(nickName);
        ArrayList<String> uIDs = new ArrayList<String>();
        for (UserMission userMission : userMissions) {
            uIDs.add(userMission.getUId());
        }
        List<String> uIDExits = this.userMissionDao.checkListUID(nickName, uIDs);
        userMissions.removeIf(m -> uIDExits.contains(m.getUId()));
        boolean success = this.userMissionDao.createListUserMission(userMissions);
        if (!success) {
            throw new Exception("Failed to create user missions");
        }
        userMissions = this.userMissionDao.getAllMission(nickName, Arrays.asList(MissionStatus.IN_PROGRESS, MissionStatus.COMPLETE, MissionStatus.REWARD, MissionStatus.REWARD_IN_PROGRESS, MissionStatus.FINISH));
        this.pushToCache(nickName, userMissions);
        return userMissions;
    }

    public UserMission createMissionForUserAndMissionId(String nickName, String missionId) throws Exception {
        List<UserMission> userMissions = this.createMissionForUser(nickName);
        for (UserMission userMission : userMissions) {
            if (!userMission.getMissionId().contains(missionId)) continue;
            return userMission;
        }
        return null;
    }

    @Override
    public void listenChangeMission() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap map = client.getMap("missionMap");
        map.addEntryListener((EntryListener)new EntryAdapter<String, Mission>(){

            public void entryAdded(EntryEvent<String, Mission> entryEvent) {
                UserMissionServiceImpl.this.logger.info("Mission added notify");
                UserMissionServiceImpl.this.userMissionCache.clear();
            }

            public void entryRemoved(EntryEvent<String, Mission> entryEvent) {
                UserMissionServiceImpl.this.logger.info("Mission removed notify");
                UserMissionServiceImpl.this.userMissionCache.clear();
            }

            public void entryUpdated(EntryEvent<String, Mission> entryEvent) {
                UserMissionServiceImpl.this.logger.info("Mission updated notify");
                UserMissionServiceImpl.this.userMissionCache.clear();
            }
        }, true);
    }

    private void pushToCache(String nickName, List<UserMission> userMissions) {
        this.userMissionCache.put(nickName, userMissions, 30L, TimeUnit.MINUTES);
    }
}

