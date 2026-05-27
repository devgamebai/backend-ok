/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.dailyQuest.model.DailyGiftData
 *  com.vinplay.dailyQuest.model.DailyQuestData
 */
package game.modules.quest;

import com.vinplay.dailyQuest.model.DailyGiftData;
import com.vinplay.dailyQuest.model.DailyQuestData;

public class DailyQuestSendData {
    public DailyQuestData dailyQuestData;
    public DailyGiftData dailyGiftData;
    public int index;

    public DailyQuestSendData(DailyQuestData dailyQuestData, DailyGiftData dailyGiftData, int index) {
        this.dailyQuestData = dailyQuestData;
        this.dailyGiftData = dailyGiftData;
        this.index = index;
    }
}

