package game.fishing.server.Object;

import java.util.LinkedList;

public class GameSceneOBJ {
    private int cbBackIndex = 0;
    private int lPlayScore = 0;
    private int[] lPlayCurScore = new int[6];
    private int[] lPlayStartScore = new int[6];
    private int lCellScore = 0;
    private int nBulletVelocity = 0;
    private int nBulletCoolingTime = 0;
    private int[] nFishMultiple = new int[27];
    private int nMaxTipsCount = 0;
    private LinkedList<Integer> lBulletConsume = new LinkedList<>();
    private int[] lPlayFishCount = new int[27];
    private int[] nMultipleValue = new int[6];
    private int[] nMultipleIndex = new int[6];
    private boolean bUnlimitedRebound = false;
    private String szBrowseUrl = "";
    private String lIngnetCount = "";

    public GameSceneOBJ() {
    }

    public int getCbBackIndex() {
        return cbBackIndex;
    }

    public void setCbBackIndex(int cbBackIndex) {
        this.cbBackIndex = cbBackIndex;
    }

    public int getlPlayScore() {
        return lPlayScore;
    }

    public void setlPlayScore(int lPlayScore) {
        this.lPlayScore = lPlayScore;
    }

    public int[] getlPlayCurScore() {
        return lPlayCurScore;
    }

    public void setlPlayCurScore(int[] lPlayCurScore) {
        this.lPlayCurScore = lPlayCurScore;
    }

    public int[] getlPlayStartScore() {
        return lPlayStartScore;
    }

    public void setlPlayStartScore(int[] lPlayStartScore) {
        this.lPlayStartScore = lPlayStartScore;
    }

    public int getlCellScore() {
        return lCellScore;
    }

    public void setlCellScore(int lCellScore) {
        this.lCellScore = lCellScore;
    }

    public int getnBulletVelocity() {
        return nBulletVelocity;
    }

    public void setnBulletVelocity(int nBulletVelocity) {
        this.nBulletVelocity = nBulletVelocity;
    }

    public int getnBulletCoolingTime() {
        return nBulletCoolingTime;
    }

    public void setnBulletCoolingTime(int nBulletCoolingTime) {
        this.nBulletCoolingTime = nBulletCoolingTime;
    }

    public int[] getnFishMultiple() {
        return nFishMultiple;
    }

    public void setnFishMultiple(int[] nFishMultiple) {
        this.nFishMultiple = nFishMultiple;
    }

    public int getnMaxTipsCount() {
        return nMaxTipsCount;
    }

    public void setnMaxTipsCount(int nMaxTipsCount) {
        this.nMaxTipsCount = nMaxTipsCount;
    }

    public LinkedList<Integer> getlBulletConsume() {
        return lBulletConsume;
    }

    public void setlBulletConsume(LinkedList<Integer> lBulletConsume) {
        this.lBulletConsume = lBulletConsume;
    }

    public int[] getlPlayFishCount() {
        return lPlayFishCount;
    }

    public void setlPlayFishCount(int[] lPlayFishCount) {
        this.lPlayFishCount = lPlayFishCount;
    }

    public int[] getnMultipleValue() {
        return nMultipleValue;
    }

    public void setnMultipleValue(int[] nMultipleValue) {
        this.nMultipleValue = nMultipleValue;
    }

    public int[] getnMultipleIndex() {
        return nMultipleIndex;
    }

    public void setnMultipleIndex(int[] nMultipleIndex) {
        this.nMultipleIndex = nMultipleIndex;
    }

    public boolean isbUnlimitedRebound() {
        return bUnlimitedRebound;
    }

    public void setbUnlimitedRebound(boolean bUnlimitedRebound) {
        this.bUnlimitedRebound = bUnlimitedRebound;
    }

    public String getSzBrowseUrl() {
        return szBrowseUrl;
    }

    public void setSzBrowseUrl(String szBrowseUrl) {
        this.szBrowseUrl = szBrowseUrl;
    }

    public String getlIngnetCount() {
        return lIngnetCount;
    }

    public void setlIngnetCount(String lIngnetCount) {
        this.lIngnetCount = lIngnetCount;
    }
}
