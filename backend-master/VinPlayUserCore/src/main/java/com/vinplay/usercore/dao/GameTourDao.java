/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.usercore.dao;

import com.vinplay.gamebai.entities.GameFreeCodeDetail;
import com.vinplay.gamebai.entities.GameFreeCodePackage;
import com.vinplay.gamebai.entities.GameFreeCodeStatistic;
import com.vinplay.gamebai.entities.PokerFreeTicket;
import com.vinplay.gamebai.entities.PokerFreeTicketStatistic;
import com.vinplay.gamebai.entities.PokerTicketCount;
import com.vinplay.gamebai.entities.PokerTourInfo;
import com.vinplay.gamebai.entities.PokerTourInfoDetail;
import com.vinplay.gamebai.entities.PokerTourInfoGeneral;
import com.vinplay.gamebai.entities.PokerTourPlayer;
import com.vinplay.gamebai.entities.PokerTourState;
import com.vinplay.gamebai.entities.PokerTourType;
import com.vinplay.gamebai.entities.TopTourModel;
import com.vinplay.gamebai.entities.UserTourModel;
import com.vinplay.gamebai.entities.VipTourModel;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GameTourDao {
    public boolean updateMark(String var1, String var2, String var3, int var4, Calendar var5, int var6, int var7, int var8, int var9, String var10) throws SQLException, ParseException;

    public List<VipTourModel> getVips(String var1, String var2) throws SQLException;

    public boolean saveVipTour(List<TopTourModel> var1, String var2, String var3, String var4) throws SQLException;

    public boolean updateMoneyJackpot(String var1, long var2) throws SQLException;

    public long getMoneyJackPot(String var1) throws SQLException;

    public String getString(String var1) throws SQLException;

    public boolean saveString(String var1, String var2) throws SQLException;

    public Object[] getUserTours(String var1, String var2, String var3, int var4) throws SQLException, ParseException;

    public List<UserTourModel> getLogUserTour(String var1, String var2, int var3, int var4) throws SQLException, ParseException;

    public PokerTourInfo createPokerTour(PokerTourInfo var1) throws ParseException, SQLException;

    public PokerTourInfo getPokerTour(int var1) throws ParseException, SQLException;

    public boolean updatePokerTour(PokerTourInfo var1) throws ParseException, SQLException;

    public boolean updatePokerTourPlayer(PokerTourPlayer var1) throws ParseException, SQLException;

    public PokerTourPlayer getPokerTourPlayer(int var1, String var2) throws ParseException, SQLException;

    public List<PokerTourInfo> getPokerTourList(Calendar var1, Calendar var2, PokerTourType var3) throws ParseException, SQLException;

    public List<PokerTourPlayer> getPokerTourPlayers(int var1) throws ParseException, SQLException;

    public PokerFreeTicket createPokerFreeTicket(PokerFreeTicket var1) throws ParseException, SQLException;

    public boolean canCreateFreeTicket(String var1, PokerTourType var2, String var3, String var4) throws ParseException, SQLException;

    public PokerFreeTicket usePokerFreeTicket(String var1, int var2, PokerTourType var3, int var4) throws ParseException, SQLException;

    public List<PokerTicketCount> getPokerFreeTicket(String var1) throws ParseException, SQLException;

    public List<PokerTourInfoGeneral> getPokerTourListGeneral(Integer var1, PokerTourState var2, PokerTourType var3, int var4, int var5) throws ParseException, SQLException;

    public PokerTourInfoDetail getPokerTourDetail(int var1) throws ParseException, SQLException;

    public PokerFreeTicketStatistic getPokerFreeTicketStatistic(int var1, String var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, String var10, String var11, int var12, int var13) throws ParseException, SQLException;

    public int exportFreeCode(String var1, int var2, int var3, int var4, Calendar var5, String var6, Set<String> var7) throws ParseException, SQLException;

    public Set<String> getAllCode() throws SQLException;

    public List<GameFreeCodePackage> getFreeCodePackage(int var1, String var2, int var3, int var4, String var5, String var6, String var7) throws ParseException, SQLException;

    public List<GameFreeCodeDetail> getFreeCodeDetails(int var1, String var2, int var3, String var4, int var5, int var6, int var7, String var8, String var9, String var10, String var11, int var12) throws ParseException, SQLException;

    public GameFreeCodeDetail getFreeCodeDetail(String var1, String var2) throws ParseException, SQLException;

    public boolean updateFreeCodeDetail(int var1, String var2, int var3, String var4, int var5, int var6, int var7, List<Integer> var8) throws ParseException, SQLException;

    public PokerFreeTicket getTicketFromFreeCode(String var1, String var2, GameFreeCodeDetail var3, boolean var4) throws ParseException, SQLException;

    public Map<Integer, GameFreeCodeStatistic> getFreeCodeStatistic(String var1, String var2, String var3, int var4) throws ParseException, SQLException;

    public boolean clearUserInTour(int var1) throws SQLException;
}

