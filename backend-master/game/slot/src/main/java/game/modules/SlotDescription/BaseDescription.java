package game.modules.SlotDescription;

public class BaseDescription {
    public byte type;
    public String gameID;

    public BaseDescription(byte type, String gameID){
        this.type = type;
        this.gameID = gameID;
    }
}
