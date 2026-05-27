/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.EntryAdapter
 *  com.hazelcast.core.EntryEvent
 *  com.hazelcast.core.EntryListener
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.log4j.Logger
 */
package com.gamebase.mission;

import com.gamebase.dao.impl.MissionDaoImpl;
import com.gamebase.entities.Mission;
import com.gamebase.mission.MissionBet;
import com.gamebase.mission.MissionComplex;
import com.gamebase.mission.MissionDeposit;
import com.gamebase.mission.MissionFirstDeposit;
import com.gamebase.mission.MissionFirstDepositComplex;
import com.gamebase.mission.MissionProcess;
import com.gamebase.mission.MissionWin;
import com.gamebase.mission.exception.MissionExpiredException;
import com.gamebase.service.EventMissionService;
import com.gamebase.service.UserMissionService;
import com.gamebase.service.impl.EventMissionServiceImpl;
import com.gamebase.service.impl.UserMissionServiceImpl;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

public class ManagerMission {
    private static final Logger logger = Logger.getLogger((String)"base_game");
    private final List<MissionProcess> missionProcesses;
    private final UserMissionService userMissionService = new UserMissionServiceImpl();
    private final EventMissionService eventMissionService = new EventMissionServiceImpl();
    private static ManagerMission _instance = null;

    public ManagerMission() {
        this.missionProcesses = new ArrayList<MissionProcess>();
    }

    public static ManagerMission Instance() {
        if (_instance == null) {
            _instance = new ManagerMission();
        }
        return _instance;
    }

    public void updateMissionFromDatabase() throws SQLException {
        MissionDaoImpl missionDao = new MissionDaoImpl();
        List<Mission> missionsFromDatabase = missionDao.getListMission();
        for (Mission mission : missionsFromDatabase) {
            MissionProcess missionProcess = this.findMissionProcessById(mission);
            if (missionProcess != null) continue;
            missionProcess = this.createMissionProcess(mission);
            this.missionProcesses.add(missionProcess);
        }
    }

    private MissionProcess createMissionProcess(Mission mission) {
        switch (mission.getType()) {
            case "bet": {
                return new MissionBet(this.eventMissionService, mission, this.userMissionService);
            }
            case "win": {
                return new MissionWin(this.eventMissionService, mission, this.userMissionService);
            }
            case "deposit": {
                return new MissionDeposit(this.eventMissionService, mission, this.userMissionService);
            }
            case "first_deposit": {
                return new MissionFirstDeposit(this.eventMissionService, mission, this.userMissionService);
            }
            case "complex": {
                return new MissionComplex(this.eventMissionService, mission, this.userMissionService);
            }
            case "first_deposit_complex": {
                return new MissionFirstDepositComplex(this.eventMissionService, mission, this.userMissionService);
            }
        }
        return null;
    }

    public MissionProcess findMissionProcessById(Mission mission) {
        for (MissionProcess missionProcess : this.missionProcesses) {
            String id = String.format("%s_%s", missionProcess.getMissionType(), mission.getId());
            if (!missionProcess.getMissionId().contains(id)) continue;
            return missionProcess;
        }
        return null;
    }

    public void addMissionProcess(MissionProcess missionProcess) {
        this.missionProcesses.add(missionProcess);
    }

    public void removeMissionProcess(MissionProcess missionProcess) {
        this.missionProcesses.remove(missionProcess);
    }

    public void processLogMoneyUserMessage(LogMoneyUserMessage logMoneyUserMessage) throws Exception {
        for (MissionProcess missionProcess : this.missionProcesses) {
            try {
                missionProcess.checkMission(logMoneyUserMessage);
            }
            catch (MissionExpiredException e) {
                this.removeMissionProcess(missionProcess);
            }
        }
    }

    public List<String> getAllMissionTypes() {
        HashSet<String> missionIds = new HashSet<String>();
        for (MissionProcess missionProcess : this.missionProcesses) {
            missionIds.add(missionProcess.getMissionType());
        }
        return new ArrayList<String>(missionIds);
    }

    public void listenChangeMission() {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        final IMap map = client.getMap("missionMap");
        map.addEntryListener((EntryListener)new EntryAdapter<String, Mission>(){

            public void entryAdded(EntryEvent<String, Mission> entryEvent) {
                Mission mission = (Mission)entryEvent.getValue();
                if (mission == null) {
                    map.delete(entryEvent.getKey());
                    return;
                }
                MissionProcess missionProcess = ManagerMission.this.findMissionProcessById(mission);
                if (missionProcess == null) {
                    missionProcess = ManagerMission.this.createMissionProcess(mission);
                    ManagerMission.this.missionProcesses.add(missionProcess);
                }
            }

            public void entryRemoved(EntryEvent<String, Mission> entryEvent) {
                Mission mission = (Mission)entryEvent.getValue();
                if (mission == null) {
                    map.delete(entryEvent.getKey());
                    return;
                }
                MissionProcess missionProcess = ManagerMission.this.findMissionProcessById(mission);
                if (missionProcess != null) {
                    ManagerMission.this.removeMissionProcess(missionProcess);
                }
            }

            public void entryUpdated(EntryEvent<String, Mission> entryEvent) {
                Mission mission = (Mission)entryEvent.getValue();
                if (mission == null) {
                    map.delete(entryEvent.getKey());
                    return;
                }
                MissionProcess missionProcess = ManagerMission.this.findMissionProcessById(mission);
                if (missionProcess != null) {
                    ManagerMission.this.removeMissionProcess(missionProcess);
                }
                missionProcess = ManagerMission.this.createMissionProcess(mission);
                ManagerMission.this.missionProcesses.add(missionProcess);
            }
        }, true);
    }

    public Set<String> hideMission() {
        HashSet<String> missionIds = new HashSet<String>();
        IMap mapMission = HazelcastClientFactory.getInstance().getMap("missionMap");
        Collection missions = mapMission.values();
        for (Object _obj : missions) {
            Mission mission = (Mission) _obj;
            if (mission == null || !mission.getType().equals("first_deposit")) continue;
            missionIds.add(mission.getId());
        }
        return missionIds;
    }
}

