package game.fishing.server.Object;

public enum BulletEffect {
    Other(-1),
    Laser(-2),
    Electric(-3),
    Boom(-4),
    Frozen(-128);

    private int type;

    BulletEffect(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
