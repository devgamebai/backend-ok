/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.Games
 */
package com.vinplay.dailyQuest;

import com.vinplay.dailyQuest.model.DailyQuestData;
import com.vinplay.vbee.common.enums.Games;

public class DailyQuestConfig {
    public static DailyQuestData[] allQuest = new DailyQuestData[]{new DailyQuestData(Games.NHIEM_VU.getId(), 500000, 0, 10000, 0, -1, -1, 0, "N\u1ea1p th\u1ebb 500k t\u1eb7ng 10k"), new DailyQuestData(Games.MINI_POKER.getId(), 200000, 0, 1, 1, Games.SPARTAN.getId(), 100, 1, "T\u1ed5ng c\u01b0\u1ee3c Minipoker 200k T\u1eb7ng 1 l\u01b0\u1ee3t quay Th\u1ea7n t\u00e0i 100"), new DailyQuestData(Games.CHIEM_TINH.getId(), 200000, 0, 1, 1, Games.BENLEY.getId(), 100, 2, "T\u1ed5ng c\u01b0\u1ee3c Chi\u00eam tinh 200k T\u1eb7ng 1 l\u01b0\u1ee3t quay Bitcoin 100"), new DailyQuestData(Games.MAYBACH.getId(), 200000, 0, 1, 1, Games.BENLEY.getId(), 100, 3, "T\u1ed5ng c\u01b0\u1ee3c B\u00f3ng \u0110\u00e1 200k T\u1eb7ng 1 l\u01b0\u1ee3t quay Bitcoin 100"), new DailyQuestData(Games.BENLEY.getId(), 200000, 0, 1, 1, Games.AUDITION.getId(), 100, 4, "T\u1ed5ng c\u01b0\u1ee3c Bitcoin 200k T\u1eb7ng 1 l\u01b0\u1ee3t quay \u0110ua Xe 100"), new DailyQuestData(Games.AUDITION.getId(), 200000, 0, 1, 1, Games.TAMHUNG.getId(), 100, 5, "T\u1ed5ng c\u01b0\u1ee3c \u0110ua Xe 200k T\u1eb7ng 1 l\u01b0\u1ee3t quay Chim \u0110i\u00ean 100"), new DailyQuestData(Games.TAMHUNG.getId(), 200000, 0, 1, 1, Games.MAYBACH.getId(), 100, 6, "T\u1ed5ng c\u01b0\u1ee3c Chim \u0110i\u00ean 200k T\u1eb7ng 1 l\u01b0\u1ee3t quay B\u00f3ng \u0110\u00e1 100"), new DailyQuestData(Games.ROLL_ROYE.getId(), 200000, 0, 2000, 1, Games.TAMHUNG.getId(), 100, 7, "T\u1ed5ng c\u01b0\u1ee3c Th\u1ea7n B\u00e0i 200k T\u1eb7ng 1 l\u01b0\u1ee3t quay Chim \u0110i\u00ean 100"), new DailyQuestData(Games.TAI_XIU.getId(), 500000, 0, 2000, 0, -1, -1, 8, "T\u1ed5ng c\u01b0\u1ee3c T\u00e0i x\u1ec9u 500k T\u1eb7ng 2k"), new DailyQuestData(Games.BAU_CUA.getId(), 500000, 0, 2000, 0, -1, -1, 9, "T\u1ed5ng c\u01b0\u1ee3c B\u1ea7u cua 500k T\u1eb7ng 2k")};
    public static boolean[] questActive = new boolean[]{false, false, true, false, true, true, true, true, false, false};
}

