package game.fishing.server.Object;

public class FishPath {
    private int pathCount = 1;
    private TagBezierPoint point;

    public FishPath(int pathCount, TagBezierPoint point) {
        this.pathCount = pathCount;
        this.point = point;
    }

    public int getPathCount() {
        return pathCount;
    }

    public void setPathCount(int pathCount) {
        this.pathCount = pathCount;
    }

    public TagBezierPoint getPoint() {
        return point;
    }

    public void setPoint(TagBezierPoint point) {
        this.point = point;
    }
}
