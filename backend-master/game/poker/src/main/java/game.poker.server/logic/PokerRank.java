/*
 * Decompiled with CFR 0.144.
 */
package game.poker.server.logic;

import game.poker.server.logic.GroupCard;
import game.poker.server.logic.PokerRule;

public class PokerRank
implements Comparable {
    public boolean fold;
    public boolean win;
    public int chair;
    public int rank;
    public GroupCard cards;
    public long totalBet;
    public long totalMoneyWin;
    public long totalMoneyLost;
    public long finalMoney;

    public int compareTo(Object o) {
        PokerRank rank = (PokerRank)o;
        if (!this.fold && !rank.fold) {
            int result = PokerRule.soSanhBoBai(rank.cards, this.cards);
            if (result != 0) {
                return result;
            }
            if (this.totalBet < rank.totalBet) {
                return -1;
            }
            if(this.totalBet > rank.totalBet)
                return 1;
        }
        if (this.fold && rank.fold) {
            return 0;
        }
        if (rank.fold) {
            return -1;
        }
        return 1;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("chair=").append(this.chair);
        sb.append(";fold=").append(this.fold);
        sb.append(";rank=").append(this.rank);
        sb.append(";totalBet=").append(this.totalBet);
        sb.append(";totalMoneyWin=").append(this.totalMoneyWin);
        sb.append(";totalMoneyLost=").append(this.totalMoneyLost);
        sb.append(";finalMoney=").append(this.finalMoney);
        sb.append(";cards=").append(this.cards);
        return sb.toString();
    }
}

