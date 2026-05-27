/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.entities.PlayerInfo
 *  game.utils.GameUtils
 *  game.utils.LoggerUtils
 */
package game.tour.server.logic;

import game.tour.server.GamePlayer;
import game.tour.server.cmd.receive.RevTakeTurn;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import java.util.LinkedList;
import java.util.List;

public class PokerPlayerInfo {
    public boolean registerCall;
    public boolean registerCallAll;
    public boolean registerCheck;
    public boolean registerFold;
    public boolean allIn;
    public boolean fold;
    public long registerMoney;
    public boolean randomRaise;
    public List<Turn> listTurn = new LinkedList<Turn>();
    public long moneyBet = 0L;
    public long totalBet = 0L;
    public long currentMoney = 0L;
    public GamePlayer gamePlayer;

    public PokerPlayerInfo(GamePlayer gp) {
        this.gamePlayer = gp;
        this.clearOutGame();
    }

    public void clearNewRound() {
        this.randomRaise = false;
        this.registerCall = false;
        this.registerCheck = false;
        this.registerCallAll = false;
        this.registerMoney = 0L;
        this.moneyBet = 0L;
    }

    public void clearNewGame() {
        this.randomRaise = false;
        this.registerCall = false;
        this.registerCheck = false;
        this.registerCallAll = false;
        this.registerFold = false;
        this.registerMoney = 0L;
        this.listTurn.clear();
        this.moneyBet = 0L;
        this.totalBet = 0L;
        this.fold = false;
        this.allIn = false;
    }

    public void clearOutGame() {
        this.clearNewGame();
        this.currentMoney = 0L;
    }

    public void registerFold() {
        this.registerFold = true;
    }

    public void registerCheck() {
        this.registerCheck = true;
    }

    public void cancelCheck() {
        this.registerCheck = false;
    }

    public void cancelFold() {
        this.registerFold = false;
    }

    public void registerMoney(long amount) {
        this.registerMoney = amount;
        if (this.moneyBet + this.registerMoney > this.currentMoney) {
            this.registerMoney = this.currentMoney - this.moneyBet;
        }
    }

    public void registerBlindMoney(long amount) {
        this.registerMoney = amount;
        if (this.registerMoney > this.currentMoney) {
            this.registerMoney = this.currentMoney;
        }
    }

    public void allIn() {
        this.registerMoney = this.currentMoney - this.moneyBet;
    }

    public Turn takeTurn(PokerGameInfo potInfo, Round round) {
        boolean flag;
        if (this.randomRaise) {
            long minRaise = potInfo.maxBetMoney * 2L;
            int random = GameUtils.rd.nextInt(10);
            if (random > 5) {
                minRaise += 5L * potInfo.bigBlindMoney;
            }
            this.registerMoney = minRaise;
        }
        if (this.registerCallAll) {
            this.registerMoney = potInfo.maxBetMoney - this.moneyBet;
            if (this.registerMoney == 0L) {
                this.registerCheck = true;
            } else if (this.registerMoney >= this.currentMoney) {
                this.allIn = true;
            }
        }
        if (this.registerCheck) {
            if (this.moneyBet == potInfo.maxBetMoney) {
                return this.check(potInfo, round);
            }
            return this.fold(potInfo, round);
        }
        if (this.registerFold || this.fold) {
            return this.fold(potInfo, round);
        }
        if (this.registerCall) {
            if (this.registerMoney != potInfo.maxBetMoney - this.moneyBet) {
                this.registerMoney = potInfo.maxBetMoney - this.moneyBet;
            }
        } else if (this.allIn || this.registerMoney > this.currentMoney) {
            this.registerMoney = this.currentMoney;
            this.allIn = true;
        }
        boolean bl = flag = this.registerMoney > 0L || this.registerMoney == 0L && this.currentMoney == 0L;
        if (flag && this.registerMoney <= this.currentMoney) {
            return this.take(potInfo, round, false);
        }
        return null;
    }

    public Turn fold(PokerGameInfo potInfo, Round round) {
        Turn turn = Turn.makeTurn(this, 0, 0L);
        round.addTurn(turn);
        this.listTurn.add(turn);
        return turn;
    }

    public Turn check(PokerGameInfo potInfo, Round round) {
        Turn turn = Turn.makeTurn(this, 1, 0L);
        round.addTurn(turn);
        return turn;
    }

