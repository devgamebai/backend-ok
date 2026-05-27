/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 */
package com.vinplay.dailyQuest;

import com.google.gson.Gson;
import com.vinplay.dailyQuest.DailyQuestDescription;
import com.vinplay.dal.service.impl.SlotMachineServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.statics.TransType;

public class DailyQuestActionReceiveGift {
    public static Gson gson = new Gson();

    public static void addMoney(String userName, long money, byte action) {
        UserServiceImpl userService = new UserServiceImpl();
        userService.updateMoney(userName, money, "vin", "Nhi\u1ec7m v\u1ee5", Games.NHIEM_VU.getId() + "", gson.toJson(new DailyQuestDescription(action)), 0L, 0L, TransType.END_TRANS);
    }

    public static void addFreeSpinSpartan(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.SPARTAN.getName(), userName, 100);
    }

    public static void addFreeSpinPokeGo(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.POKE_GO.getName(), userName, 100);
    }

    public static void addFreeSpinGalaxy(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.GALAXY.getName(), userName, 100);
    }

    public static void addFreeSpinBenley(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.BENLEY.getName(), userName, 100);
    }

    public static void addFreeSpinChiemtinh(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.CHIEM_TINH.getName(), userName, 100);
    }

    public static void addFreeSpinAudition(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.AUDITION.getName(), userName, 100);
    }

    public static void addFreeSpinTamHung(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.TAMHUNG.getName(), userName, 100);
    }

    public static void addFreeSpinMayBach(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.MAYBACH.getName(), userName, 100);
    }

    public static void addFreeSpinThanBai(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.ROLL_ROYE.getName(), userName, 100);
    }

    public static void addFreeSpinBikini(String userName) {
        SlotMachineServiceImpl slotMachineService = new SlotMachineServiceImpl();
        slotMachineService.setLuotQuayFreeDaily(Games.BIKINI.getName(), userName, 100);
    }
}

