/*
 * Games enum — declaration order MUST match precompiled (ordinal-sensitive).
 * Precompiled Minigame.jar uses enum switch tables keyed by ordinal.
 * Adding new values: append ONLY at the end, never insert in the middle.
 */
package com.vinplay.vbee.common.enums;

public enum Games {
    // --- Ordinals 0–67: must match precompiled exactly ---
    MINIGAME(0, "MiniGame", "Minigame"),
    MINI_POKER(1, "MiniPoker", "Mini poker"),
    TAI_XIU(2, "TaiXiu", "T\u00e0i x\u1ec9u"),
    BAU_CUA(3, "BauCua", "B\u1ea7u cua"),
    CAO_THAP(4, "CaoThap", "Cao th\u1ea5p"),
    POKE_GO(5, "PokeGo", "Pokego"),
    POKEMON(6, "Pokemon", "Pokemon"),
    VQMM(7, "VQMM", "V\u00f2ng quay may m\u1eafn"),
    SAM(8, "Sam", "S\u00e2m"),
    BA_CAY(9, "BaCay", "Ba c\u00e2y"),
    BINH(10, "Binh", "M\u1eadu binh"),
    TLMN(11, "Tlmn", "TLMN"),
    TA_LA(12, "TaLa", "T\u00e1 l\u1ea3"),
    LIENG(13, "Lieng", "Li\u00eang"),
    XI_TO(14, "XiTo", "X\u00ec t\u1ed1"),
    XOC_DIA(15, "XocDia", "X\u00f3c \u0111\u0129a"),
    BAI_CAO(16, "BaiCao", "B\u00e0i c\u00e0o"),
    POKER(17, "Poker", "Poker"),
    AVENGERS(18, "SieuAnhHung", "Si\u00eau anh h\u00f9ng"),
    MY_NHAN_NGU(19, "MyNhanNgu", "M\u1ef9 nh\u00e2n ng\u01b0"),
    KHO_BAU(20, "KhoBau", "Kho b\u00e1u"),
    NU_DIEP_VIEN(21, "NuDiepVien", "N\u1eef \u0111i\u1ec7p vi\u00ean"),
    VUONG_QUOC_VIN(22, "VuongQuocVin", "V\u01b0\u01a1ng Qu\u1ed1c Vin"),
    XI_DZACH(23, "XiDzach", "X\u00ec D\u00e1ch"),
    CARO(24, "Caro", "C\u1edd Caro"),
    CO_TUONG(25, "CoTuong", "C\u1edd T\u01b0\u1edbng"),
    CO_VUA(26, "CoVua", "C\u1edd Vua"),
    POKER_TOUR(27, "PokerTour", "Poker Tour"),
    CO_UP(28, "CoUp", "C\u1edd \u00dap"),
    HAM_CA_MAP(29, "BanCa", "H\u00e0m C\u00e1 M\u1eadp"),
    TAI_XIU_SICBO(30, "TaiXiuSicbo", "T\u00e0i X\u1ec9u Sicbo"),
    TAI_XIU_LIVE(31, "TaiXiuLive", "T\u00e0i X\u1ec9u Live"),
    LIVE_CASINO(32, "LiveCasino", "Live Casino"),
    TAI_XIU_MD5(33, "TaiXiuMD5", "T\u00e0i X\u1ec9u MD5"),
    CANDY(34, "Candy", "Candy"),
    LIVE_GAME(35, "LiveGame", "Live Game"),
    OVER_UNDER(36, "OverUnder", "Over Under"),
    SPARTAN(37, "Spartan", "Th\u1ea7n T\u00e0i"),
    AUDITION(38, "Audition", "T\u00e2y Du"),
    SAMTRUYEN(39, "SamTruyen", "S\u00e2m Truy\u1ec1n"),
    RANGE_ROVER(40, "RangeRover", "Range Rover"),
    MAYBACH(41, "MayBach", "MayBach"),
    TAMHUNG(42, "TamHung", "Ng\u0169 Long"),
    BENLEY(43, "Benley", "Benley"),
    ROLL_ROYE(44, "RollRoyce", "Roll Royce"),
    NHIEM_VU(45, "NhiemVu", "Nhi\u1ec7m V\u1ee5"),
    GIFT_CODE(46, "GiftCode", "Gift Code"),
    AG_GAMES(47, "AGGames", "AG Games"),
    WM_GAMES(48, "WMGames", "WM Games"),
    IBC2_GAMES(49, "IBC2Games", "IBC2 Games"),
    HOAN_TRA(50, "HoanTra", "Ho\u00e0n Tr\u1ea3"),
    KHUYEN_MAI(51, "KhuyenMai", "Khuy\u1ebfn M\u00e3i"),
    VERIFY_PHONE(52, "VerifyPhone", "X\u00e1c th\u1ef1c SĐT"),
    CMD_GAMES(53, "CMDGames", "CMD Games"),
    TAI_XIU_ST(54, "TaiXiuST", "T\u00e0i X\u1ec9u Si\u00eau T\u1ed1c"),
    CHIEM_TINH(55, "ChiemTinh", "Chi\u00eam Tinh"),
    SHOT_FISH(56, "ShotFish", "B\u1eafn C\u00e1"),
    MOON_NIGHT(57, "MoonNight", "Moon Night"),
    EBET_GAMES(58, "EbetGames", "Ebet Games"),
    SBO_GAMES(59, "SBOGames", "SBO Games"),
    SEXYGIRL(60, "SexyGirl", "Sexy Girl"),
    BIKINI(61, "Bikini", "Bikini"),
    GALAXY(62, "Galaxy", "Galaxy"),
    DIEM_DANH(63, "DiemDanh", "\u0110i\u1ec3m Danh"),
    AGENT_TOPUP(64, "AgentTopup", "Agent Topup"),
    LUCKY_MONEY(65, "LuckyMoney", "Lucky Money"),
    USER_WAGES(66, "UserWages", "User Wages"),
    LODE(67, "LoDe", "L\u00f4 \u0110\u1ec1"),
    // --- Source-only values (unique IDs above precompiled range 0-67) ---
    CHIEMTINH(68, "ChiemTinh", "Th\u1ea7n Th\u00fa"),
    THETHAO(69, "TheThao", "Th\u1ee7y Cung"),
    THANBAI(70, "ThanBai", "The Witcher"),
    SIC_BO(71, "SicBo", "Sic Bo"),
    THAN_DEN(72, "ThanDen", "Th\u1ea7n \u0110\u00e8n"),
    DRAGONBALL(73, "DragonBall", "DragonBall"),
    SLOT(100, "Slot", "Slot");

    private int id;
    private String name;
    private String description;

    private Games(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Games findGameById(int id) {
        for (Games entry : Games.values()) {
            if (entry.getId() != id) continue;
            return entry;
        }
        return null;
    }

    public static Games findGameByName(String name) {
        for (Games entry : Games.values()) {
            if (!entry.getName().equalsIgnoreCase(name)) continue;
            return entry;
        }
        return null;
    }

    public static String getGameNameById(int id) {
        for (Games entry : Games.values()) {
            if (entry.getId() == id) {
                return entry.getName();
            }
        }
        return "";
    }
}
