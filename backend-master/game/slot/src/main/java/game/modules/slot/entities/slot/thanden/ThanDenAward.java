package game.modules.slot.entities.slot.thanden;

public enum ThanDenAward {
    // Nổ hũ sao (Wild 5x)
    PENTA_WILD("PENTA_WILD", 0, (byte)1, ThanDenItem.WILD, (byte)5, -2.0F, -2.0F),

    // ======================================================
    // BASE GAME PAYTABLE (xlsx Sheet "paytable Pirate" = Thần Đèn)
    //   ratio       → hệ số nhân bet per line (Base Game)
    //   ratioFreeSpin → hệ số nhân bet per line (Free Spin)
    //
    // Symbol mapping:
    //   xlsx "Jackpot" → Java A (ID 0) → 5x = Nổ Hũ Pot
    //   xlsx "A"       → Java B (ID 1)
    //   xlsx "K"       → Java C (ID 2)
    //   xlsx "Q"       → Java D (ID 3)
    //   xlsx "J"       → Java E (ID 4)
    //   xlsx "10"      → Java F (ID 5)
    //   xlsx "9"       → Java G (ID 6)
    //   xlsx "8"       → Java H (ID 7)
    // ======================================================

    // Icon A (ID 0) = "Jackpot" (Base) / Symbol "a" (Free)
    // Base: 2x=5, 3x=50, 4x=200, 5x=Jackpot(Pot)
    // Free: 3x=300, 4x=500, 5x=5000
    PENTA_A ("PENTA_A",  1, (byte)1, ThanDenItem.A, (byte)5, -2.0F,   5000.0F),
    QUADRA_A("QUADRA_A", 2, (byte)1, ThanDenItem.A, (byte)4, 200.0F,   500.0F),
    TRIPLE_A("TRIPLE_A", 3, (byte)1, ThanDenItem.A, (byte)3,  50.0F,   300.0F),
    DOUBLE_A("DOUBLE_A", 4, (byte)1, ThanDenItem.A, (byte)2,   5.0F,     0.0F),

    // Icon B (ID 1) = "A" (Base) / Symbol "b" (Free)
    // Base: 2x=2, 3x=15, 4x=100, 5x=200
    // Free: 3x=200, 4x=250, 5x=300
    PENTA_B ("PENTA_B",  5, (byte)1, ThanDenItem.B, (byte)5, 200.0F,   300.0F),
    QUADRA_B("QUADRA_B", 6, (byte)1, ThanDenItem.B, (byte)4, 100.0F,   250.0F),
    TRIPLE_B("TRIPLE_B", 7, (byte)1, ThanDenItem.B, (byte)3,  15.0F,   200.0F),
    DOUBLE_B("DOUBLE_B", 8, (byte)1, ThanDenItem.B, (byte)2,   2.0F,     0.0F),

    // Icon C (ID 2) = "K" (Base) / Symbol "c" (Free)
    // Base: 2x=2, 3x=10, 4x=55, 5x=150
    // Free: 3x=100, 4x=200, 5x=250
    PENTA_C ("PENTA_C",  9, (byte)1, ThanDenItem.C, (byte)5, 150.0F,   250.0F),
    QUADRA_C("QUADRA_C",10, (byte)1, ThanDenItem.C, (byte)4,  55.0F,   200.0F),
    TRIPLE_C("TRIPLE_C",11, (byte)1, ThanDenItem.C, (byte)3,  10.0F,   100.0F),
    DOUBLE_C("DOUBLE_C",12, (byte)1, ThanDenItem.C, (byte)2,   2.0F,     0.0F),

    // Icon D (ID 3) = "Q" (Base) / Symbol "d" (Free)
    // Base: 2x=2, 3x=10, 4x=40, 5x=100
    // Free: 3x=85, 4x=125, 5x=175
    PENTA_D ("PENTA_D", 13, (byte)1, ThanDenItem.D, (byte)5, 100.0F,   175.0F),
    QUADRA_D("QUADRA_D",14, (byte)1, ThanDenItem.D, (byte)4,  40.0F,   125.0F),
    TRIPLE_D("TRIPLE_D",15, (byte)1, ThanDenItem.D, (byte)3,  10.0F,    85.0F),
    DOUBLE_D("DOUBLE_D",16, (byte)1, ThanDenItem.D, (byte)2,   2.0F,     0.0F),

