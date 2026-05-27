package game.fishing.server.Object;

import java.util.Arrays;

public class FishData {

    public long nFishKey = 0;
    public int unCreateTime = 0;
    public String wHitChair = "";
    public int nFishType = 0;
    public int nFishState = 0;
    public int nFishScore = 0;
    public boolean bRepeatCreate = false;
    public boolean bFlockKill = false;
    public float fRotateAngle = 0;
    public CPoint PointOffSet;
    public float fInitalAngle = 0;
    public int nBezierCount = 0;
    public TagBezierPoint[] TBzierPoint;
    public long lCreateTime = 0;
    public boolean isFishGroup = false;

    public class CDoublePoint {
        private double x;
        private double y;

        public CDoublePoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

    }

    public long getlCreateTime() {
        return lCreateTime;
    }

    public void setlCreateTime(long lCreateTime) {
        this.lCreateTime = lCreateTime;
    }

    public long getnFishKey() {
        return nFishKey;
    }

    public void setnFishKey(long nFishKey) {
        this.nFishKey = nFishKey;
    }

    public int getUnCreateTime() {
        return unCreateTime;
    }

    public void setUnCreateTime(int unCreateTime) {
        this.unCreateTime = unCreateTime;
    }

    public String getwHitChair() {
        return wHitChair;
    }

    public void setwHitChair(String wHitChair) {
        this.wHitChair = wHitChair;
    }

    public int getnFishType() {
        return nFishType;
    }

    public void setnFishType(int nFishType) {
        this.nFishType = nFishType;
    }

    public int getnFishState() {
        return nFishState;
    }

    public void setnFishState(int nFishState) {
        this.nFishState = nFishState;
    }

    public int getnFishScore() {
        return nFishScore;
    }

    public void setnFishScore(int nFishScore) {
        this.nFishScore = nFishScore;
    }

    public boolean isbRepeatCreate() {
        return bRepeatCreate;
    }

    public void setbRepeatCreate(boolean bRepeatCreate) {
        this.bRepeatCreate = bRepeatCreate;
    }

    public boolean isbFlockKill() {
        return bFlockKill;
    }

    public void setbFlockKill(boolean bFlockKill) {
        this.bFlockKill = bFlockKill;
    }

    public float getfRotateAngle() {
        return fRotateAngle;
    }

    public void setfRotateAngle(float fRotateAngle) {
        this.fRotateAngle = fRotateAngle;
    }

    public CPoint getPointOffSet() {
        return PointOffSet;
    }

    public void setPointOffSet(CPoint pointOffSet) {
        this.PointOffSet = pointOffSet;
    }

    public float getfInitalAngle() {
        return fInitalAngle;
    }

    public void setfInitalAngle(float fInitalAngle) {
        this.fInitalAngle = fInitalAngle;
    }

    public int getnBezierCount() {
        return nBezierCount;
    }

    public void setnBezierCount(int nBezierCount) {
        this.nBezierCount = nBezierCount;
    }

    public TagBezierPoint[] getTBzierPoint() {
        return TBzierPoint;
    }

    public void setTBzierPoint(TagBezierPoint[] TBzierPoint) {
        this.TBzierPoint = TBzierPoint;
    }

    @Override
    public String toString() {
        return "{" +
                "\"nFishKey\":" + nFishKey +
                ",\"unCreateTime\":" + unCreateTime +
                ",\"wHitChair\":\"" + wHitChair + "\"" +
                ",\"nFishType\":" + nFishType +
                ",\"nFishState\":" + nFishState +
                ",\"nFishScore\":" + nFishScore +
                ",\"bRepeatCreate\":" + bRepeatCreate +
                ",\"bFlockKill\":" + bFlockKill +
                ",\"fRotateAngle\":" + fRotateAngle +
                ",\"PointOffSet\":" + PointOffSet +
                ",\"fInitalAngle\":" + fInitalAngle +
                ",\"nBezierCount\":" + nBezierCount +
                ",\"TBzierPoint\":" + Arrays.toString(TBzierPoint) +
                '}';
    }

}

