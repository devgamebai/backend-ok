package game.fishing.server.Object;

public class FishBossObject {
    private long nFishKey;
    private int nFishType;

    public FishBossObject() {
    }

    public FishBossObject(int nFishKey, int nFishType) {
        this.nFishKey = nFishKey;
        this.nFishType = nFishType;
    }

    public long getnFishKey() {
        return nFishKey;
    }

    public void setnFishKey(long nFishKey) {
        this.nFishKey = nFishKey;
    }

    public int getnFishType() {
        return nFishType;
    }

    public void setnFishType(int nFishType) {
        this.nFishType = nFishType;
    }
}
