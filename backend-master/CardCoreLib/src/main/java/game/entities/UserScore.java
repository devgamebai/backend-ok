/*
 * Decompiled with CFR 0_116.
 */
package game.entities;

public class UserScore {
    public int moneyType = -1;
    public long money;
    public long wastedMoney;
    public int winCount;
    public int lostCount;
    public String nick_name;
    public int gameId;

    public UserScore clone() {
        UserScore score = new UserScore();
        score.moneyType = this.moneyType;
        score.money = this.money;
        score.wastedMoney = this.wastedMoney;
        score.winCount = this.winCount;
        score.lostCount = this.lostCount;
        return score;
    }
}

