package game.fishing.server.Object;

public class Bullet {
    private int index;
    private int uId;
    private int mutipleIndex;

    public Bullet(int index, int uId, int mutipleIndex) {
        this.index = index;
        this.uId = uId;
        this.mutipleIndex = mutipleIndex;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getuId() {
        return uId;
    }

    public void setuId(int uId) {
        this.uId = uId;
    }

    public int getMutipleIndex() {
        return mutipleIndex;
    }

    public void setMutipleIndex(int mutipleIndex) {
        this.mutipleIndex = mutipleIndex;
    }

    @Override
    public String toString() {
        return "Bullet{" +
                "index=" + index +
                ", uId=" + uId +
                ", mutipleIndex=" + mutipleIndex +
                '}';
    }
}
