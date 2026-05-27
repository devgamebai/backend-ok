package game.modules.slot.entities.slot.thanden;

public enum ThanDenItem {
    A("A", (byte)0),
    B("B", (byte)1),
    C("C", (byte)2),
    D("D", (byte)3),
    E("E", (byte)4),
    F("F", (byte)5),
    G("G", (byte)6),
    H("H", (byte)7),
    WILD("WILD", (byte)8),
    BONUS("BONUS", (byte)9),
    SCATTER("SCATTER", (byte)10);

    private String name;
    private byte id;

    private ThanDenItem(String name, byte id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getId() {
        return this.id;
    }

    public void setId(byte id) {
        this.id = id;
    }

    public static ThanDenItem findItem(byte id) {
        for (ThanDenItem entry : ThanDenItem.values()) {
            if (entry.getId() == id) {
                return entry;
            }
        }
        return null;
    }
}