    // Icon E (ID 4) = "J" (Base) / Symbol "e" (Free)
    // Base: 2x=0, 3x=5, 4x=30, 5x=70
    // Free: 3x=80, 4x=100, 5x=150
    PENTA_E ("PENTA_E", 17, (byte)1, ThanDenItem.E, (byte)5,  70.0F,   150.0F),
    QUADRA_E("QUADRA_E",18, (byte)1, ThanDenItem.E, (byte)4,  30.0F,   100.0F),
    TRIPLE_E("TRIPLE_E",19, (byte)1, ThanDenItem.E, (byte)3,   5.0F,    80.0F),
    DOUBLE_E("DOUBLE_E",20, (byte)1, ThanDenItem.E, (byte)2,   0.0F,     0.0F),

    // Icon F (ID 5) = "10" (Base) / Symbol "f" (Free)
    // Base: 2x=0, 3x=5, 4x=20, 5x=55
    // Free: 3x=50, 4x=80, 5x=100
    PENTA_F ("PENTA_F", 21, (byte)1, ThanDenItem.F, (byte)5,  55.0F,   100.0F),
    QUADRA_F("QUADRA_F",22, (byte)1, ThanDenItem.F, (byte)4,  20.0F,    80.0F),
    TRIPLE_F("TRIPLE_F",23, (byte)1, ThanDenItem.F, (byte)3,   5.0F,    50.0F),
    DOUBLE_F("DOUBLE_F",24, (byte)1, ThanDenItem.F, (byte)2,   0.0F,     0.0F),

    // Icon G (ID 6) = "9" (Base) / Symbol "g" (Free)
    // Base: 2x=0, 3x=3, 4x=15, 5x=40
    // Free: 3x=25, 4x=35, 5x=45
    PENTA_G ("PENTA_G", 25, (byte)1, ThanDenItem.G, (byte)5,  40.0F,    45.0F),
    QUADRA_G("QUADRA_G",26, (byte)1, ThanDenItem.G, (byte)4,  15.0F,    35.0F),
    TRIPLE_G("TRIPLE_G",27, (byte)1, ThanDenItem.G, (byte)3,   3.0F,    25.0F),
    DOUBLE_G("DOUBLE_G",28, (byte)1, ThanDenItem.G, (byte)2,   0.0F,     0.0F),

    // Icon H (ID 7) = "8" (Base) / Symbol "h" (Free)
    // Base: 2x=0, 3x=3, 4x=10, 5x=30
    // Free: 3x=20, 4x=30, 5x=40
    PENTA_H ("PENTA_H", 29, (byte)1, ThanDenItem.H, (byte)5,  30.0F,    40.0F),
    QUADRA_H("QUADRA_H",30, (byte)1, ThanDenItem.H, (byte)4,  10.0F,    30.0F),
    TRIPLE_H("TRIPLE_H",31, (byte)1, ThanDenItem.H, (byte)3,   3.0F,    20.0F),
    DOUBLE_H("DOUBLE_H",32, (byte)1, ThanDenItem.H, (byte)2,   0.0F,     0.0F);

    private byte id;
    private ThanDenItem item;
    private byte duplicate;
    private float ratio;
    private float ratioFreeSpin;

    private ThanDenAward(String s, int n, byte id, ThanDenItem item, byte duplicate, float ratio, float ratioFreeSpin) {
        this.id = id;
        this.item = item;
        this.duplicate = duplicate;
        this.ratio = ratio;
        this.ratioFreeSpin = ratioFreeSpin;
    }

    public byte getId() { return this.id; }
    public ThanDenItem getItem() { return this.item; }
    public byte getDuplicate() { return this.duplicate; }
    public float getRatio() { return this.ratio; }
    public float getRatioFreeSpin() { return this.ratioFreeSpin; }
}
