/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  bitzero.server.entities.managers.IUserManager
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.util.ExtensionUtility
 *  bitzero.util.common.business.CommonHandle
 *  com.vinplay.gamebai.entities.PokerFreeTicket
 *  com.vinplay.gamebai.entities.PokerFreeTicketResponse
 *  com.vinplay.gamebai.entities.PokerTourFreeCreateType
 *  com.vinplay.gamebai.entities.PokerTourInfo
 *  com.vinplay.gamebai.entities.PokerTourPlayer
 *  com.vinplay.gamebai.entities.PokerTourState
 *  com.vinplay.gamebai.entities.PokerTourType
 *  com.vinplay.gamebai.entities.TopTourModel
 *  com.vinplay.usercore.service.GameTourService
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.GameTourServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 *  org.json.JSONArray
 *  org.json.JSONObject
 */
package game.modules.tour.control;

import bitzero.server.entities.User;
import bitzero.server.entities.managers.IUserManager;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.util.ExtensionUtility;
import bitzero.util.common.business.CommonHandle;
import com.vinplay.gamebai.entities.PokerFreeTicket;
import com.vinplay.gamebai.entities.PokerFreeTicketResponse;
import com.vinplay.gamebai.entities.PokerTourFreeCreateType;
import com.vinplay.gamebai.entities.PokerTourInfo;
import com.vinplay.gamebai.entities.PokerTourPlayer;
import com.vinplay.gamebai.entities.PokerTourState;
import com.vinplay.gamebai.entities.PokerTourType;
import com.vinplay.gamebai.entities.TopTourModel;
import com.vinplay.usercore.service.GameTourService;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.GameTourServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import game.modules.bot.Bot;
import game.modules.bot.BotManager;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.tour.control.PrizeRate;
import game.modules.tour.control.RoomTourGroup;
import game.modules.tour.control.Tour;
import game.modules.tour.control.TourPrize;
import game.modules.tour.control.TourSetting;
import game.modules.tour.control.TourUserInfo;
import game.modules.tour.control.cmd.send.SendGetListTour;
import game.modules.tour.control.cmd.send.SendGiveJackpot;
import game.modules.tour.control.cmd.send.SendMoneyInfo;
import game.modules.tour.control.cmd.send.SendNewFreeTicket;
import game.modules.tour.control.cmd.send.SendRegisterTour;
import game.modules.tour.control.cmd.send.SendUserTourInfo;
import game.modules.tour.control.config.TourConfig;
import game.modules.tour.log.LogEntry;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class TourManager {
    public int lastDay = 0;
    public long jackpot;
    public List<Tour> dailyTours = new LinkedList<Tour>();
    public Tour vipTour = null;
    private static TourManager ins = null;
    private GameTourService tourService = new GameTourServiceImpl();
    public UserService userService = new UserServiceImpl();
    public Map<String, Integer> userPlayingTour = new ConcurrentHashMap<String, Integer>();
    private List<Tour> playingAndWaitingTours = new LinkedList<Tour>();
    public long currentJackpot = 0;
    public List<TopTourModel> vips = new LinkedList<TopTourModel>();

    public static TourManager instance() {
        if (ins == null) {
            ins = new TourManager();
        }
        return ins;
    }

    private TourManager() {
        try {
            String day = this.tourService.getString("Last_Day_Give_Prize");
            if (day != null) {
                Integer v = Integer.parseInt(day);
                this.lastDay = v;
            } else {
                this.lastDay = -1;
            }
        }
        catch (SQLException e) {
            this.lastDay = Calendar.getInstance().get(7);
            CommonHandle.writeErrLog((Throwable)e);
        }
        this.init();
    }

    public void updateScore(Tour tour, TourUserInfo info, int score, int numberOfPlayer) {
        int status = 1;
        try {
            boolean result;
            LogEntry entry = new LogEntry(LogEntry.ACTION.GIVE_PRIZE);
            if (info.prize > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Trao thuong PokerTour #").append(info.tourId).append(" top ").append(info.rank).append(" score ").append(score);
                MoneyResponse res = TourManager.instance().userService.updateMoney(info.nickName, (long)info.prize, "vin", GameUtils.gameName, "PokerTour", sb.toString(), (long)(info.prize / 95 * 5), Long.valueOf(tour.tourId), TransType.END_TRANS);
                if (!res.isSuccess()) {
                    CommonHandle.writeErrLog((String)sb.toString());
                } else {
                    LoggerUtils.debug("tour", info.nickName, sb.toString());
                }
            }
            if (result = this.tourService.updateMark(info.nickName, GameUtils.gameName, String.valueOf(tour.tourId), info.ticket, tour.startTime, score, info.rank, numberOfPlayer, status, String.valueOf(info.prize))) {
                entry.error = "OK";
                info.score = score;
            } else {
                entry.error = "FAIL";
            }
            this.updatePlayerTour(tour, info);
            entry.initTourInfo(info);
            tour.addLogEntry(entry);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            StringBuilder sb = new StringBuilder();
            sb.append(info.nickName).append("|");
            sb.append(GameUtils.gameName).append("|");
            sb.append(String.valueOf(tour.tourId)).append("|");
            sb.append(info.ticket).append("|");
            sb.append(tour.startTime).append("|");
            sb.append(score).append("|");
            sb.append(info.rank).append("|");
            sb.append(numberOfPlayer).append("|");
            sb.append(status).append("|");
            sb.append(String.valueOf(info.prize)).append("|");
            CommonHandle.writeErrLog((String)sb.toString());
        }
    }

    public Tour restorePlayingTour(PokerTourInfo tourInfo) {
        Tour tour = this.restoreOnScheduleTour(tourInfo);
        tour.tourState = 0;
        Calendar now = Calendar.getInstance();
        now.add(12, 1);
        int hour = now.get(11);
        int minute = now.get(12);
        tour.startDay = now.get(7);
        tour.startHour = hour;
        tour.startMinute = minute;
        return tour;
    }

    public Tour restoreOnScheduleTour(PokerTourInfo tourInfo) {
        Tour tour = null;
        try {
            JSONObject mode = TourConfig.instance().config.getJSONArray("modes").getJSONObject(0);
            TourSetting setting = new TourSetting();
            setting.init(mode);
            tour = new Tour(setting);
            tour.tourId = tourInfo.tourId;
            tour.level = tourInfo.level;
            tour.tourType = tourInfo.tourType == PokerTourType.Daily ? 1 : 2;
            tour.updateTourLevel();
            if (tour.startTime == null) {
                tour.startTime = tourInfo.startTimeSchedule;
            }
            if (tourInfo.startTime == null) {
                tourInfo.startTime = tourInfo.startTimeSchedule;
            }
            if (tourInfo.startTimeSchedule != null) {
                tour.endRegisterTime = tourInfo.endRegisterSchedule;
            }
            tour.startHour = tourInfo.startTime.get(11);
            tour.startMinute = tourInfo.startTime.get(12);
            tour.endRegisterHour = tour.startHour + 1;
            tour.endRegisterMinute = tour.startMinute;
            if (tourInfo.tourState == PokerTourState.Onschedule) {
                int countDown = tour.getCountDownToStart();
                tour.tourState = countDown <= 3600 ? 0 : 5;
            }
            if (tourInfo.tourState == PokerTourState.TourStarted) {
                tour.tourState = 1;
            }
            if (tourInfo.tourState == PokerTourState.RegisterClosed) {
                tour.tourState = 2;
            }
            if (tourInfo.tourState == PokerTourState.TourEnded || tourInfo.tourState == PokerTourState.TourCanceled) {
                tour.tourState = 3;
            }
            List<PokerTourPlayer> players = this.tourService.getPokerTourPlayers(tour.tourId);
            tour.allRegisteredPlayers.clear();
            int count = 0;
            tour.ticket = tourInfo.ticket;
            for (PokerTourPlayer player : players) {
                TourUserInfo tourUserInfo = new TourUserInfo();
                tourUserInfo.chip = player.currentChip;
                tourUserInfo.lastChip = player.lastChip;
                tourUserInfo.nickName = player.nickname;
                tourUserInfo.tourId = player.tourId;
                tourUserInfo.ticket = tour.ticket;
                tourUserInfo.timeBuyTicket = player.ticketCount;
                count += player.ticketCount;
                tourUserInfo.timeOutTour = player.outTourCount;
                tourUserInfo.outTourTimeStamp = player.outTourTimestamp;
                tour.allRegisteredPlayers.add(tourUserInfo);
            }
            tour.countTimeUpLevel = tourInfo.countTimeUpLevel;
            if (tour.tourType == 1) {
                tour.prizePool = count * tour.ticket;
            } else {
                tour.prizePool = count * tour.ticket;
                if (tour.tourType == 2 && tour.prizePool < TourConfig.instance().getMinVipPrizePool()) {
                    tour.prizePool = TourConfig.instance().getMinVipPrizePool();
                }
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        return tour;
    }

    public void initDailyTour() {
        Calendar now = Calendar.getInstance();
        int today = now.get(7);
        now.set(11, 0);
        now.set(12, 0);
        now.set(13, 0);
        Calendar endDay = Calendar.getInstance();
        endDay.set(11, 23);
        endDay.set(12, 59);
        endDay.set(13, 59);
        List<PokerTourInfo> pokerTours = null;
        try {
            pokerTours = this.tourService.getPokerTourList(now, endDay, PokerTourType.Daily);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        boolean initNewDailyTour = true;
        if (pokerTours != null && pokerTours.size() != 0) {
            for (PokerTourInfo tourInfo : pokerTours) {
                Tour tour;
                int startDay = tourInfo.startTimeSchedule.get(7);
                if (tourInfo.tourState == PokerTourState.TourStarted || tourInfo.tourState == PokerTourState.RegisterClosed) {
                    tour = this.restorePlayingTour(tourInfo);
                    tour.startDay = startDay;
                    this.dailyTours.add(tour);
                    tour.init();
                }
                if (tourInfo.tourState == PokerTourState.Onschedule) {
                    tour = this.restoreOnScheduleTour(tourInfo);
                    tour.startDay = startDay;
                    if (startDay == today) {
                        tour.init();
                        this.dailyTours.add(tour);
                    } else {
                        tour.tourState = 4;
                        tour.cancelTime = Calendar.getInstance();
                        this.updateTour(tour);
                    }
                }
                if (startDay != today) continue;
                initNewDailyTour = false;
            }
        }
        if (initNewDailyTour) {
            try {
                JSONObject mode = TourConfig.instance().config.getJSONArray("modes").getJSONObject(0);
                JSONArray arr = TourConfig.instance().config.getJSONArray("tours");
                for (int i = 0; i < arr.length(); ++i) {
                    JSONObject json = arr.getJSONObject(i);
                    TourSetting setting = new TourSetting();
                    setting.init(mode);
                    Tour tour = new Tour(setting);
                    tour.startHour = json.getJSONObject("start").getInt("h");
                    tour.startMinute = json.getJSONObject("start").getInt("m");
                    tour.endRegisterHour = json.getJSONObject("endRegister").getInt("h");
                    tour.endRegisterMinute = json.getJSONObject("endRegister").getInt("m");
                    tour.ticket = json.getInt("ticket");
                    Calendar start = Calendar.getInstance();
                    start.set(11, tour.startHour);
                    start.set(12, tour.startMinute);
                    start.set(13, 0);
                    tour.startDay = start.get(7);
                    PokerTourInfo info = new PokerTourInfo();
                    info.startTimeSchedule = start;
                    info.tourType = PokerTourType.Daily;
                    info.ticket = tour.ticket;
                    info.tourState = PokerTourState.Onschedule;
                    int countDown = tour.getCountDownToStart();
                    tour.tourState = countDown <= 3600 ? 0 : 5;
                    PokerTourInfo tourInfo = this.tourService.createPokerTour(info);
                    if (tourInfo == null) continue;
                    tour.tourId = tourInfo.tourId;
                    tour.tourType = 1;
                    tour.startDay = today;
                    this.dailyTours.add(tour);
                    tour.init();
                }
            }
            catch (Exception e) {
                CommonHandle.writeErrLog((Throwable)e);
            }
        }
    }

    public void createVipTour() {
        try {
            JSONObject mode = TourConfig.instance().config.getJSONArray("modes").getJSONObject(0);
            TourSetting setting = new TourSetting();
            setting.init(mode);
            this.vipTour = new Tour(setting);
            this.vipTour.startHour = TourConfig.instance().config.getJSONObject("vip").getJSONObject("start").getInt("h");
            this.vipTour.endRegisterHour = TourConfig.instance().config.getJSONObject("vip").getJSONObject("endRegister").getInt("h");
            this.vipTour.startMinute = TourConfig.instance().config.getJSONObject("vip").getJSONObject("start").getInt("m");
            this.vipTour.endRegisterMinute = TourConfig.instance().config.getJSONObject("vip").getJSONObject("endRegister").getInt("m");
            this.vipTour.startDay = TourConfig.instance().config.getJSONObject("vip").getInt("day");
            Calendar start = Calendar.getInstance();
            Calendar now = Calendar.getInstance();
            int today = start.get(7);
            start.set(7, this.vipTour.startDay);
            start.set(11, this.vipTour.startHour);
            start.set(12, this.vipTour.startMinute);
            start.set(13, 0);
            if (start.before(now)) {
                start.add(7, 7);
            }
            Calendar end = Calendar.getInstance();
            end.set(7, this.vipTour.startDay);
            end.set(11, this.vipTour.startHour + 1);
            end.set(12, this.vipTour.startMinute);
            end.set(13, 0);
            if (end.before(now)) {
                end.add(7, 7);
            }
            this.vipTour.prizePool = TourConfig.instance().getMinVipPrizePool();
            this.vipTour.ticket = TourConfig.instance().config.getJSONObject("vip").getInt("ticket");
            this.vipTour.tourState = 0;
            PokerTourInfo info = new PokerTourInfo();
            info.startTimeSchedule = start;
            info.endRegisterTime = end;
            info.endRegisterSchedule = end;
            info.tourType = PokerTourType.Vip;
            info.ticket = this.vipTour.ticket;
            info.tourState = PokerTourState.Onschedule;
            PokerTourInfo tourInfo = this.tourService.createPokerTour(info);
            if (tourInfo != null) {
                this.vipTour.tourId = tourInfo.tourId;
                this.vipTour.tourType = 2;
                this.vipTour.init();
            }
            this.addVipTour();
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public synchronized void addVipTour() {
        Tour currentVip = null;
        for (Tour tour : this.dailyTours) {
            if (tour.tourType != 2) continue;
            currentVip = tour;
            break;
        }
        if (currentVip != null) {
            this.dailyTours.remove(currentVip);
        }
        this.dailyTours.add(this.vipTour);
    }

    public void initVipTour() {
        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        start.set(7, 2);
        start.set(11, 0);
        start.set(12, 0);
        start.set(13, 0);
        if (start.after(now)) {
            start.add(7, -7);
        }
        Calendar endDay = Calendar.getInstance();
        endDay.setTime(start.getTime());
        endDay.add(7, 7);
        List vipTours = null;
        try {
            vipTours = this.tourService.getPokerTourList(start, endDay, PokerTourType.Vip);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        if (vipTours != null && vipTours.size() != 0) {
            PokerTourInfo vipInfo = (PokerTourInfo)vipTours.get(0);
            int startDay = vipInfo.startTimeSchedule.get(7);
            if (vipInfo.tourState == PokerTourState.TourStarted || vipInfo.tourState == PokerTourState.RegisterClosed) {
                this.vipTour = this.restorePlayingTour(vipInfo);
                this.vipTour.startDay = startDay;
                this.addVipTour();
                this.vipTour.init();
            } else if (vipInfo.tourState == PokerTourState.Onschedule) {
                this.vipTour = this.restoreOnScheduleTour(vipInfo);
                this.vipTour.startDay = startDay;
                this.addVipTour();
                this.vipTour.init();
            }
        } else {
            this.createVipTour();
        }
    }

    public void init() {
        this.dailyTours.clear();
        this.initDailyTour();
        this.initVipTour();
        this.calculatePlayingAndWaitingTour();
        this.updateVipTourPlayers();
    }

    public synchronized void registerTour(User user, Tour tour) {
        if (tour != null) {
            LogEntry entry = new LogEntry(LogEntry.ACTION.REGISTER);
            SendRegisterTour msg = new SendRegisterTour();
            msg.tourId = tour.tourId;
            SendRegisterTour.ERR err = SendRegisterTour.ERR.PLAYING_OTHER_TOUR;
            if (tour.tourState != 1 || !this.checkUserPlayManyTour(user.getName(), tour)) {
                err = tour.register(user);
            }
            msg.Error = Byte.valueOf((byte)err.ordinal());
            entry.error = err.toString();
            entry.user = user.getName();
            TourUserInfo info = tour.findTourInfoByUser(user);
            if (info != null) {
                msg.timeBuyTicket = info.timeBuyTicket;
                entry.timeBuyTicket = info.timeBuyTicket;
                this.updatePlayerTour(tour, info);
            }
            msg.maxTimeBuyTicket = tour.tourSetting.rebuy;
            GameUtils.send(msg, user);
            SendMoneyInfo moneyMsg = new SendMoneyInfo();
            moneyMsg.money = this.userService.getCurrentMoneyUserCache(user.getName(), "vin");
            GameUtils.send(moneyMsg, user);
            tour.addLogEntry(entry);
            tour.notifyTourInfo();
        }
    }

    public synchronized void registerTour(String nickName, Tour tour) {
        if (tour != null) {
            LogEntry entry = new LogEntry(LogEntry.ACTION.REGISTER);
            SendRegisterTour msg = new SendRegisterTour();
            msg.tourId = tour.tourId;
            SendRegisterTour.ERR err = SendRegisterTour.ERR.PLAYING_OTHER_TOUR;
            if (tour.tourState != 1 || !this.checkUserPlayManyTour(nickName, tour)) {
                err = tour.register(nickName);
            }
            msg.Error = Byte.valueOf((byte)err.ordinal());
            entry.error = err.toString();
            entry.user = nickName;
            TourUserInfo info = tour.findTourInfoByName(nickName);
            if (info != null) {
                msg.timeBuyTicket = info.timeBuyTicket;
                entry.timeBuyTicket = info.timeBuyTicket;
                this.updatePlayerTour(tour, info);
            }
            msg.maxTimeBuyTicket = tour.tourSetting.rebuy;
            User user = ExtensionUtility.globalUserManager.getUserByName(nickName);
            if (user != null) {
                GameUtils.send(msg, user);
                SendMoneyInfo moneyMsg = new SendMoneyInfo();
                moneyMsg.money = this.userService.getCurrentMoneyUserCache(user.getName(), "vin");
                GameUtils.send(moneyMsg, user);
            }
            tour.addLogEntry(entry);
            tour.notifyTourInfo();
        }
    }

    public void registerTour(User user, int tourId) {
        Tour tour = this.findTourById(tourId);
        this.registerTour(user, tour);
    }

    public Tour findTourById(int tourId) {
        for (int i = 0; i < this.dailyTours.size(); ++i) {
            Tour tour = this.dailyTours.get(i);
            if (tour.tourId != tourId) continue;
            return tour;
        }
        return null;
    }

    public synchronized void calculatePlayingAndWaitingTour() {
        this.playingAndWaitingTours.clear();
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(11);
        int minute = cal.get(12);
        int size = this.dailyTours.size();
        for (int i = 0; i < size; ++i) {
            Tour tour = this.dailyTours.get(i);
            if (tour.tourState == 1 || tour.tourState == 2 || tour.tourType == 2) {
                this.playingAndWaitingTours.add(tour);
                continue;
            }
            if (tour.startHour <= hour && (tour.startHour != hour || tour.startMinute <= minute)) continue;
            this.playingAndWaitingTours.add(tour);
        }
    }

    public synchronized List<Tour> getPlayingAndWaitingTours() {
        this.calculatePlayingAndWaitingTour();
        return this.playingAndWaitingTours;
    }

    public SendUserTourInfo checkUserTourInfo(User user, int tourId) {
        Tour tour = this.findTourById(tourId);
        if (tour != null) {
            SendUserTourInfo msg = new SendUserTourInfo();
            TourUserInfo info = tour.findTourInfoByUser(user);
            if (info != null) {
                msg.timeBuyTicket = info.timeBuyTicket;
                msg.timeOutTour = info.timeOutTour;
            } else {
                msg.timeBuyTicket = 0;
                msg.timeOutTour = 0;
            }
            msg.maxTimeBuyTicket = tour.tourSetting.rebuy;
            msg.tourId = tourId;
            return msg;
        }
        return null;
    }

    public String getTourRank(int tourId, int from, int to) {
        Tour tour = this.findTourById(tourId);
        if (tour == null) {
            return "[]";
        }
        return tour.getTourRank(from, to);
    }

    public void clearRegister(Tour tour) {
        try {
            this.tourService.clearUserInTour(tour.tourId);
        }
        catch (SQLException e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public void updateTour(Tour tour) {
        PokerTourInfo tourInfo = new PokerTourInfo();
        tourInfo.tourId = tour.tourId;
        tourInfo.ticket = tour.ticket;
        Calendar start = Calendar.getInstance();
        start.set(11, tour.startHour);
        start.set(12, tour.startMinute);
        start.set(13, 0);
        if (tour.tourType == 2) {
            start.set(7, tour.startDay);
            Calendar now = Calendar.getInstance();
            if (start.before(now) && now.get(7) != 1) {
                start.add(7, 7);
            }
        } else if (tour.startTime != null) {
            start.set(7, tour.startTime.get(7));
        }
        tourInfo.startTimeSchedule = start;
        tourInfo.startTime = tour.startTime;
        Calendar end = Calendar.getInstance();
        end.set(11, tour.endRegisterHour);
        end.set(12, tour.endRegisterMinute);
        end.set(13, 0);
        if (tour.endTime != null) {
            end.set(7, tour.endTime.get(7));
        }
        tourInfo.endRegisterSchedule = end;
        tourInfo.endRegisterTime = tour.endRegisterTime;
        tourInfo.endTime = tour.endTime;
        tourInfo.cancelTime = tour.cancelTime;
        if (tour.tourState == 0 || tour.tourState == 5) {
            tourInfo.tourState = PokerTourState.Onschedule;
        }
        if (tour.tourState == 1) {
            tourInfo.tourState = PokerTourState.TourStarted;
        }
        if (tour.tourState == 3) {
            tourInfo.tourState = PokerTourState.TourEnded;
        }
        if (tour.tourState == 2) {
            tourInfo.tourState = PokerTourState.RegisterClosed;
        }
        if (tour.tourState == 4) {
            tourInfo.tourState = PokerTourState.TourCanceled;
        }
        tourInfo.level = tour.level;
        tourInfo.countTimeUpLevel = tour.countTimeUpLevel;
        tourInfo.tourType = tour.tourType == 1 ? PokerTourType.Daily : PokerTourType.Vip;
        tourInfo.fund = tour.prizePool;
        try {
            this.tourService.updatePokerTour(tourInfo);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        LoggerUtils.debug("tour", "updateTour", GameUtils.toJsonString((Object)tourInfo));
    }

    public void updatePlayerTour(Tour tour, TourUserInfo player) {
        PokerTourPlayer info = new PokerTourPlayer();
        info.currentChip = player.chip;
        info.lastChip = player.lastChip;
        info.nickname = player.nickName;
        info.tourId = tour.tourId;
        info.outTourCount = player.timeOutTour;
        info.ticketCount = player.timeBuyTicket;
        info.outTourTimestamp = player.outTourTimeStamp;
        info.rank = player.rank;
        info.prize = player.prize;
        info.mark = player.score;
        try {
            this.tourService.updatePokerTourPlayer(info);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public PokerFreeTicket useFreeTicket(String nickName, int ticket, int tourType, int tourId) {
        try {
            PokerFreeTicket freeTicket = null;
            freeTicket = tourType == 1 ? this.tourService.usePokerFreeTicket(nickName, ticket, PokerTourType.Daily, tourId) : this.tourService.usePokerFreeTicket(nickName, ticket, PokerTourType.Vip, tourId);
            return freeTicket;
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            return null;
        }
    }

    public synchronized void updateJackpot(Tour tour, int prizePool) {
        try {
            double rate = 0.01;
            int addJackpot = (int)((double)prizePool * rate);
            StringBuilder sb = new StringBuilder();
            sb.append("PokerTour cong tien jack pot #").append(tour.tourId).append(" money ").append(addJackpot);
            boolean result = this.tourService.updateMoneyJackpot(GameUtils.gameName, (long)addJackpot, sb.toString());
            if (!result) {
                CommonHandle.writeErrLogDebug((Object[])new Object[]{"ERROR updateJackpot", sb.toString()});
            } else {
                this.currentJackpot += (long)addJackpot;
                LoggerUtils.debug("tour", sb.toString());
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public synchronized long giveJackpot(List<String> nickNames, GameRoom room, String card, int gameId) {
        Tour tour = (Tour)room.getProperty("TOUR_INFO");
        int size = nickNames.size();
        try {
            long jackpotValue = this.tourService.getMoneyJackPot(GameUtils.gameName);
            double rate = 1.0 * (double)tour.ticket / 1000000.0;
            long totalPrize = (long)((double)jackpotValue * rate);
            StringBuilder sb = new StringBuilder();
            for (String nickName : nickNames) {
                sb.append(nickName).append("_");
            }
            sb.append("No hu pokertour tour ");
            sb.append(tour.tourId).append(" phong ").append(tour.ticket).append(" phien ").append(gameId).append(" bo bai ").append(card);
            boolean result = this.tourService.updateMoneyJackpot(GameUtils.gameName, - totalPrize, sb.toString());
            this.currentJackpot = this.tourService.getMoneyJackPot(GameUtils.gameName);
            if (!result) {
                CommonHandle.writeErrLogDebug((Object[])new Object[]{"Error", sb});
                return 0;
            }
            long eachPrize = totalPrize / (long)size;
            for (String nickName2 : nickNames) {
                MoneyResponse res = TourManager.instance().userService.updateMoney(nickName2, eachPrize, "vin", GameUtils.gameName, "PokerTour", sb.toString(), 0, null, TransType.NO_VIPPOINT);
                if (!res.isSuccess()) {
                    CommonHandle.writeErrLogDebug((Object[])new Object[]{"Error Give Jackpot", nickName2, sb.toString()});
                    continue;
                }
                this.tourService.logJackPotPokerTour(nickName2, tour.ticket, jackpotValue, eachPrize, card, String.valueOf(tour.tourId));
            }
            SendGiveJackpot msg = new SendGiveJackpot();
            msg.nickNames = nickNames;
            msg.currentJackpot = (int)jackpotValue;
            msg.eachPrize = (int)eachPrize;
            this.sendFull(msg);
            return eachPrize;
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            return 0;
        }
    }

    private void sendFull(BaseMsg msg) {
        for (Object user : ExtensionUtility.globalUserManager.getAllUsers()) {
            if (user == null) continue;
            ExtensionUtility.instance().send(msg, (User) user);
        }
    }

    public int getCurrentJackpot() {
        if (this.currentJackpot == 0 || this.currentJackpot == -1) {
            try {
                this.currentJackpot = this.tourService.getMoneyJackPot(GameUtils.gameName);
            }
            catch (SQLException e) {
                CommonHandle.writeErrLog((Throwable)e);
            }
        }
        return (int)this.currentJackpot;
    }

    public void createVipTourFreeTicket(Tour tour) {
        Calendar start = Calendar.getInstance();
        start.set(7, 1);
        start.set(11, 0);
        start.set(12, 0);
        start.set(13, 0);
        start.add(7, -7);
        Calendar end = Calendar.getInstance();
        end.set(7, 7);
        end.set(11, 23);
        end.set(12, 59);
        end.set(13, 59);
        end.add(7, -7);
        this.vips.clear();
        try {
            this.vips = this.tourService.getTop("PokerTour", start, end, 10, 100);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        for (TopTourModel vip : this.vips) {
            try {
                Calendar now = Calendar.getInstance();
                PokerFreeTicket ticket = new PokerFreeTicket();
                ticket.nickname = vip.getNickname();
                ticket.tourType = PokerTourType.Vip;
                ticket.limitTime = tour.endRegisterTime;
                ticket.availableTime = now;
                ticket.createTime = now;
                ticket.used = false;
                ticket.ticket = 100000;
                Bot bot = BotManager.instance().getBotByName(ticket.nickname);
                ticket.isBot = bot != null;
                ticket.createType = PokerTourFreeCreateType.TopWeek;
                ticket.addInfo = VinPlayUtils.getDateTimeStr((Date)start.getTime());
                this.tourService.createPokerFreeTicket(ticket);
                this.registerTour(vip.getNickname(), tour);
            }
            catch (Exception e) {
                CommonHandle.writeErrLog((Throwable)e);
            }
        }
    }

    public void updateVipTourPlayers() {
        int today = Calendar.getInstance().get(7);
        Calendar start = Calendar.getInstance();
        start.set(7, 1);
        start.set(11, 0);
        start.set(12, 0);
        start.set(13, 0);
        if (today == 1) {
            start.add(7, -7);
        }
        Calendar end = Calendar.getInstance();
        end.set(7, 7);
        end.set(11, 23);
        end.set(12, 59);
        end.set(13, 59);
        if (today == 1) {
            end.add(7, -7);
        }
        this.vips.clear();
        try {
            this.vips = this.tourService.getTop("PokerTour", start, end, 10, 100);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public String getVipTourRankNotStart() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < this.vips.size(); ++i) {
            arr.put((Object)this.toRankJson(this.vips.get(i), i + 1, this.getVipTourPrize(i + 1)));
        }
        return arr.toString();
    }

    public int getVipTourPrize(int rank) {
        PrizeRate rate = TourPrize.findPrizeRate(this.vips.size());
        int prize = TourPrize.getPrize(rate, rank, 10000000);
        return prize;
    }

    public JSONObject toRankJson(TopTourModel model, int rank, int prize) {
        JSONObject json = new JSONObject();
        try {
            json.put("n", (Object)model.getNickname());
            json.put("r", rank);
            json.put("p", prize);
            json.put("c", 10000);
            json.put("l", 10000);
        }
        catch (Exception e) {
            return null;
        }
        return json;
    }

    public synchronized void playingOtherTour(String nickName, int tourId) {
        LoggerUtils.debug("tour", nickName, "playing at tour", tourId);
        this.userPlayingTour.put(nickName, tourId);
    }

    public synchronized boolean checkUserPlayManyTour(String nickName, Tour tour) {
        if (this.userPlayingTour.containsKey(nickName)) {
            int tourId = this.userPlayingTour.get(nickName);
            if (tourId != tour.tourId) {
                LoggerUtils.debug("tour", nickName, " is already playing at tour", tourId);
                return true;
            }
            return false;
        }
        return false;
    }

    public void unlockPlayingUser(Tour tour) {
        for (int i = 0; i < tour.allRegisteredPlayers.size(); ++i) {
            TourUserInfo info = tour.allRegisteredPlayers.get(i);
            this.unlockPlayingUser(tour, info);
        }
    }

    public void unlockPlayingUser(Tour tour, TourUserInfo tourInfo) {
        Integer tourId = this.userPlayingTour.get(tourInfo.nickName);
        if (tourId != null && tourId == tour.tourId) {
            this.userPlayingTour.remove(tourInfo.nickName);
        }
    }

    public void giveDailyPrize(int today) {
        try {
            Integer day;
            String s = this.tourService.getString("Last_Day_Give_Prize");
            if (s == null) {
                s = "-1";
            }
            if ((day = Integer.valueOf(Integer.parseInt(s))) != today) {
                this.tourService.saveString("Last_Day_Give_Prize", String.valueOf(today));
                if (today == 1) {
                    this.giveWeeklyPrize();
                }
                this.giveDailyTopPrize();
                this.tourService.saveString("Last_Day_Give_Prize", String.valueOf(today));
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((String)("Error give daily prize: " + today));
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public void giveDailyTopPrize() {
        Calendar start = Calendar.getInstance();
        start.add(7, -1);
        start.set(11, 0);
        start.set(12, 0);
        Calendar end = Calendar.getInstance();
        end.add(7, -1);
        end.set(11, 23);
        end.set(12, 59);
        end.set(13, 59);
        List dailyTops = null;
        try {
            dailyTops = this.tourService.getTop("PokerTour", start, end, 3, 20);
            for (int i = 0; i < dailyTops.size(); ++i) {
                int j;
                int rank = i + 1;
                TopTourModel model = (TopTourModel)dailyTops.get(i);
                Calendar now = Calendar.getInstance();
                PokerFreeTicket ticket = new PokerFreeTicket();
                ticket.nickname = model.getNickname();
                ticket.tourType = PokerTourType.Daily;
                ticket.availableTime = now;
                ticket.createTime = now;
                ticket.used = false;
                Bot bot = BotManager.instance().getBotByName(ticket.nickname);
                ticket.isBot = bot != null;
                ticket.createType = PokerTourFreeCreateType.TopDay;
                ticket.addInfo = VinPlayUtils.getDateTimeStr((Date)start.getTime());
                if (rank == 1) {
                    ticket.ticket = 200000;
                    this.tourService.createPokerFreeTicket(ticket);
                    for (j = 0; j < 3; ++j) {
                        ticket.ticket = 100000;
                        this.tourService.createPokerFreeTicket(ticket);
                    }
                    continue;
                }
                if (rank >= 2 && rank <= 3) {
                    ticket.ticket = 200000;
                    this.tourService.createPokerFreeTicket(ticket);
                    ticket.ticket = 100000;
                    this.tourService.createPokerFreeTicket(ticket);
                    continue;
                }
                if (rank >= 4 && rank <= 6) {
                    ticket.ticket = 100000;
                    this.tourService.createPokerFreeTicket(ticket);
                    continue;
                }
                if (rank >= 7 && rank <= 10) {
                    ticket.ticket = 50000;
                    this.tourService.createPokerFreeTicket(ticket);
                    continue;
                }
                if (rank < 11 || rank > 20) continue;
                for (j = 0; j < 2; ++j) {
                    ticket.ticket = 10000;
                    this.tourService.createPokerFreeTicket(ticket);
                }
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
    }

    public void giveWeeklyPrize() {
        this.createVipTourFreeTicket(this.vipTour);
    }

    public synchronized void registerTourForBot() {
        if (!GameUtils.isBot) {
            return;
        }
        List<Tour> playingTour = this.getPlayingAndWaitingTours();
        for (int i = 0; i < playingTour.size(); ++i) {
            Tour tour = playingTour.get(i);
            if (tour.isBotRegistered || tour.allRegisteredPlayers.size() >= 6) {
                return;
            }
            tour.isBotRegistered = true;
            if (tour.tourType != 1) continue;
            tour.maxNumberOfBot = GameUtils.rd.nextInt(2) + 6 - tour.allRegisteredPlayers.size();
            while (tour.users.size() < tour.maxNumberOfBot) {
                Bot bot = BotManager.instance().findFitBot(tour.ticket);
                if (bot == null) continue;
                tour.users.add(bot.user);
            }
            tour.fixTopPrize();
            LoggerUtils.debug("tour", "registerTourForBot", "tour", tour.tourId, "bot count: ", tour.users.size(), "time: ", VinPlayUtils.getCurrentDateTime());
        }
    }

    public void reconnectTour(User user) {
        for (Tour tour : this.dailyTours) {
            boolean check;
            TourUserInfo info;
            if (tour.tourState != 0 && tour.tourState != 1 && tour.tourState != 2 || !(check = tour.checkUserInRoom(user)) || (info = tour.findTourInfoByUser(user)) == null || info.timeBuyTicket <= info.timeOutTour) continue;
            TourManager.instance().registerTour(user, tour);
            return;
        }
    }

    public void freeTicketCode(User user, String code) {
        try {
            PokerFreeTicketResponse res = this.tourService.getTicketFromFreeCode(user.getName(), code);
            if (res.getTicket() != null) {
                SendNewFreeTicket msg = new SendNewFreeTicket();
                msg.ticket = res.getTicket().ticket;
                ExtensionUtility.instance().send((BaseMsg)msg, user);
            } else {
                SendNewFreeTicket msg = new SendNewFreeTicket();
                msg.Error = Byte.valueOf((byte)res.errorCode);
                msg.timeBlock = res.timeBlock;
                ExtensionUtility.instance().send((BaseMsg)msg, user);
            }
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            SendNewFreeTicket msg = new SendNewFreeTicket();
            msg.Error = Byte.valueOf("1");
            ExtensionUtility.instance().send((BaseMsg)msg, user);
        }
    }

    public void destroy(Tour tour) {
        tour.destroy();
        tour.allRegisteredPlayers.clear();
        tour.roomGroup.destroy();
        this.dailyTours.remove(tour);
        this.sendGetListTour();
    }

    public synchronized void sendGetListTour() {
        SendGetListTour msg = new SendGetListTour();
        List<Tour> allTour = TourManager.instance().getPlayingAndWaitingTours();
        msg.playingAndWaitingTours = new LinkedList<Tour>();
        for (Tour tour : allTour) {
            if (tour == null) continue;
            msg.playingAndWaitingTours.add(tour);
        }
        for (Object user : ExtensionUtility.globalUserManager.getAllUsers()) {
            ExtensionUtility.instance().send((BaseMsg)msg, (User) user);
        }
    }

    public synchronized void sendGetListTour(User user) {
        SendGetListTour msg = new SendGetListTour();
        List<Tour> allTour = TourManager.instance().getPlayingAndWaitingTours();
        msg.playingAndWaitingTours = new LinkedList<Tour>();
        for (Tour tour : allTour) {
            if (tour == null) continue;
            msg.playingAndWaitingTours.add(tour);
        }
        ExtensionUtility.instance().send((BaseMsg)msg, user);
    }

    public synchronized boolean checkInVipTour(TourUserInfo info) {
        for (TopTourModel top : this.vips) {
            if (!top.getNickname().equalsIgnoreCase(info.nickName)) continue;
            return true;
        }
        return false;
    }
}

