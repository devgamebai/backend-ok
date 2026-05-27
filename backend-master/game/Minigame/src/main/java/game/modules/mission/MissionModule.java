/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.common.business.Debug
 *  com.vinplay.usercore.service.UserMissionService
 *  com.vinplay.usercore.service.impl.UserMissionServiceImpl
 *  com.vinplay.vbee.common.models.userMission.CompleteMissionObj
 *  com.vinplay.vbee.common.models.userMission.UserMissionResponse
 */
package game.modules.mission;

import bitzero.server.entities.User;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import com.vinplay.usercore.service.UserMissionService;
import com.vinplay.usercore.service.impl.UserMissionServiceImpl;
import com.vinplay.vbee.common.models.userMission.CompleteMissionObj;
import com.vinplay.vbee.common.models.userMission.UserMissionResponse;
import game.modules.mission.cmd.rev.RewardMissionCmd;
import game.modules.mission.cmd.send.ListMissionMsg;
import game.modules.mission.cmd.send.RewardMissionMsg;

public class MissionModule
extends BaseClientRequestHandler {
    private UserMissionService missionService = new UserMissionServiceImpl();

    public void init() {
        super.init();
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 21000: {
                this.getListMission(user);
                break;
            }
            case 21001: {
                this.rewardMission(user, dataCmd);
            }
        }
    }

    private void getListMission(User user) {
        ListMissionMsg msg = new ListMissionMsg();
        try {
            UserMissionResponse obj = this.missionService.getUserMission(user.getName());
            msg.listMission = obj.toJson();
        }
        catch (Exception e) {
            e.printStackTrace();
            Debug.trace(e);
        }
        this.send((BaseMsg)msg, user);
    }

    private synchronized void rewardMission(User user, DataCmd dataCmd) {
        RewardMissionCmd cmd = new RewardMissionCmd(dataCmd);
        RewardMissionMsg rewardMsg = new RewardMissionMsg();
        byte errorCode = -100;
        try {
            String moneyType = "vin";
            moneyType = cmd.moneyType == 1 ? "vin" : "xu";
            CompleteMissionObj obj = this.missionService.completeMission(user.getName(), cmd.missionName, moneyType);
            if (obj.isUpdateSuccess()) {
                errorCode = 1;
                rewardMsg.currentMoney = obj.getMoneyUser();
                rewardMsg.prize = (int)obj.getMoneyBonus();
            } else {
                errorCode = this.parseRewardMissionError(obj.getError());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        rewardMsg.Error = errorCode;
        this.send((BaseMsg)rewardMsg, user);
        if (errorCode == 1) {
            this.getListMission(user);
        }
        Debug.trace(("Mission Module: " + user.getName() + " nhan thuong error_code=" + errorCode));
    }

    private byte parseRewardMissionError(String errorStr) {
        switch (errorStr) {
            case "1047": {
                return -1;
            }
            case "1048": {
                return -2;
            }
            case "1049": {
                return -3;
            }
        }
        return -100;
    }
}

