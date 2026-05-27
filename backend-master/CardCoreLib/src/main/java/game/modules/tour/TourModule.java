/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.engine.sessions.ISession
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  bitzero.util.common.business.CommonHandle
 *  com.vinplay.usercore.service.UserService
 */
package game.modules.tour;

import bitzero.engine.sessions.ISession;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import bitzero.util.common.business.CommonHandle;
import com.vinplay.usercore.service.UserService;
import game.eventHandlers.GameEventParam;
import game.eventHandlers.GameEventType;
import game.modules.gameRoom.entities.GameRoom;
import game.modules.tour.control.Tour;
import game.modules.tour.control.TourManager;
import game.modules.tour.control.TourUserInfo;
import game.modules.tour.control.cmd.rev.RevFreeTicketCode;
import game.modules.tour.control.cmd.rev.RevGetTourInfo;
import game.modules.tour.control.cmd.rev.RevGetTourRank;
import game.modules.tour.control.cmd.rev.RevRegisterTour;
import game.modules.tour.control.cmd.rev.RevSetTourStartTime;
import game.modules.tour.control.cmd.rev.RevTourUserInfo;
import game.modules.tour.control.cmd.send.SendJackpotInfo;
import game.modules.tour.control.cmd.send.SendMoneyInfo;
import game.modules.tour.control.cmd.send.SendTourInfo;
import game.modules.tour.control.cmd.send.SendTourRank;
import game.modules.tour.control.cmd.send.SendUserTourInfo;
import game.modules.tour.log.LogEntry;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import java.io.IOException;

public class TourModule
extends BaseClientRequestHandler {
    public void init() {
        TourManager.instance();
        this.getParentExtension().addEventListener((IBZEventType)GameEventType.OUT_TOUR, (IBZEventListener)this);
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == GameEventType.OUT_TOUR) {
            User user = (User)ibzevent.getParameter((IBZEventParam)GameEventParam.USER);
            this.outTour(user);
        }
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 5200: {
                this.registerTour(user, dataCmd);
                break;
            }
            case 5201: {
                this.getListTour(user, dataCmd);
                break;
            }
            case 5202: {
                this.getTourUserInfo(user, dataCmd);
                break;
            }
            case 5203: {
                this.getTourRank(user, dataCmd);
                break;
            }
            case 5205: {
                this.getTourInfo(user, dataCmd);
                break;
            }
            case 5204: {
                this.getTourLevel(user, dataCmd);
                break;
            }
            case 5207: {
                this.setTourStartTime(user, dataCmd);
                break;
            }
            case 5211: {
                this.getJackpotInfo(user, dataCmd);
                break;
            }
            case 5212: {
                this.reconnectTour(user, dataCmd);
                break;
            }
            case 5214: {
                this.freeTicketCode(user, dataCmd);
            }
        }
    }

    private void getTourLevel(User user, DataCmd data) {
        Tour tour;
        GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
        if (room != null && (tour = (Tour)room.getProperty("TOUR_INFO")) != null) {
            tour.sendUpdateLevel(user);
        }
    }

    private void registerTour(User user, DataCmd dataCmd) {
        RevRegisterTour cmd = new RevRegisterTour(dataCmd);
        TourManager.instance().registerTour(user, cmd.tourId);
    }

    private void getListTour(User user, DataCmd dataCmd) {
        TourManager.instance().sendGetListTour(user);
    }

    private void getTourUserInfo(User user, DataCmd dataCmd) {
        RevTourUserInfo cmd = new RevTourUserInfo(dataCmd);
        SendUserTourInfo msg = TourManager.instance().checkUserTourInfo(user, cmd.tourId);
        if (msg != null) {
            this.send((BaseMsg)msg, user);
        }
    }

    private void getTourInfo(User user, DataCmd dataCmd) {
        RevGetTourInfo cmd = new RevGetTourInfo(dataCmd);
        Tour tour = TourManager.instance().findTourById(cmd.tourId);
        if (tour != null) {
            SendTourInfo msg = new SendTourInfo();
            msg.tourId = tour.tourId;
            msg.prizePool = tour.prizePool;
            msg.level = (byte)tour.level;
            msg.userCount = tour.playingCount();
            msg.registerCount = tour.totalCount();
            msg.endRegisterHour = tour.endRegisterHour;
            msg.endRegisterMinute = tour.endRegisterMinute;
            this.send((BaseMsg)msg, user);
        }
    }

    private void getTourRank(User user, DataCmd dataCmd) {
        RevGetTourRank cmd = new RevGetTourRank(dataCmd);
        LoggerUtils.debug("tour", "GetTourRank:", cmd.tourId, "from", cmd.from, "to", cmd.to);
        SendTourRank msg = new SendTourRank();
        if (cmd.tourId >= 0) {
            msg.tourId = cmd.tourId;
            msg.bxh = TourManager.instance().getTourRank(cmd.tourId, cmd.from, cmd.to);
        } else {
            Tour tour;
            GameRoom room = (GameRoom)user.getProperty((Object)"GAME_ROOM");
            if (room != null && (tour = (Tour)room.getProperty("TOUR_INFO")) != null) {
                msg.tourId = tour.tourId;
                msg.bxh = tour.getTourRank(cmd.from, cmd.to);
                LoggerUtils.debug("tour", "getTourRank", GameUtils.toJsonString((Object)msg));
            }
        }
        this.send((BaseMsg)msg, user);
    }

    private void outTour(User user) {
        TourUserInfo tourInfo = (TourUserInfo)user.getProperty((Object)"TOUR_USER_INFO");
        if (tourInfo != null) {
            ++tourInfo.timeOutTour;
            tourInfo.outTourTimeStamp = System.currentTimeMillis() / 1000;
            user.removeProperty((Object)"TOUR_USER_INFO");
            Tour tour = TourManager.instance().findTourById(tourInfo.tourId);
            if (tour != null) {
                TourManager.instance().updatePlayerTour(tour, tourInfo);
                TourManager.instance().unlockPlayingUser(tour, tourInfo);
                LogEntry entry = new LogEntry(LogEntry.ACTION.OUT_TOUR);
                entry.initTourInfo(tourInfo);
                tour.addLogEntry(entry);
                tour.notifyTourInfo();
                long money = TourManager.instance().userService.getCurrentMoneyUserCache(user.getName(), "vin");
                SendMoneyInfo msg = new SendMoneyInfo();
                msg.money = money;
                this.send((BaseMsg)msg, user);
            }
        }
        if (GameUtils.isMainTain) {
            try {
                user.getSession().close();
            }
            catch (IOException e) {
                CommonHandle.writeErrLog((Throwable)e);
            }
        }
    }

    private void setTourStartTime(User user, DataCmd data) {
        RevSetTourStartTime cmd = new RevSetTourStartTime(data);
        Tour tour = TourManager.instance().findTourById(cmd.tourId);
        if (tour != null) {
            tour.setStartTime(cmd.startHour, cmd.startMinute);
            TourManager.instance().calculatePlayingAndWaitingTour();
        }
    }

    private void getJackpotInfo(User user, DataCmd data) {
        SendJackpotInfo msg = new SendJackpotInfo();
        msg.currentJackpot = TourManager.instance().getCurrentJackpot();
        this.send((BaseMsg)msg, user);
    }

    private void reconnectTour(User user, DataCmd data) {
        TourManager.instance().reconnectTour(user);
    }

    private void freeTicketCode(User user, DataCmd data) {
        RevFreeTicketCode cmd = new RevFreeTicketCode(data);
        TourManager.instance().freeTicketCode(user, cmd.code);
    }
}

