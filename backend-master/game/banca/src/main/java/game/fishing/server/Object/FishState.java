package game.fishing.server.Object;

public enum FishState {
   Normal(0),
   King(1),
   Killer(2),
   Aquatic(3);

    private int state;

    FishState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
