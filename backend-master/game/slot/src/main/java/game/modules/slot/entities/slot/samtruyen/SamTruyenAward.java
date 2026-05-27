/*
 * Decompiled with CFR 0.144.
 */
package game.modules.slot.entities.slot.samtruyen;

public enum SamTruyenAward {
    PENTA_SCATTER("PENTA_SCATTER", 0, (byte)1, SamTruyenItem.SCATTER, (byte)5, -3.0f),
    QUADAR_SCATTER("QUADAR_SCATTER", 1, (byte)2, SamTruyenItem.SCATTER, (byte)4, -3.0f),
    TRIPLE_SCATTER("TRIPLE_SCATTER", 2, (byte)3, SamTruyenItem.SCATTER, (byte)3, -3.0f),
    PENTA_BONUS("PENTA_BONUS", 3, (byte)4, SamTruyenItem.BONUS, (byte)5, -2.0f),
    QUADAR_BONUS("QUADAR_BONUS", 4, (byte)5, SamTruyenItem.BONUS, (byte)4, -2.0f),
    TRIPLE_BONUS("TRIPLE_BONUS", 5, (byte)6, SamTruyenItem.BONUS, (byte)3, -2.0f),
    PENTA_WILD("PENTA_WILD", 6, (byte)7, SamTruyenItem.WILD, (byte)5, 1000.0f),
    QUADAR_WILD("QUADAR_WILD", 7, (byte)8, SamTruyenItem.WILD, (byte)4, 80.0f),
    TRIPLE_WILD("TRIPLE_WILD", 8, (byte)9, SamTruyenItem.WILD, (byte)3, 25.0f),
    PENTA_JACK_POT("PENTA_JACK_POT", 9, (byte)10, SamTruyenItem.JACK_POT, (byte)5, -1.0f),
    QUADAR_JACK_POT("QUADAR_JACK_POT", 10, (byte)11, SamTruyenItem.JACK_POT, (byte)4, 100.0f),
    TRIPLE_JACK_POT("TRIPLE_JACK_POT", 11, (byte)12, SamTruyenItem.JACK_POT, (byte)3, 30.0f),
    PENTA_NAM_TAY("PENTA_NAM_TAY", 12, (byte)13, SamTruyenItem.NAM_TAY, (byte)5, 300.0f),
    QUADAR_NAM_TAY("QUADAR_NAM_TAY", 13, (byte)14, SamTruyenItem.NAM_TAY, (byte)4, 50.0f),
    TRIPLE_NAM_TAY("TRIPLE_NAM_TAY", 14, (byte)15, SamTruyenItem.NAM_TAY, (byte)3, 20.0f),
    PENTA_BUA("PENTA_BUA", 15, (byte)16, SamTruyenItem.BUA, (byte)5, 200.0f),
    QUADAR_BUA("QUADAR_BUA", 16, (byte)17, SamTruyenItem.BUA, (byte)4, 30.0f),
    TRIPLE_BUA("TRIPLE_BUA", 17, (byte)18, SamTruyenItem.BUA, (byte)3, 10.0f),
    PENTA_KHIEN("PENTA_KHIEN", 18, (byte)19, SamTruyenItem.KHIEN, (byte)5, 110.0f),
    QUADAR_KHIEN("QUADAR_KHIEN", 19, (byte)20, SamTruyenItem.KHIEN, (byte)4, 20.0f),
    TRIPLE_KHIEN("TRIPLE_KHIEN", 20, (byte)21, SamTruyenItem.KHIEN, (byte)3, 10.0f),
    PENTA_KIM_CUONg("PENTA_KIM_CUONg", 21, (byte)22, SamTruyenItem.KIM_CUONG, (byte)5, 50.0f),
    QUADAR_KIM_CUONg("QUADAR_KIM_CUONg", 22, (byte)23, SamTruyenItem.KIM_CUONG, (byte)4, 10.0f),
    TRIPLE_KIM_CUONg("TRIPLE_KIM_CUONg", 23, (byte)24, SamTruyenItem.KIM_CUONG, (byte)3, 6.0f),
    PENTA_DAI_BANG("PENTA_DAI_BANG", 24, (byte)25, SamTruyenItem.DAI_BANG, (byte)5, 30.0f),
    QUADAR_DAI_BANG("QUADAR_DAI_BANG", 25, (byte)26, SamTruyenItem.DAI_BANG, (byte)4, 7.0f),
    TRIPLE_DAI_BANG("TRIPLE_DAI_BANG", 26, (byte)27, SamTruyenItem.DAI_BANG, (byte)3, 5.0f),
    PENTA_NGUOI_NHEN("PENTA_NGUOI_NHEN", 27, (byte)28, SamTruyenItem.NGUOI_NHEN, (byte)5, 20.0f),
    QUADAR_NGUOI_NHEN("QUADAR_NGUOI_NHEN", 28, (byte)29, SamTruyenItem.NGUOI_NHEN, (byte)4, 6.0f),
    TRIPLE_NGUOI_NHEN("TRIPLE_NGUOI_NHEN", 29, (byte)30, SamTruyenItem.NGUOI_NHEN, (byte)3, 4.0f),
    PENTA_RADAR("PENTA_RADAR", 30, (byte)31, SamTruyenItem.RADAR, (byte)5, 11.0f),
    QUADAR_RADAR("QUADAR_RADAR", 31, (byte)32, SamTruyenItem.RADAR, (byte)4, 5.0f),
    TRIPLE_RADAR("TRIPLE_RADAR", 32, (byte)33, SamTruyenItem.RADAR, (byte)3, 3.0f);
    
    private byte id;
    private SamTruyenItem item;
    private byte duplicate;
    private float ratio;

    private SamTruyenAward(String s, int n2, byte id, SamTruyenItem item, byte duplicate, float ratio) {
        this.id = id;
        this.item = item;
        this.duplicate = duplicate;
        this.ratio = ratio;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public void setItem(SamTruyenItem item) {
        this.item = item;
    }

    public SamTruyenItem getItem() {
        return this.item;
    }

    public void setDuplicate(byte duplicate) {
        this.duplicate = duplicate;
    }

    public byte getDuplicate() {
        return this.duplicate;
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    public float getRatio() {
        return this.ratio;
    }
}

