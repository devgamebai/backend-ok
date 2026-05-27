/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  game.modules.tour.control.TourUserInfo
 *  game.utils.GameUtils
 *  game.utils.LoggerUtils
 */
package game.tour.server.logic.ai;

import game.tour.server.GamePlayer;
import game.tour.server.logic.GroupCard;
import game.tour.server.logic.PokerGameInfo;
import game.tour.server.logic.PokerPlayerInfo;
import game.tour.server.logic.PokerRule;
import game.tour.server.logic.Round;
import game.utils.GameUtils;
import game.utils.LoggerUtils;

public class BotAI {
    public static void decideAction(PokerGameInfo game, GamePlayer gp, Round round) {
        int random;
        PokerPlayerInfo playerInfo = gp.spInfo.pokerInfo;
        if (playerInfo.currentMoney <= game.bigBlindMoney) {
            playerInfo.allIn = true;
            return;
        }
        int roundId = 0;
        if (round != null) {
            roundId = round.roundId;
        }
        if (gp.tourUserInfo.fixRank == 1 && (random = GameUtils.rd.nextInt(10)) >= 7) {
            playerInfo.fold = true;
            return;
        }
        GroupCard max = gp.spInfo.handCards;
        if (roundId > 0) {
            max = PokerRule.findMaxGroup(gp.spInfo.handCards, game.getGroupCardPublic(roundId));
        }
        if (max == null) {
            playerInfo.registerCallAll = true;
            LoggerUtils.debug((String)"tour", (Object[])new Object[]{"decideAction MAX_NULL: isPlaying?", gp.isPlaying(), "username", gp.getUser().getName(), "roundId=", roundId, "Public_Card: ", game.publicCard});
            if (gp.spInfo.handCards == null) {
                LoggerUtils.debug((String)"tour", (Object[])new Object[]{"gp.spInfo.handCards NULL"});
            }
            return;
        }
        int check = BotAI.highCards(max);
        if (check > 0) {
            if (check == 1) {
                int random2 = GameUtils.rd.nextInt(10);
                if (random2 == 0) {
                    playerInfo.allIn = true;
                } else if (random2 < 3) {
                    playerInfo.registerCallAll = true;
                } else if (random2 < 6) {
                    playerInfo.randomRaise = true;
                } else {
                    playerInfo.registerCheck = true;
                }
            } else {
                int random3 = GameUtils.rd.nextInt(10);
                if (random3 <= 5) {
                    playerInfo.allIn = true;
                } else if (random3 < 6) {
                    playerInfo.registerCallAll = true;
                } else {
                    playerInfo.randomRaise = true;
                }
            }
        } else {
            playerInfo.registerCheck = true;
        }
    }

    public static int highCards(GroupCard max) {
        int size = max.cards.size();
        int bo = max.kiemtraBo();
        int maxCard = max.GetMaxNumber();
        if (size == 2) {
            if (bo == 8) {
                return 2;
            }
            return 1;
        }
        if (size == 5) {
            if (bo <= 6) {
                return 2;
            }
            if (bo <= 8) {
                return 1;
            }
        }
        return 0;
    }
}

