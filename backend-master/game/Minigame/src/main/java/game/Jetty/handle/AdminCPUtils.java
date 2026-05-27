/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.enums.Games
 */
package game.Jetty.handle;

import com.vinplay.vbee.common.enums.Games;
import game.Jetty.model.FundData;
import game.Jetty.model.FundDataSlot;
import game.Jetty.model.JackpotData;
import game.modules.minigame.CandyModule;
import game.modules.minigame.CaoThapModule;
import game.modules.minigame.GalaxyModule;
import game.modules.minigame.MiniPokerModule;
import game.modules.minigame.room.MGRoomCandy;
import game.modules.minigame.room.MGRoomCaoThap;
import game.modules.minigame.room.MGRoomGalaxy;
import game.modules.minigame.room.MGRoomMiniPoker;

public class AdminCPUtils {
    public static MGRoomMiniPoker[] getMinipokerRoom() {
        MGRoomMiniPoker room100 = (MGRoomMiniPoker)MiniPokerModule.rooms.get(Games.MINI_POKER.getName() + "_vin_100");
        MGRoomMiniPoker room1000 = (MGRoomMiniPoker)MiniPokerModule.rooms.get(Games.MINI_POKER.getName() + "_vin_1000");
        MGRoomMiniPoker room10000 = (MGRoomMiniPoker)MiniPokerModule.rooms.get(Games.MINI_POKER.getName() + "_vin_10000");
        return new MGRoomMiniPoker[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotMinipoker() {
        JackpotData jackpotData = new JackpotData();
        MGRoomMiniPoker[] minipokerRooms = AdminCPUtils.getMinipokerRoom();
        jackpotData.listJackpot = new long[minipokerRooms.length];
        for (int i = 0; i < minipokerRooms.length; ++i) {
            jackpotData.listJackpot[i] = minipokerRooms[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlotMinipoker(JackpotData jackpotData) {
        MGRoomMiniPoker[] minipokerRooms = AdminCPUtils.getMinipokerRoom();
        for (int i = 0; i < minipokerRooms.length; ++i) {
            minipokerRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundDataSlot getFundDataSlotMinipoker() {
        FundDataSlot fundDataSlot = new FundDataSlot();
        MGRoomMiniPoker[] minipokerRooms = AdminCPUtils.getMinipokerRoom();
        fundDataSlot.listFundJackpot = new long[minipokerRooms.length];
        fundDataSlot.listFundPayline = new long[minipokerRooms.length];
        for (int i = 0; i < minipokerRooms.length; ++i) {
            fundDataSlot.listFundJackpot[i] = minipokerRooms[i].fundJackPot;
            fundDataSlot.listFundPayline[i] = minipokerRooms[i].fund;
        }
        return fundDataSlot;
    }

    public static void setFundDataSlotMinipoker(FundDataSlot fundDataSlot) {
        MGRoomMiniPoker[] minipokerRooms = AdminCPUtils.getMinipokerRoom();
        for (int i = 0; i < minipokerRooms.length; ++i) {
            minipokerRooms[i].fundJackPot = fundDataSlot.listFundJackpot[i];
            minipokerRooms[i].fund = fundDataSlot.listFundPayline[i];
        }
    }

    public static MGRoomCandy[] getCandyRoom() {
        MGRoomCandy room100 = (MGRoomCandy)CandyModule.rooms.get(Games.CANDY.getName() + "_vin_100");
        MGRoomCandy room1000 = (MGRoomCandy)CandyModule.rooms.get(Games.CANDY.getName() + "_vin_1000");
        MGRoomCandy room10000 = (MGRoomCandy)CandyModule.rooms.get(Games.CANDY.getName() + "_vin_10000");
        return new MGRoomCandy[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlot3x3() {
        JackpotData jackpotData = new JackpotData();
        MGRoomCandy[] mgRoomCandies = AdminCPUtils.getCandyRoom();
        jackpotData.listJackpot = new long[mgRoomCandies.length];
        for (int i = 0; i < mgRoomCandies.length; ++i) {
            jackpotData.listJackpot[i] = mgRoomCandies[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlot3x3(JackpotData jackpotData) {
        MGRoomCandy[] mgRoomCandies = AdminCPUtils.getCandyRoom();
        for (int i = 0; i < mgRoomCandies.length; ++i) {
            mgRoomCandies[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundDataSlot getFundDataSlot3x3() {
        FundDataSlot fundDataSlot = new FundDataSlot();
        MGRoomCandy[] mgRoomCandies = AdminCPUtils.getCandyRoom();
        fundDataSlot.listFundJackpot = new long[mgRoomCandies.length];
        fundDataSlot.listFundPayline = new long[mgRoomCandies.length];
        for (int i = 0; i < mgRoomCandies.length; ++i) {
            fundDataSlot.listFundJackpot[i] = mgRoomCandies[i].fundJackPot;
            fundDataSlot.listFundPayline[i] = mgRoomCandies[i].fund;
        }
        return fundDataSlot;
    }

    public static void setFundDataSlot3x3(FundDataSlot fundDataSlot) {
        MGRoomCandy[] mgRoomCandies = AdminCPUtils.getCandyRoom();
        for (int i = 0; i < mgRoomCandies.length; ++i) {
            mgRoomCandies[i].fundJackPot = fundDataSlot.listFundJackpot[i];
            mgRoomCandies[i].fund = fundDataSlot.listFundPayline[i];
        }
    }

    public static MGRoomCaoThap[] getCaoThapRoom() {
        MGRoomCaoThap room1000 = (MGRoomCaoThap)CaoThapModule.getInstance().rooms.get("cao_thap_vin_1000");
        MGRoomCaoThap room10000 = (MGRoomCaoThap)CaoThapModule.getInstance().rooms.get("cao_thap_vin_10000");
        MGRoomCaoThap room50000 = (MGRoomCaoThap)CaoThapModule.getInstance().rooms.get("cao_thap_vin_50000");
        MGRoomCaoThap room100000 = (MGRoomCaoThap)CaoThapModule.getInstance().rooms.get("cao_thap_vin_100000");
        MGRoomCaoThap room500000 = (MGRoomCaoThap)CaoThapModule.getInstance().rooms.get("cao_thap_vin_500000");
        return new MGRoomCaoThap[]{room1000, room10000, room50000, room100000, room500000};
    }

    public static JackpotData getJackpotDataCaoThap() {
        JackpotData jackpotData = new JackpotData();
        MGRoomCaoThap[] mgRoomCaoThaps = AdminCPUtils.getCaoThapRoom();
        jackpotData.listJackpot = new long[mgRoomCaoThaps.length];
        for (int i = 0; i < mgRoomCaoThaps.length; ++i) {
            jackpotData.listJackpot[i] = mgRoomCaoThaps[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataCaoThap(JackpotData jackpotData) {
        MGRoomCaoThap[] mgRoomCaoThaps = AdminCPUtils.getCaoThapRoom();
        for (int i = 0; i < mgRoomCaoThaps.length; ++i) {
            mgRoomCaoThaps[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataCaoThap() {
        FundData fundData = new FundData();
        MGRoomCaoThap[] mgRoomCaoThaps = AdminCPUtils.getCaoThapRoom();
        fundData.listFund = new long[mgRoomCaoThaps.length];
        for (int i = 0; i < mgRoomCaoThaps.length; ++i) {
            fundData.listFund[i] = mgRoomCaoThaps[i].fund;
        }
        return fundData;
    }

    public static void setFundDataCaoTHap(FundData fundData) {
        MGRoomCaoThap[] mgRoomCaoThaps = AdminCPUtils.getCaoThapRoom();
        for (int i = 0; i < mgRoomCaoThaps.length; ++i) {
            mgRoomCaoThaps[i].fund = fundData.listFund[i];
        }
    }

    public static MGRoomGalaxy[] getGalaxyRoom() {
        MGRoomGalaxy room100 = (MGRoomGalaxy)GalaxyModule.rooms.get(Games.GALAXY.getName() + "_vin_100");
        MGRoomGalaxy room1000 = (MGRoomGalaxy)GalaxyModule.rooms.get(Games.GALAXY.getName() + "_vin_1000");
        MGRoomGalaxy room10000 = (MGRoomGalaxy)GalaxyModule.rooms.get(Games.GALAXY.getName() + "_vin_10000");
        return new MGRoomGalaxy[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataGalaxy() {
        JackpotData jackpotData = new JackpotData();
        MGRoomGalaxy[] mgRoomsGalaxy = AdminCPUtils.getGalaxyRoom();
        jackpotData.listJackpot = new long[mgRoomsGalaxy.length];
        for (int i = 0; i < mgRoomsGalaxy.length; ++i) {
            jackpotData.listJackpot[i] = mgRoomsGalaxy[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataGalaxy(JackpotData jackpotData) {
        MGRoomGalaxy[] mgRoomsGalaxy = AdminCPUtils.getGalaxyRoom();
        for (int i = 0; i < mgRoomsGalaxy.length; ++i) {
            mgRoomsGalaxy[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundDataSlot getFundDataGalaxy() {
        FundDataSlot fundDataSlot = new FundDataSlot();
        MGRoomGalaxy[] mgRoomsGalaxy = AdminCPUtils.getGalaxyRoom();
        fundDataSlot.listFundJackpot = new long[mgRoomsGalaxy.length];
        fundDataSlot.listFundPayline = new long[mgRoomsGalaxy.length];
        for (int i = 0; i < mgRoomsGalaxy.length; ++i) {
            fundDataSlot.listFundJackpot[i] = mgRoomsGalaxy[i].fundJackPot;
            fundDataSlot.listFundPayline[i] = mgRoomsGalaxy[i].fund;
        }
        return fundDataSlot;
    }

    public static void setFundDataGalaxy(FundDataSlot fundDataSlot) {
        MGRoomGalaxy[] mgRoomsGalaxy = AdminCPUtils.getGalaxyRoom();
        for (int i = 0; i < mgRoomsGalaxy.length; ++i) {
            mgRoomsGalaxy[i].fundJackPot = fundDataSlot.listFundJackpot[i];
            mgRoomsGalaxy[i].fund = fundDataSlot.listFundPayline[i];
        }
    }
}

