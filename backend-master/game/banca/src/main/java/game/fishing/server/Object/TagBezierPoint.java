package game.fishing.server.Object;


public class TagBezierPoint {
    public CPoint BeginPoint;
    public CPoint EndPoint;
    public CPoint KeyOne;
    public CPoint KeyTwo;
    public int Time;

    public TagBezierPoint() {
    }

    public TagBezierPoint(TagBezierPoint tPoint) {
        this.BeginPoint = tPoint.BeginPoint;
        this.EndPoint = tPoint.EndPoint;
        this.KeyOne = tPoint.KeyOne;
        this.KeyTwo = tPoint.KeyTwo;
        this.Time = tPoint.Time;
    }


    public TagBezierPoint(CPoint beginPoint, CPoint endPoint, CPoint keyOne, CPoint keyTwo, int time) {
        BeginPoint = beginPoint;
        EndPoint = endPoint;
        KeyOne = keyOne;
        KeyTwo = keyTwo;
        Time = time;
    }

    public CPoint getBeginPoint() {
        return BeginPoint;
    }

    public void setBeginPoint(CPoint beginPoint) {
        BeginPoint = beginPoint;
    }

    public CPoint getEndPoint() {
        return EndPoint;
    }

    public void setEndPoint(CPoint endPoint) {
        EndPoint = endPoint;
    }

    public CPoint getKeyOne() {
        return KeyOne;
    }

    public void setKeyOne(CPoint keyOne) {
        KeyOne = keyOne;
    }

    public CPoint getKeyTwo() {
        return KeyTwo;
    }

    public void setKeyTwo(CPoint keyTwo) {
        KeyTwo = keyTwo;
    }

    public int getTime() {
        return Time;
    }

    public void setTime(int time) {
        Time = time;
    }

    @Override
    public String toString() {
        return "{" +
                "\"BeginPoint\":" + BeginPoint +
                ", \"EndPoint\":" + EndPoint +
                ", \"KeyOne\":" + KeyOne +
                ", \"KeyTwo\":" + KeyTwo +
                ", \"Time\":" + Time +
                '}';
    }


}
