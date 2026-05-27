package game.modules.SlotDescription;

public class PayDescription extends BaseDescription {
    public long totalbet;
    public long totalPrizes;
    public short result;

    public PayDescription(String gameID, long totalbet, long totalPrizes, short result){
        super((byte) 2,gameID);
        this.totalbet = totalbet;
        this.totalPrizes = totalPrizes;
        this.result = result;
    }
}
