/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.common.business.Debug
 *  com.vinplay.dailyQuest.DailyQuestConfig
 *  com.vinplay.dailyQuest.DailyQuestUtils
 *  com.vinplay.dailyQuest.model.DailyGiftData
 *  com.vinplay.dailyQuest.model.DailyQuestModel
 */
package game.modules.quest;

import bitzero.server.entities.User;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.Debug;
import com.vinplay.dailyQuest.DailyQuestConfig;
import com.vinplay.dailyQuest.DailyQuestUtils;
import com.vinplay.dailyQuest.model.DailyGiftData;
import com.vinplay.dailyQuest.model.DailyQuestModel;
import game.GameConfig.GameConfig;
import game.modules.quest.DailyQuestSendData;
import game.modules.quest.cmd.rev.ReceiveGiftCmd;
import game.modules.quest.cmd.send.GetListQuestMsg;
import game.modules.quest.cmd.send.ReceiveGiftMsg;
import java.util.ArrayList;

public class DailyQuestModule
extends BaseClientRequestHandler {
    public void init() {
        super.init();
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 21000: {
                this.getListQuest(user);
                break;
            }
            case 21001: {
                this.receiveQuest(user, dataCmd);
            }
        }
    }

    private void getListQuest(User user) {
        DailyQuestModel dailyQuestModel = DailyQuestUtils.getDailyQuestModel((String)user.getName());
        Debug.trace((Object[])new Object[]{"dailyQuestModel cached=" + GameConfig.gson.toJson(dailyQuestModel)});
        ArrayList<DailyQuestSendData> dailyQuestSendData = new ArrayList<DailyQuestSendData>();
        for (int i = 0; i < DailyQuestConfig.questActive.length; ++i) {
            if (!DailyQuestConfig.questActive[i]) continue;
            dailyQuestSendData.add(new DailyQuestSendData(DailyQuestConfig.allQuest[i], (DailyGiftData)dailyQuestModel.dailyGiftData.get(i), i));
        }
        Debug.trace((Object[])new Object[]{"dailyQuestSendData size=" + dailyQuestSendData.size()});
        GetListQuestMsg getListQuestMsg = new GetListQuestMsg(GameConfig.gson.toJson(dailyQuestSendData));
        Debug.trace((Object[])new Object[]{"getListQuestMsg =" + GameConfig.gson.toJson(getListQuestMsg)});
        this.send(getListQuestMsg, user);
    }

    private synchronized void receiveQuest(User user, DataCmd dataCmd) {
        ReceiveGiftCmd receiveGiftCmd = new ReceiveGiftCmd(dataCmd);
        boolean isSuccess = DailyQuestUtils.playerReceiveGift((String)user.getName(), (int)receiveGiftCmd.indexQuest);
        ReceiveGiftMsg receiveGiftMsg = new ReceiveGiftMsg(user.getName(), isSuccess);
        this.send(receiveGiftMsg, user);
    }
}

