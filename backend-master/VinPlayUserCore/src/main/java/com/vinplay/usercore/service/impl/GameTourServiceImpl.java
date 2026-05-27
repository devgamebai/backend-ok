/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.gencode.CodeGenerator
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  com.vinplay.vbee.common.utils.MapUtils
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dichvuthe.utils.DvtUtils;
import com.vinplay.gamebai.entities.GameFreeCodeDetail;
import com.vinplay.gamebai.entities.GameFreeCodePackage;
import com.vinplay.gamebai.entities.GameFreeCodeStatistic;
import com.vinplay.gamebai.entities.PokerFreeTicket;
import com.vinplay.gamebai.entities.PokerFreeTicketResponse;
import com.vinplay.gamebai.entities.PokerFreeTicketStatistic;
import com.vinplay.gamebai.entities.PokerTicketCount;
import com.vinplay.gamebai.entities.PokerTourFreeCreateType;
import com.vinplay.gamebai.entities.PokerTourInfo;
import com.vinplay.gamebai.entities.PokerTourInfoDetail;
import com.vinplay.gamebai.entities.PokerTourInfoGeneral;
import com.vinplay.gamebai.entities.PokerTourPlayer;
import com.vinplay.gamebai.entities.PokerTourState;
import com.vinplay.gamebai.entities.PokerTourType;
import com.vinplay.gamebai.entities.TopTourModel;
import com.vinplay.gamebai.entities.UserTourModel;
import com.vinplay.gamebai.entities.VipTourModel;
import com.vinplay.usercore.dao.GameTourDao;
import com.vinplay.usercore.dao.impl.GameTourDaoImpl;
import com.vinplay.usercore.service.GameTourService;
import com.vinplay.vbee.common.gencode.CodeGenerator;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.gamebai.LogNoHuGameBaiMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import com.vinplay.vbee.common.utils.MapUtils;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class GameTourServiceImpl
implements GameTourService {
    private static final Logger logger = Logger.getLogger((String)"user_core");
    private GameTourDao gtDao = new GameTourDaoImpl();

    @Override
    public boolean updateMark(String nickname, String gamename, String tourId, int fee, Calendar tourStartTime, int mark, int top, int userTotal, int status, String prize) throws SQLException, ParseException {
        boolean res = this.gtDao.updateMark(nickname, gamename, tourId, fee, tourStartTime, mark, top, userTotal, status, prize);
        logger.debug(("updateMark: " + gamename + " " + nickname + " " + tourId + " " + fee + " " + VinPlayUtils.getDateTimeStr((Date)tourStartTime.getTime()) + " " + mark + " " + top + " " + userTotal + " " + status + " " + prize));
        return res;
    }

    @Override
    public List<VipTourModel> getVips(String gamename, String vipTourId) throws SQLException {
        return this.gtDao.getVips(vipTourId, gamename);
    }

    @Override
    public boolean saveVipTour(String gamename, String vipTourId, Calendar startTime, Calendar endTime, int maxTour, int size, String value) throws SQLException, ParseException {
        boolean res = false;
        List<TopTourModel> topTours = this.getTop(gamename, startTime, endTime, maxTour, size);
        if (topTours.size() > 0) {
            res = this.gtDao.saveVipTour(topTours, gamename, vipTourId, value);
        }
        return res;
    }

    @Override
    public boolean updateMoneyJackpot(String key, long money, String des) throws SQLException {
        boolean res = this.gtDao.updateMoneyJackpot(key, money);
        logger.debug(("updateMoneyJackpot: " + key + " " + money + " " + des));
        return res;
    }

    @Override
    public long getMoneyJackPot(String key) throws SQLException {
        return this.gtDao.getMoneyJackPot(key);
    }

    @Override
    public String getString(String key) throws SQLException {
        return this.gtDao.getString(key);
    }

    @Override
    public boolean saveString(String key, String value) throws SQLException {
        return this.gtDao.saveString(key, value);
    }

    @Override
    public List<TopTourModel> getTop(String gamename, Calendar startTime, Calendar endTime, int maxTour, int size) throws SQLException, ParseException {
        String et;
        ArrayList<TopTourModel> res = new ArrayList<TopTourModel>();
        String st = VinPlayUtils.getDateTimeStr((Date)startTime.getTime());
        Object[] arr = this.gtDao.getUserTours(gamename, st, et = VinPlayUtils.getDateTimeStr((Date)endTime.getTime()), maxTour);
        if (arr != null) {
            Map mapVipTour = (Map)arr[0];
            Map<String, Long> mapSorted = MapUtils.sortMapByValue2((HashMap)((HashMap)arr[1]));
            int stt = 0;
            for (Map.Entry entry : mapSorted.entrySet()) {
                if (stt >= size) break;
                TopTourModel model = (TopTourModel)mapVipTour.get(entry.getKey());
                model.setStt(++stt);
                res.add(model);
            }
        }
        return res;
    }

    @Override
    public List<UserTourModel> getLogUserTour(String gamename, String nickname, int rowStart, int skip) throws SQLException, ParseException {
        return this.gtDao.getLogUserTour(gamename, nickname, rowStart, skip);
    }

    @Override
    public PokerTourInfo createPokerTour(PokerTourInfo tourInfo) throws ParseException, SQLException {
        if (tourInfo != null) {
            return this.gtDao.createPokerTour(tourInfo);
        }
        return null;
    }

    @Override
    public PokerTourInfo getPokerTour(int tourId) throws ParseException, SQLException {
        return this.gtDao.getPokerTour(tourId);
    }

    @Override
    public boolean updatePokerTour(PokerTourInfo tourInfo) throws ParseException, SQLException {
        return tourInfo != null && this.gtDao.updatePokerTour(tourInfo);
    }

    @Override
    public boolean updatePokerTourPlayer(PokerTourPlayer player) throws ParseException, SQLException {
        return player != null && player.nickname != null && this.gtDao.updatePokerTourPlayer(player);
    }

    @Override
    public PokerTourPlayer getPokerTourPlayer(int tourId, String nickname) throws ParseException, SQLException {
        if (nickname != null && !nickname.isEmpty()) {
            return this.gtDao.getPokerTourPlayer(tourId, nickname);
        }
        return null;
    }

    @Override
    public List<PokerTourInfo> getPokerTourList(Calendar startTime, Calendar endTime, PokerTourType type) throws ParseException, SQLException {
        if (startTime != null && endTime != null) {
            return this.gtDao.getPokerTourList(startTime, endTime, type);
        }
        return new ArrayList<PokerTourInfo>();
    }

    @Override
    public List<PokerTourPlayer> getPokerTourPlayers(int tourId) throws ParseException, SQLException {
        return this.gtDao.getPokerTourPlayers(tourId);
    }

    @Override
    public boolean logJackPotPokerTour(String nickname, int betValue, long valuePotBefore, long money, String desc, String tourId) throws IOException, TimeoutException, InterruptedException {
        if (nickname != null && !nickname.isEmpty()) {
            LogNoHuGameBaiMessage nohuMessage = new LogNoHuGameBaiMessage(nickname, betValue, valuePotBefore, money, "PokerTour", desc, tourId);
            MessageBusFactory.get("queue_hu_gamebai").publish((String)"queue_hu_gamebai", (BaseMessage)nohuMessage, (int)402);
            return true;
        }
        return false;
    }

    @Override
    public PokerFreeTicket createPokerFreeTicket(PokerFreeTicket freeTicket) throws ParseException, SQLException {
        if (freeTicket != null && freeTicket.nickname != null && freeTicket.tourType != null && freeTicket.availableTime != null && freeTicket.createType != null) {
            String startTime = null;
            String endTime = null;
            boolean isCanCreateFree = true;
            if (freeTicket.tourType == PokerTourType.Daily) {
                startTime = DateTimeUtils.getStartTimeToDay();
                endTime = DateTimeUtils.getEndTimeToDay();
            } else if (freeTicket.tourType == PokerTourType.Vip) {
                startTime = DateTimeUtils.getStartTimeThisWeek();
                endTime = DateTimeUtils.getEndTimeThisWeek();
                isCanCreateFree = this.gtDao.canCreateFreeTicket(freeTicket.nickname, freeTicket.tourType, startTime, endTime);
            }
            if (isCanCreateFree) {
                return this.gtDao.createPokerFreeTicket(freeTicket);
            }
        }
        return null;
    }

    @Override
    public PokerFreeTicket usePokerFreeTicket(String nickname, int ticket, PokerTourType tourType, int tourId) throws ParseException, SQLException {
        if (nickname != null && tourType != null) {
            return this.gtDao.usePokerFreeTicket(nickname, ticket, tourType, tourId);
        }
        return null;
    }

    @Override
    public List<PokerTourInfoGeneral> getPokerTourListGeneral(Integer ticket, PokerTourState state, PokerTourType type, int page, int rows) throws ParseException, SQLException {
        return this.gtDao.getPokerTourListGeneral(ticket, state, type, page, rows);
    }

    @Override
    public PokerTourInfoDetail getPokerTourDetail(int tourId) throws ParseException, SQLException {
        return this.gtDao.getPokerTourDetail(tourId);
    }

    @Override
    public List<PokerTicketCount> getPokerFreeTicket(String nickname) throws ParseException, SQLException {
        return this.gtDao.getPokerFreeTicket(nickname);
    }

    @Override
    public PokerFreeTicketStatistic getPokerFreeTicketStatistic(int id, String nickname, int createType, int amount, int status, int isBot, int isUse, int tourId, int tourType, String startTime, String endTime, int page, int rows) throws ParseException, SQLException {
        return this.gtDao.getPokerFreeTicketStatistic(id, nickname, createType, amount, status, isBot, isUse, tourId, tourType, startTime, endTime, page, rows);
    }

    @Override
    public int exportFreeCode(String gamename, int quantity, int amount, int codeType, Calendar expire, String creater) throws ParseException, SQLException {
        int length = 10;
        String prefix = "";
        String postfix = "";
        switch (gamename) {
            case "PokerTour": {
                prefix = "PKT";
            }
        }
        Set codes = CodeGenerator.genCode((int)quantity, (int)10, (String)prefix, (String)"");
        return this.gtDao.exportFreeCode(gamename, quantity, amount, codeType, expire, creater, codes);
    }

    @Override
    public List<GameFreeCodePackage> getFreeCodePackage(int id, String gamename, int amount, int codeType, String startTime, String endTime, String creater) throws ParseException, SQLException {
        return this.gtDao.getFreeCodePackage(id, gamename, amount, codeType, startTime, endTime, creater);
    }

    @Override
    public List<GameFreeCodeDetail> getFreeCodeDetails(int id, String code, int packageId, String gamename, int amount, int codeType, int status, String nickname, String addInfo, String startTime, String endTime, int timeType) throws ParseException, SQLException {
        return this.gtDao.getFreeCodeDetails(id, code, packageId, gamename, amount, codeType, status, nickname, addInfo, startTime, endTime, timeType);
    }

    @Override
    public Map<Integer, GameFreeCodeStatistic> getFreeCodeStatistic(String gamename, String startTime, String endTime, int timeType) throws ParseException, SQLException {
        return this.gtDao.getFreeCodeStatistic(gamename, startTime, endTime, timeType);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public PokerFreeTicketResponse getTicketFromFreeCode(String nickname, String code) throws ParseException, SQLException {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        PokerFreeTicket ticket = null;
        int errorCode = 1;
        int timeBlock = 0;
        int fail = 0;
        GameFreeCodeDetail freeCode = this.gtDao.getFreeCodeDetail(code, "PokerTour");
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey(nickname)) {
            try {
                 userMap.lock(nickname);
                UserCacheModel user = (UserCacheModel)userMap.get(nickname);
                long time = DvtUtils.checkCodeGameFail(user.getCodeGameFail(), user.getCodeGameFailTime());
                if (time <= 0L) {
                    if (freeCode != null) {
                        switch (freeCode.status) {
                            case 0: {
                                errorCode = 3;
                                break;
                            }
                            case 1: {
                                if (freeCode.expire != null && freeCode.expire.getTime().getTime() < new Date().getTime()) {
                                    errorCode = 4;
                                    break;
                                }
                                ticket = this.gtDao.getTicketFromFreeCode(code, nickname, freeCode, user.isBot());
                                errorCode = 0;
                                break;
                            }
                            case 2: {
                                errorCode = 5;
                                break;
                            }
                            case 3: {
                                errorCode = 6;
                            }
                        }
                    } else {
                        errorCode = 2;
                    }
                    if (errorCode != 0) {
                        user.setCodeGameFail(user.getCodeGameFail() + 1);
                        user.setCodeGameFailTime(new Date());
                    } else {
                        user.setCodeGameFail(0);
                    }
                    userMap.put(nickname, user);
                } else {
                    errorCode = 7;
                    timeBlock = (int)time;
                }
                fail = user.getCodeGameFail();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                 userMap.unlock(nickname);
            }
        }
        return new PokerFreeTicketResponse(ticket, errorCode, timeBlock, fail);
    }

    @Override
    public boolean updateFreeCodeDetail(int status, String gamename, int id, String code, int packageId, int type, int amount, List<Integer> statusW) throws ParseException, SQLException {
        return this.gtDao.updateFreeCodeDetail(status, gamename, id, code, packageId, type, amount, statusW);
    }

    @Override
    public boolean clearUserInTour(int tourId) throws SQLException {
        return this.gtDao.clearUserInTour(tourId);
    }
}

