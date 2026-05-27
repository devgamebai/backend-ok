/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.common.business.Debug
 *  game.entities.PlayerInfo
 *  game.entities.UserScore
 *  game.modules.gameRoom.entities.GameMoneyInfo
 *  game.modules.gameRoom.entities.GameRoom
 *  game.modules.gameRoom.entities.MoneyException
 *  game.utils.GameUtils
 */
package game.poker.server.logic;

import bitzero.server.entities.User;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.common.business.Debug;
import game.entities.PlayerInfo;
import game.entities.UserScore;
import game.modules.gameRoom.entities.GameMoneyInfo;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.gameRoom.entities.MoneyException;
import game.poker.server.GameManager;
import game.poker.server.GamePlayer;
import game.poker.server.PokerGameServer;
import game.poker.server.cmd.receive.RevTakeTurn;
import game.poker.server.logic.Gamble;
import game.poker.server.logic.PokerGameInfo;
import game.poker.server.logic.Round;
import game.poker.server.logic.Turn;
import game.utils.GameUtils;
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
    public List<Turn> listTurn = new LinkedList<Turn>();
    public long moneyBet = 0L;
    public long totalBet = 0L;
    public long currentMoney = 0L;
    public long lastBuyInMoney = 0L;
    public GamePlayer gamePlayer;

    public PokerPlayerInfo(GamePlayer gp) {
        this.gamePlayer = gp;
        this.clearOutGame();
    }

    public void clearNewRound() {
        this.registerCall = false;
        this.registerCheck = false;
        this.registerCallAll = false;
        this.registerMoney = 0L;
        this.moneyBet = 0L;
    }

    public void clearNewGame() {
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

    public void allIn() {
        this.registerMoney = this.currentMoney - this.moneyBet;
    }

    public Turn takeTurn(PokerGameInfo potInfo, Round round) {
        boolean flag;
        if (this.registerCallAll) {
            this.registerMoney = potInfo.maxBetMoney - this.moneyBet;
            if (this.registerMoney == 0L) {
                this.registerCheck = true;
            } else if (this.registerMoney > this.currentMoney) {
                this.allIn = true;
            }
        }
        if (this.registerCheck) {
            if (this.moneyBet == potInfo.maxBetMoney) {
                Debug.trace((Object[])new Object[]{"Dang ky check", this.moneyBet, potInfo.maxBetMoney});
                return this.check(potInfo, round);
            }
            Debug.trace((Object[])new Object[]{"Dang ky check khong du tien cuoc", this.moneyBet, potInfo.maxBetMoney});
            return null;
        }
        if (this.registerFold || this.fold) {
            return this.fold(potInfo, round);
        }
        if (this.registerCall) {
            if (this.registerMoney != potInfo.maxBetMoney - this.moneyBet) {
                this.registerMoney = potInfo.maxBetMoney - this.moneyBet;
            }
        } else if (this.allIn) {
            this.registerMoney = this.currentMoney;
        }
        boolean bl = flag = this.registerMoney > 0L || this.registerMoney == 0L && this.currentMoney == 0L;
        if (flag && this.registerMoney <= this.currentMoney) {
            return this.take(potInfo, round, false);
        }
        return null;
    }

    public Turn fold(PokerGameInfo potInfo, Round round) {
        Debug.trace((Object[])new Object[]{"fold1: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"fold1: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
        Turn turn = Turn.makeTurn(this, 0, 0L);
        round.addTurn(turn);
        this.listTurn.add(turn);
        Debug.trace((Object[])new Object[]{"fold2: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"fold3: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
        return turn;
    }

    public Turn check(PokerGameInfo potInfo, Round round) {
        Debug.trace((Object)"check");
        Turn turn = Turn.makeTurn(this, 1, 0L);
        round.addTurn(turn);
        return turn;
    }

    public Turn allIn(PokerGameInfo potInfo, Round round, long newMoneyBet) {
        Debug.trace((Object[])new Object[]{"allIn1: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"allIn1: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
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
        Debug.trace((Object[])new Object[]{"allIn2: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"allIn2: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
        return turn;
    }

    public Turn call(PokerGameInfo potInfo, Round round, long newMoneyBet) {
        Debug.trace((Object[])new Object[]{"follow1: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"follow2: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
        Turn turn = Turn.makeTurn(this, 2, this.registerMoney);
        round.addTurn(turn);
        this.listTurn.add(turn);
        this.moneyBet = newMoneyBet;
        potInfo.maxBetMoney = newMoneyBet;
        this.currentMoney -= this.registerMoney;
        this.registerMoney = 0L;
        Debug.trace((Object[])new Object[]{"follow2: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"follow2: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
        return turn;
    }

    public Turn raise(PokerGameInfo potInfo, Round round, long newMoneyBet) {
        Debug.trace((Object[])new Object[]{"raise1: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"raise1: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
        Turn turn = Turn.makeTurn(this, 3, this.registerMoney);
        round.addTurn(turn);
        this.listTurn.add(turn);
        potInfo.lastRaise = this.moneyBet = newMoneyBet;
        potInfo.maxBetMoney = newMoneyBet;
        this.currentMoney -= this.registerMoney;
        this.registerMoney = 0L;
        Debug.trace((Object[])new Object[]{"raise2: PokerInfo=", this.toString()});
        Debug.trace((Object[])new Object[]{"raise2: PotInfo=", GameUtils.toJSONObject((Object)potInfo)});
        return turn;
    }

    public Turn take(PokerGameInfo potInfo, Round round, boolean isBlind) {
        boolean flag;
        long money = this.registerMoney;
        if (this.registerMoney > 0L) {
            this.registerMoney = this.chargeMoney(this.registerMoney);
        }
        potInfo.potMoney += this.registerMoney;
        long newMoneyBet = this.moneyBet + this.registerMoney;
        if (this.registerMoney == 0L && this.currentMoney == 0L) {
            this.allIn = true;
        }
        if ((this.allIn || money != this.registerMoney) && this.registerMoney > 0L) {
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
        if (money <= 0L) {
            return 0L;
        }
        UserScore score = new UserScore();
        score.moneyType = this.gamePlayer.gameMoneyInfo.moneyType;
        score.money = -money;
        try {
            score.money = this.gamePlayer.gameMoneyInfo.chargeMoneyInGame(score, this.gamePlayer.gameServer.room.getId(), this.gamePlayer.gameServer.gameMgr.game.id);
            this.gamePlayer.gameServer.dispatchAddEventScore(this.gamePlayer.getUser(), score);
            return -score.money;
        }
        catch (MoneyException e) {
            CommonHandle.writeErrLog((String)("ERROR WHEN CHARGE MONEY INGAME" + this.gamePlayer.gameMoneyInfo.toString()));
            this.gamePlayer.reqQuitRoom = true;
            return 0L;
        }
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
        if (info.fold || info.allIn) {
            Debug.trace((Object)"Da fold hoac all in truoc do");
            return false;
        }
        this.registerFold = cmd.fold;
        this.registerCheck = cmd.check;
        this.registerCallAll = cmd.callAll;
        this.registerCall = cmd.follow;
        this.allIn = cmd.allIn;
        if (cmd.raise > 0L) {
            if (cmd.raise + info.moneyBet < potInfo.maxBetMoney + potInfo.lastRaise) {
                Debug.trace((Object[])new Object[]{"Vi pham quy tac raise: cmd.raise, info.moneyBet, potInfo.maxBetMoney, potInfo.lastRaise", cmd.raise, info.moneyBet, potInfo.maxBetMoney, potInfo.lastRaise});
                return false;
            }
            if (cmd.raise > this.currentMoney) {
                Debug.trace((Object[])new Object[]{"Raise qua so tien minh co:", cmd.raise, this.currentMoney});
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