    public Turn allIn(PokerGameInfo potInfo, Round round, long newMoneyBet) {
        Turn turn = Turn.makeTurn(this, 4, this.registerMoney);
        round.addTurn(turn);
        this.listTurn.add(turn);
        this.allIn = true;
        potInfo.raiseBlock = false;
        this.moneyBet = newMoneyBet;
        if (this.moneyBet > potInfo.maxBetMoney) {
            potInfo.maxBetMoney = this.moneyBet;
            potInfo.lastRaise = this.moneyBet;
        }
        this.currentMoney = 0L;
        this.registerMoney = 0L;
        return turn;
    }

    public Turn call(PokerGameInfo potInfo, Round round, long newMoneyBet) {
        Turn turn = Turn.makeTurn(this, 2, this.registerMoney);
        round.addTurn(turn);
        this.listTurn.add(turn);
        this.moneyBet = newMoneyBet;
        potInfo.maxBetMoney = newMoneyBet;
        this.currentMoney -= this.registerMoney;
        this.registerMoney = 0L;
        return turn;
    }

    public Turn raise(PokerGameInfo potInfo, Round round, long newMoneyBet) {
        Turn turn = Turn.makeTurn(this, 3, this.registerMoney);
        round.addTurn(turn);
        this.listTurn.add(turn);
        potInfo.lastRaise = this.moneyBet = newMoneyBet;
        potInfo.maxBetMoney = newMoneyBet;
        this.currentMoney -= this.registerMoney;
        this.registerMoney = 0L;
        return turn;
    }

    public Turn take(PokerGameInfo potInfo, Round round, boolean isBlind) {
        boolean flag;
        if (this.registerMoney >= 0L) {
            this.registerMoney = this.chargeMoney(this.registerMoney);
        }
        potInfo.potMoney += this.registerMoney;
        long newMoneyBet = this.moneyBet + this.registerMoney;
        if (this.allIn) {
            return this.allIn(potInfo, round, newMoneyBet);
        }
        if (newMoneyBet == potInfo.maxBetMoney) {
            return this.call(potInfo, round, newMoneyBet);
        }
        boolean bl = flag = isBlind || newMoneyBet >= potInfo.maxBetMoney + potInfo.bigBlindMoney;
        if (!potInfo.raiseBlock && newMoneyBet >= potInfo.maxBetMoney + potInfo.lastRaise && flag) {
            return this.raise(potInfo, round, newMoneyBet);
        }
        return this.fold(potInfo, round);
    }

    public long chargeMoney(long money) {
        if (money < 0L) {
            return 0L;
        }
        if (money >= this.currentMoney) {
            this.allIn = true;
            return this.currentMoney;
        }
        return money;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("player=").append(this.gamePlayer.pInfo.nickName);
        sb.append(";registerFollow=").append(this.registerCall);
        sb.append(";registerFollowAll=").append(this.registerCallAll);
        sb.append(";registerCheck=").append(this.registerCheck);
        sb.append(";registerFold=").append(this.registerFold);
        sb.append(";allIn=").append(this.allIn);
        sb.append(";fold=").append(this.fold);
        sb.append(";registerMoney=").append(this.registerMoney);
        sb.append(";moneyBet=").append(this.moneyBet);
        sb.append(";totalBet=").append(this.totalBet);
        sb.append(";currentMoney=").append(this.currentMoney);
        return sb.toString();
    }

    public boolean register(RevTakeTurn cmd, PokerPlayerInfo info, PokerGameInfo potInfo) {
        LoggerUtils.debug((String)"tour", (Object[])new Object[]{"register", cmd.allIn, cmd.fold, cmd.raise});
        if (info.fold || info.allIn) {
            return false;
        }
        this.registerFold = cmd.fold;
        this.registerCheck = cmd.check;
        this.registerCallAll = cmd.callAll;
        this.registerCall = cmd.follow;
        this.allIn = cmd.allIn;
        if (cmd.raise > 0L) {
            if (cmd.raise + info.moneyBet < potInfo.maxBetMoney + potInfo.lastRaise) {
                return false;
            }
            if (cmd.raise > this.currentMoney) {
                return false;
            }
            this.registerMoney = cmd.raise;
        }
        return true;
    }

    public byte getStatus() {
        if (this.fold) {
            return 1;
        }
        if (this.allIn) {
            return 2;
        }
        return 3;
    }

    public void clearNewTurn() {
        this.registerCall = false;
        this.registerCheck = false;
        this.registerCallAll = false;
    }

    public void register(Turn turn) {
        if (turn.action == 0) {
            this.registerFold = true;
        }
        if (turn.action == 1) {
            this.registerCheck = true;
        }
        if (turn.action == 2) {
            this.registerCallAll = true;
        }
        if (turn.action == 3) {
            this.registerMoney = turn.raiseAmount;
        }
        if (turn.action == 4) {
            this.allIn = true;
        }
    }
}

