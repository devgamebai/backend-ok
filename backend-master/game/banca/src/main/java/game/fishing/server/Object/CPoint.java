package game.fishing.server.Object;

public class CPoint {
    public double x;
    public double y;

    public CPoint(double x, double y) {
        this.x = x;
        this.y = y;

    }

    public CPoint(CPoint p) {
        this.x =  p.x;
        this.y =  p.y;
    }

    public CPoint addPoint(double value) {
        this.x += value;
        this.y += value;
        return this;
    }

    public void addX(double value) {
        this.x += value;
    }

    public void addY(double value) {
        this.y += value;
    }

    @Override
    public String toString() {
        return "{\"x\":" + x + ",\"y\":" + y + "}";
    }


}
