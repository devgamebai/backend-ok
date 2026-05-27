/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dailyQuest.model;

import com.vinplay.dailyQuest.DailyQuestActionReceiveGift;
import com.vinplay.dailyQuest.DailyQuestConfig;
import com.vinplay.dailyQuest.model.DailyGiftData;
import com.vinplay.vbee.common.enums.Games;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

public class DailyQuestModel
implements Serializable {
    private static final long serialVersionUID = 1123581347L;
    public long lastTimeChange = 0L;
    public ArrayList<DailyGiftData> dailyGiftData = new ArrayList();
    public String userName;

    public DailyQuestModel(String userName) {
        this.lastTimeChange = DailyQuestModel.getTimeStampInDay();
        this.userName = userName;
        for (int i = 0; i < DailyQuestConfig.allQuest.length; ++i) {
            this.dailyGiftData.add(new DailyGiftData());
        }
    }

    public boolean receiveGiftDailyQuest(int index) {
        if (DailyQuestConfig.questActive[index] && this.dailyGiftData.get(index).receiveGift()) {
            if (DailyQuestConfig.allQuest[index].giftType == 1) {
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.SPARTAN.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinSpartan(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.POKE_GO.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinPokeGo(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.GALAXY.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinGalaxy(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.BENLEY.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinBenley(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.AUDITION.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinAudition(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.TAMHUNG.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinTamHung(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.MAYBACH.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinMayBach(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.CHIEM_TINH.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinChiemtinh(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.ROLL_ROYE.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinThanBai(this.userName);
                }
                if (DailyQuestConfig.allQuest[index].giftTypeData.gameID == Games.BIKINI.getId()) {
                    DailyQuestActionReceiveGift.addFreeSpinBikini(this.userName);
                }
            }
            if (DailyQuestConfig.allQuest[index].giftType == 0) {
                if (DailyQuestConfig.allQuest[index].gameId == Games.NHIEM_VU.getId()) {
                    DailyQuestActionReceiveGift.addMoney(this.userName, DailyQuestConfig.allQuest[index].gift, (byte)0);
                }
                if (DailyQuestConfig.allQuest[index].gameId == Games.TAI_XIU.getId()) {
                    DailyQuestActionReceiveGift.addMoney(this.userName, DailyQuestConfig.allQuest[index].gift, (byte)1);
                }
                if (DailyQuestConfig.allQuest[index].gameId == Games.BAU_CUA.getId()) {
                    DailyQuestActionReceiveGift.addMoney(this.userName, DailyQuestConfig.allQuest[index].gift, (byte)2);
                }
            }
            return true;
        }
        return false;
    }

    public void playGame(int gameId, long value) {
        for (int i = 0; i < this.dailyGiftData.size(); ++i) {
            if (!DailyQuestConfig.questActive[i] || DailyQuestConfig.allQuest[i].gameId != gameId) continue;
            this.dailyGiftData.get((int)i).currentValue += value;
            if (this.dailyGiftData.get((int)i).currentValue < (long)DailyQuestConfig.allQuest[i].valueDone || this.dailyGiftData.get((int)i).isReceive || this.dailyGiftData.get((int)i).isSuccess) continue;
            this.dailyGiftData.get((int)i).isSuccess = true;
        }
    }

    public void playerLogin() {
        long currentTime = DailyQuestModel.getTimeStampInDay();
        long deltaDay = currentTime - this.lastTimeChange;
        if (deltaDay > 0L) {
            this.lastTimeChange = currentTime;
            for (int i = 0; i < this.dailyGiftData.size(); ++i) {
                this.dailyGiftData.get(i).resetData();
            }
        }
    }

    public static long getTimeStampInDay() {
        Calendar time = Calendar.getInstance();
        time.add(14, time.getTimeZone().getOffset(time.getTimeInMillis()));
        return time.getTimeInMillis() / 86400000L;
    }
}

