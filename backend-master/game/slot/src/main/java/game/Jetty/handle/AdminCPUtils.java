package game.Jetty.handle;

import game.Jetty.model.FundData;
import game.Jetty.model.JackpotData;
import game.modules.slot.*;
import game.modules.slot.room.*;

public class AdminCPUtils {

    // ************ BITCOIN ************************
    public static BenleyRoom[] getListBenleyRoom() {
        BenleyRoom room100 = (BenleyRoom) BenleyModule.getInstance().rooms.get(BenleyModule.getInstance().gameName + "_vin_100");
        BenleyRoom room1000 = (BenleyRoom) BenleyModule.getInstance().rooms.get(BenleyModule.getInstance().gameName + "_vin_1000");
        BenleyRoom room10000 = (BenleyRoom) BenleyModule.getInstance().rooms.get(BenleyModule.getInstance().gameName + "_vin_10000");
        return new BenleyRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotBitcoin() {
        JackpotData jackpotData = new JackpotData();
        BenleyRoom[] benleyRooms = getListBenleyRoom();
        jackpotData.listJackpot = new long[benleyRooms.length];
        for (int i = 0; i < benleyRooms.length; i++) {
            jackpotData.listJackpot[i] = benleyRooms[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlotBitcoin(JackpotData jackpotData) {
        BenleyRoom[] benleyRooms = getListBenleyRoom();
        for (int i = 0; i < benleyRooms.length; i++) {
            benleyRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotBitcoin() {
        FundData fundData = new FundData();
        BenleyRoom[] benleyRooms = getListBenleyRoom();

        fundData.listFundJackpot = new long[benleyRooms.length];
        fundData.listFundMinigame = new long[benleyRooms.length];
        fundData.listFundPayline = new long[benleyRooms.length];

        for (int i = 0; i < benleyRooms.length; i++) {
            fundData.listFundJackpot[i] = benleyRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = benleyRooms[i].fundMinigame;
            fundData.listFundPayline[i] = benleyRooms[i].fund;
        }
        return fundData;
    }

    public static void setFundDataSlotBitcoin(FundData fundData) {
        BenleyRoom[] benleyRooms = getListBenleyRoom();
        for (int i = 0; i < benleyRooms.length; i++) {
            benleyRooms[i].fundJackPot = fundData.listFundJackpot[i];
            benleyRooms[i].fundMinigame = fundData.listFundMinigame[i];
            benleyRooms[i].fund = fundData.listFundPayline[i];
        }
    }
    
    //************ CHIEN TINH ************************
    public static ChiemTinhRoom[] getListChiemTinhRoom() {
        ChiemTinhRoom room100 = (ChiemTinhRoom) ChiemTinhModule.getInstance().rooms.get(ChiemTinhModule.getInstance().gameName + "_vin_100");
        ChiemTinhRoom room1000 = (ChiemTinhRoom) ChiemTinhModule.getInstance().rooms.get(ChiemTinhModule.getInstance().gameName + "_vin_1000");
        ChiemTinhRoom room10000 = (ChiemTinhRoom) ChiemTinhModule.getInstance().rooms.get(ChiemTinhModule.getInstance().gameName + "_vin_10000");
        return new ChiemTinhRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotChiemTinh() {
        JackpotData jackpotData = new JackpotData();
        ChiemTinhRoom[] benleyRooms = getListChiemTinhRoom();
        jackpotData.listJackpot = new long[benleyRooms.length];
        for (int i = 0; i < benleyRooms.length; i++) {
            jackpotData.listJackpot[i] = benleyRooms[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlotChiemTinh(JackpotData jackpotData) {
    	ChiemTinhRoom[] benleyRooms = getListChiemTinhRoom();
        for (int i = 0; i < benleyRooms.length; i++) {
            benleyRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotChiemTinh() {
        FundData fundData = new FundData();
        ChiemTinhRoom[] benleyRooms = getListChiemTinhRoom();

        fundData.listFundJackpot = new long[benleyRooms.length];
        fundData.listFundMinigame = new long[benleyRooms.length];
        fundData.listFundPayline = new long[benleyRooms.length];

        for (int i = 0; i < benleyRooms.length; i++) {
            fundData.listFundJackpot[i] = benleyRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = benleyRooms[i].fundMinigame;
            fundData.listFundPayline[i] = benleyRooms[i].fund;
        }
        return fundData;
    }

    public static void setFundDataSlotChiemtinh(FundData fundData) {
    	ChiemTinhRoom[] benleyRooms = getListChiemTinhRoom();
        for (int i = 0; i < benleyRooms.length; i++) {
            benleyRooms[i].fundJackPot = fundData.listFundJackpot[i];
            benleyRooms[i].fundMinigame = fundData.listFundMinigame[i];
            benleyRooms[i].fund = fundData.listFundPayline[i];
        }
    }

    //************ DUA XE ************************
    public static AuditionRoom[] getListAuditionTamHungRoom() {
        AuditionRoom room100 = (AuditionRoom) AuditionModule.getInstance().rooms.get(AuditionModule.getInstance().gameName + "_vin_100");
        AuditionRoom room1000 = (AuditionRoom) AuditionModule.getInstance().rooms.get(AuditionModule.getInstance().gameName + "_vin_1000");
        AuditionRoom room10000 = (AuditionRoom) AuditionModule.getInstance().rooms.get(AuditionModule.getInstance().gameName + "_vin_10000");
        return new AuditionRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotTayDu() {
        JackpotData jackpotData = new JackpotData();
        AuditionRoom[] auditionRooms = getListAuditionTamHungRoom();
        jackpotData.listJackpot = new long[auditionRooms.length];
        for (int i = 0; i < auditionRooms.length; i++) {
            jackpotData.listJackpot[i] = auditionRooms[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlotTayDu(JackpotData jackpotData) {
        AuditionRoom[] auditionRooms = getListAuditionTamHungRoom();
        for (int i = 0; i < auditionRooms.length; i++) {
            auditionRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotTayDu() {
        FundData fundData = new FundData();
        AuditionRoom[] auditionRooms = getListAuditionTamHungRoom();

        fundData.listFundJackpot = new long[auditionRooms.length];
        fundData.listFundMinigame = new long[auditionRooms.length];
        fundData.listFundPayline = new long[auditionRooms.length];

        for (int i = 0; i < auditionRooms.length; i++) {
            fundData.listFundJackpot[i] = auditionRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = auditionRooms[i].fundMinigame;
            fundData.listFundPayline[i] = auditionRooms[i].fund;
        }
        return fundData;
    }

    public static void setFundDataSlotTayDu(FundData fundData) {
        AuditionRoom[] auditionRooms = getListAuditionTamHungRoom();
        for (int i = 0; i < auditionRooms.length; i++) {
            auditionRooms[i].fundJackPot = fundData.listFundJackpot[i];
            auditionRooms[i].fundMinigame = fundData.listFundMinigame[i];
            auditionRooms[i].fund = fundData.listFundPayline[i];
        }
    }

    //************ CHIM DIEN ************************
    public static TamHungRoom[] getListTamHungRoom() {
        TamHungRoom room100 = (TamHungRoom) TamHungModule.getInstance().rooms.get(TamHungModule.getInstance().gameName + "_vin_100");
        TamHungRoom room1000 = (TamHungRoom) TamHungModule.getInstance().rooms.get(TamHungModule.getInstance().gameName + "_vin_1000");
        TamHungRoom room10000 = (TamHungRoom) TamHungModule.getInstance().rooms.get(TamHungModule.getInstance().gameName + "_vin_10000");
        return new TamHungRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotChimDien() {
        JackpotData jackpotData = new JackpotData();
        TamHungRoom[] tamHungRooms = getListTamHungRoom();
        jackpotData.listJackpot = new long[tamHungRooms.length];
        for (int i = 0; i < tamHungRooms.length; i++) {
            jackpotData.listJackpot[i] = tamHungRooms[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlotChimDien(JackpotData jackpotData) {
        TamHungRoom[] tamHungRooms = getListTamHungRoom();
        for (int i = 0; i < tamHungRooms.length; i++) {
            tamHungRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotChimDien() {
        FundData fundData = new FundData();
        TamHungRoom[] tamHungRooms = getListTamHungRoom();

        fundData.listFundJackpot = new long[tamHungRooms.length];
        fundData.listFundMinigame = new long[tamHungRooms.length];
        fundData.listFundPayline = new long[tamHungRooms.length];

        for (int i = 0; i < tamHungRooms.length; i++) {
            fundData.listFundJackpot[i] = tamHungRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = tamHungRooms[i].fundMinigame;
            fundData.listFundPayline[i] = tamHungRooms[i].fund;
        }
        return fundData;
    }

    public static void setFundDataSlotChimDien(FundData fundData) {
        TamHungRoom[] tamHungRooms = getListTamHungRoom();
        for (int i = 0; i < tamHungRooms.length; i++) {
            tamHungRooms[i].fundJackPot = fundData.listFundJackpot[i];
            tamHungRooms[i].fundMinigame = fundData.listFundMinigame[i];
            tamHungRooms[i].fund = fundData.listFundPayline[i];
        }
    }

    //************ THAN TAI ************************
    public static SpartanRoom[] getListSpartanRoom() {
        SpartanRoom room100 = (SpartanRoom) SpartanModule.getInstance().rooms.get(SpartanModule.getInstance().gameName + "_vin_100");
        SpartanRoom room1000 = (SpartanRoom) SpartanModule.getInstance().rooms.get(SpartanModule.getInstance().gameName + "_vin_1000");
        SpartanRoom room10000 = (SpartanRoom) SpartanModule.getInstance().rooms.get(SpartanModule.getInstance().gameName + "_vin_10000");
        return new SpartanRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotThanTai() {
        JackpotData jackpotData = new JackpotData();
        SpartanRoom[] spartanRooms = getListSpartanRoom();
        jackpotData.listJackpot = new long[spartanRooms.length];
        for (int i = 0; i < spartanRooms.length; i++) {
            jackpotData.listJackpot[i] = spartanRooms[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlotThanTai(JackpotData jackpotData) {
        SpartanRoom[] spartanRooms = getListSpartanRoom();
        for (int i = 0; i < spartanRooms.length; i++) {
            spartanRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotThanTai() {
        FundData fundData = new FundData();
        SpartanRoom[] spartanRooms = getListSpartanRoom();

        fundData.listFundJackpot = new long[spartanRooms.length];
        fundData.listFundMinigame = new long[spartanRooms.length];
        fundData.listFundPayline = new long[spartanRooms.length];

        for (int i = 0; i < spartanRooms.length; i++) {
            fundData.listFundJackpot[i] = spartanRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = spartanRooms[i].fundMinigame;
            fundData.listFundPayline[i] = spartanRooms[i].fund;
        }
        return fundData;
    }

    public static void setFundDataSlotThanTai(FundData fundData) {
        SpartanRoom[] spartanRooms = getListSpartanRoom();
        for (int i = 0; i < spartanRooms.length; i++) {
            spartanRooms[i].fundJackPot = fundData.listFundJackpot[i];
            spartanRooms[i].fundMinigame = fundData.listFundMinigame[i];
            spartanRooms[i].fund = fundData.listFundPayline[i];
        }
    }

    //************ THE THAO ************************
    public static MayBachRoom[] getListMayBachRoom() {
        MayBachRoom room100 = (MayBachRoom) MayBachModule.getInstance().rooms.get(MayBachModule.getInstance().gameName + "_vin_100");
        MayBachRoom room1000 = (MayBachRoom)MayBachModule.getInstance().rooms.get(MayBachModule.getInstance().gameName + "_vin_1000");
        MayBachRoom room10000 = (MayBachRoom)MayBachModule.getInstance().rooms.get(MayBachModule.getInstance().gameName + "_vin_10000");
        return new MayBachRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotTheThao() {
        JackpotData jackpotData = new JackpotData();
        MayBachRoom[] mayBachRooms = getListMayBachRoom();
        jackpotData.listJackpot = new long[mayBachRooms.length];
        for (int i = 0; i < mayBachRooms.length; i++) {
            jackpotData.listJackpot[i] = mayBachRooms[i].pot;
        }
        return jackpotData;
    }

    public static void setJackpotDataSlotTheThao(JackpotData jackpotData) {
        MayBachRoom[] mayBachRooms = getListMayBachRoom();
        for (int i = 0; i < mayBachRooms.length; i++) {
            mayBachRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotTheThao() {
        FundData fundData = new FundData();
        MayBachRoom[] mayBachRooms = getListMayBachRoom();

        fundData.listFundJackpot = new long[mayBachRooms.length];
        fundData.listFundMinigame = new long[mayBachRooms.length];
        fundData.listFundPayline = new long[mayBachRooms.length];

        for (int i = 0; i < mayBachRooms.length; i++) {
            fundData.listFundJackpot[i] = mayBachRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = mayBachRooms[i].fundMinigame;
            fundData.listFundPayline[i] = mayBachRooms[i].fund;
        }
        return fundData;
    }

    public static void setFundDataSlotTheThao(FundData fundData) {
        MayBachRoom[] mayBachRooms = getListMayBachRoom();
        for (int i = 0; i < mayBachRooms.length; i++) {
            mayBachRooms[i].fundJackPot = fundData.listFundJackpot[i];
            mayBachRooms[i].fundMinigame = fundData.listFundMinigame[i];
            mayBachRooms[i].fund = fundData.listFundPayline[i];
        }
    }

    //************ THE THAO ************************
    public static RollRoyRoom[] getListRollRoyRoom() {
        RollRoyRoom room100 = (RollRoyRoom) RollRoyModule.getInstance().rooms.get(RollRoyModule.getInstance().gameName + "_vin_100");
        RollRoyRoom room1000 = (RollRoyRoom)RollRoyModule.getInstance().rooms.get(RollRoyModule.getInstance().gameName + "_vin_1000");
        RollRoyRoom room10000 = (RollRoyRoom)RollRoyModule.getInstance().rooms.get(RollRoyModule.getInstance().gameName + "_vin_10000");
        return new RollRoyRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotThanBai() {
        JackpotData jackpotData = new JackpotData();
        RollRoyRoom[] rollRoyRooms = getListRollRoyRoom();
        jackpotData.listJackpot = new long[rollRoyRooms.length];

        for (int i = 0; i < rollRoyRooms.length; i++) {
            jackpotData.listJackpot[i] = rollRoyRooms[i].pot;
        }

        return jackpotData;
    }

    public static void setJackpotDataSlotThanBai(JackpotData jackpotData) {
        RollRoyRoom[] rollRoyRooms = getListRollRoyRoom();

        for (int i = 0; i < rollRoyRooms.length; i++) {
            rollRoyRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotThanBai() {
        FundData fundData = new FundData();
        RollRoyRoom[] rollRoyRooms = getListRollRoyRoom();

        fundData.listFundJackpot = new long[rollRoyRooms.length];
        fundData.listFundMinigame = new long[rollRoyRooms.length];
        fundData.listFundPayline = new long[rollRoyRooms.length];

        for (int i = 0; i < rollRoyRooms.length; i++) {
            fundData.listFundJackpot[i] = rollRoyRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = rollRoyRooms[i].fundMinigame;
            fundData.listFundPayline[i] = rollRoyRooms[i].fund;
        }

        return fundData;
    }

    public static void setFundDataSlotThanBai(FundData fundData) {
        RollRoyRoom[] rollRoyRooms = getListRollRoyRoom();

        for (int i = 0; i < rollRoyRooms.length; i++) {
            rollRoyRooms[i].fundJackPot = fundData.listFundJackpot[i];
            rollRoyRooms[i].fundMinigame = fundData.listFundMinigame[i];
            rollRoyRooms[i].fund = fundData.listFundPayline[i];
        }
    }

    // ************ BIKINI ************************
    public static BikiniRoom[] getListBikiniRoom() {
        BikiniRoom room100 = (BikiniRoom) BikiniModule.getInstance().rooms.get(BikiniModule.getInstance().gameName + "_vin_100");
        BikiniRoom room1000 = (BikiniRoom) BikiniModule.getInstance().rooms.get(BikiniModule.getInstance().gameName + "_vin_1000");
        BikiniRoom room10000 = (BikiniRoom) BikiniModule.getInstance().rooms.get(BikiniModule.getInstance().gameName + "_vin_10000");

        return new BikiniRoom[]{room100, room1000, room10000};
    }

    public static JackpotData getJackpotDataSlotBikini() {
        JackpotData jackpotData = new JackpotData();
        BikiniRoom[] bikiniRooms = getListBikiniRoom();
        jackpotData.listJackpot = new long[bikiniRooms.length];

        for (int i = 0; i < bikiniRooms.length; i++) {
            jackpotData.listJackpot[i] = bikiniRooms[i].pot;
        }

        return jackpotData;
    }

    public static void setJackpotDataSlotBikini(JackpotData jackpotData) {
        BikiniRoom[] bikiniRooms = getListBikiniRoom();

        for (int i = 0; i < bikiniRooms.length; i++) {
            bikiniRooms[i].pot = jackpotData.listJackpot[i];
        }
    }

    public static FundData getFundDataSlotBikini() {
        FundData fundData = new FundData();
        BikiniRoom[] bikiniRooms = getListBikiniRoom();

        fundData.listFundJackpot = new long[bikiniRooms.length];
        fundData.listFundMinigame = new long[bikiniRooms.length];
        fundData.listFundPayline = new long[bikiniRooms.length];

        for (int i = 0; i < bikiniRooms.length; i++) {
            fundData.listFundJackpot[i] = bikiniRooms[i].fundJackPot;
            fundData.listFundMinigame[i] = bikiniRooms[i].fundMinigame;
            fundData.listFundPayline[i] = bikiniRooms[i].fund;
        }

        return fundData;
    }

    public static void setFundDataSlotBikini(FundData fundData) {
        BikiniRoom[] bikiniRooms = getListBikiniRoom();

        for (int i = 0; i < bikiniRooms.length; i++) {
            bikiniRooms[i].fundJackPot = fundData.listFundJackpot[i];
            bikiniRooms[i].fundMinigame = fundData.listFundMinigame[i];
            bikiniRooms[i].fund = fundData.listFundPayline[i];
        }
    }

}
