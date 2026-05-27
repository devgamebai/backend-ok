/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.utils.VinPlayUtils
 */
package com.vinplay.usercore.dao.impl;

import com.vinplay.gamebai.entities.GameFreeCodeDetail;
import com.vinplay.gamebai.entities.GameFreeCodePackage;
import com.vinplay.gamebai.entities.GameFreeCodeStatistic;
import com.vinplay.gamebai.entities.PokerFreeTicket;
import com.vinplay.gamebai.entities.PokerFreeTicketStatistic;
import com.vinplay.gamebai.entities.PokerTicketCount;
import com.vinplay.gamebai.entities.PokerTourFreeCreateType;
import com.vinplay.gamebai.entities.PokerTourInfo;
import com.vinplay.gamebai.entities.PokerTourInfoDetail;
import com.vinplay.gamebai.entities.PokerTourInfoGeneral;
import com.vinplay.gamebai.entities.PokerTourPlayer;
import com.vinplay.gamebai.entities.PokerTourState;
import com.vinplay.gamebai.entities.PokerTourType;
import com.vinplay.gamebai.entities.PokerTourUserDetail;
import com.vinplay.gamebai.entities.TopTourModel;
import com.vinplay.gamebai.entities.UserTourModel;
import com.vinplay.gamebai.entities.VipTourModel;
import com.vinplay.usercore.dao.GameTourDao;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameTourDaoImpl
implements GameTourDao {
    @Override
    public boolean updateMark(String nickname, String gamename, String tourId, int fee, Calendar startTime, int mark, int top, int userTotal, int status, String prize) throws SQLException, ParseException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
             CallableStatement call = conn.prepareCall("CALL save_tour_mark(?,?,?,?,?,?,?,?,?,?)")) {
            call.setString(1, nickname);
            call.setString(2, gamename);
            call.setString(3, tourId);
            call.setInt(4, fee);
            call.setString(5, VinPlayUtils.getDateTimeStr((Date)startTime.getTime()));
            call.setInt(6, mark);
            call.setInt(7, top);
            call.setInt(8, userTotal);
            call.setInt(9, status);
            call.setString(10, prize);
            call.executeUpdate();
            res = true;
        }
        catch (SQLException e) {
            throw e;
        }
        return res;
    }

    @Override
    public List<VipTourModel> getVips(String vipTourId, String gamename) throws SQLException {
        ArrayList<VipTourModel> res = new ArrayList<VipTourModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT `nick_name`,`value` FROM game_tour_vip WHERE `vip_tour_id`=? AND game_name = ?";
            PreparedStatement stm = conn.prepareStatement("SELECT `nick_name`,`value` FROM game_tour_vip WHERE `vip_tour_id`=? AND game_name = ?");
            stm.setString(1, vipTourId);
            stm.setString(2, gamename);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String nickname = rs.getString("nick_name");
                String value = rs.getString("value");
                VipTourModel model = new VipTourModel(nickname, vipTourId, value);
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean saveVipTour(List<TopTourModel> topTours, String gamename, String vipTourId, String value) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai")) {
            for (TopTourModel model : topTours) {
                try (CallableStatement call = conn.prepareCall("CALL save_tour_vip(?,?,?,?)")) {
                    call.setString(1, model.getNickname());
                    call.setString(2, gamename);
                    call.setString(3, vipTourId);
                    call.setString(4, value);
                    call.executeUpdate();
                }
            }
            res = true;
        }
        catch (SQLException e) {
            throw e;
        }
        return res;
    }

    @Override
    public boolean updateMoneyJackpot(String key, long money) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
             CallableStatement call = conn.prepareCall("CALL save_tour_jackpot(?,?)")) {
            call.setString(1, key);
            call.setLong(2, money);
            call.executeUpdate();
            res = true;
        }
        catch (SQLException e) {
            throw e;
        }
        return res;
    }

    @Override
    public long getMoneyJackPot(String key) throws SQLException {
        long res = -1L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT `value` FROM game_tour_jackpot WHERE `key`=?";
            PreparedStatement stm = conn.prepareStatement("SELECT `value` FROM game_tour_jackpot WHERE `key`=?");
            stm.setString(1, key);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getLong("value");
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public String getString(String key) throws SQLException {
        String res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT `value` FROM game_tour_info WHERE `key`=?";
            PreparedStatement stm = conn.prepareStatement("SELECT `value` FROM game_tour_info WHERE `key`=?");
            stm.setString(1, key);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res = rs.getString("value");
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean saveString(String key, String value) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
             CallableStatement call = conn.prepareCall("CALL save_tour_info(?,?)")) {
            int param = 1;
            call.setString(param++, key);
            call.setString(param++, value);
            call.executeUpdate();
            res = true;
        }
        catch (SQLException e) {
            throw e;
        }
        return res;
    }

    @Override
    public Object[] getUserTours(String gamename, String startTime, String endTime, int maxTour) throws SQLException, ParseException {
        Object[] res = null;
        HashMap<String, TopTourModel> map1 = new HashMap<String, TopTourModel>();
        HashMap<String, Long> map2 = new HashMap<String, Long>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM game_tour_mark WHERE `start_time` >= ? AND `start_time` <= ? AND status = 1 AND top <= 15 AND game_name = ? ORDER BY `mark` DESC";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM game_tour_mark WHERE `start_time` >= ? AND `start_time` <= ? AND status = 1 AND top <= 15 AND game_name = ? ORDER BY `mark` DESC");
            stm.setString(1, startTime);
            stm.setString(2, endTime);
            stm.setString(3, gamename);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String nickname = rs.getString("nick_name");
                String tourId = rs.getString("tour_id");
                int fee = rs.getInt("fee");
                Date tourStartTime = VinPlayUtils.getDateTime((String)rs.getString("start_time"));
                int mark = rs.getInt("mark");
                int top = rs.getInt("top");
                int userTotal = rs.getInt("user_total");
                String prize = rs.getString("prize");
                UserTourModel model = new UserTourModel(nickname, tourId, fee, tourStartTime, mark, top, userTotal, prize);
                TopTourModel topTour = null;
                if (map1.containsKey(nickname)) {
                    topTour = (TopTourModel)map1.get(nickname);
                    topTour.addTour(model, maxTour);
                } else {
                    topTour = new TopTourModel(nickname, model);
                }
                map1.put(nickname, topTour);
                map2.put(nickname, topTour.getTotalMark());
            }
            rs.close();
            stm.close();
        }
        res = new Object[]{map1, map2};
        return res;
    }

    @Override
    public List<UserTourModel> getLogUserTour(String gamename, String nickname, int rowStart, int skip) throws SQLException, ParseException {
        ArrayList<UserTourModel> res = new ArrayList<UserTourModel>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM game_tour_mark WHERE status = 1 AND game_name = ? AND nick_name = ? ORDER BY `start_time` DESC LIMIT ?, ?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM game_tour_mark WHERE status = 1 AND game_name = ? AND nick_name = ? ORDER BY `start_time` DESC LIMIT ?, ?");
            stm.setString(1, gamename);
            stm.setString(2, nickname);
            stm.setInt(3, rowStart);
            stm.setInt(4, skip);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String tourId = rs.getString("tour_id");
                int fee = rs.getInt("fee");
                Date tourStartTime = VinPlayUtils.getDateTime((String)rs.getString("start_time"));
                int mark = rs.getInt("mark");
                int top = rs.getInt("top");
                int userTotal = rs.getInt("user_total");
                String prize = rs.getString("prize");
                UserTourModel model = new UserTourModel(nickname, tourId, fee, tourStartTime, mark, top, userTotal, prize);
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public PokerTourInfo createPokerTour(PokerTourInfo tourInfo) throws ParseException, SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "INSERT INTO poker_tour(start_time_schedule, end_register_schedule, start_time, end_register_time, cancel_time, end_time, tour_state, tour_type, level, ticket, count_time_up_level, fund) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO poker_tour(start_time_schedule, end_register_schedule, start_time, end_register_time, cancel_time, end_time, tour_state, tour_type, level, ticket, count_time_up_level, fund) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 1);
            int param = 1;
            stm.setString(param++, tourInfo.startTimeSchedule == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.startTimeSchedule.getTime()));
            stm.setString(param++, tourInfo.endRegisterSchedule == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.endRegisterSchedule.getTime()));
            stm.setString(param++, tourInfo.startTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.startTime.getTime()));
            stm.setString(param++, tourInfo.endRegisterTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.endRegisterTime.getTime()));
            stm.setString(param++, tourInfo.cancelTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.cancelTime.getTime()));
            stm.setString(param++, tourInfo.endTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.endTime.getTime()));
            stm.setInt(param++, tourInfo.tourState != null ? tourInfo.tourState.getId() : 0);
            stm.setInt(param++, tourInfo.tourType != null ? tourInfo.tourType.getId() : 0);
            stm.setInt(param++, tourInfo.level);
            stm.setInt(param++, tourInfo.ticket);
            stm.setInt(param++, tourInfo.countTimeUpLevel);
            stm.setLong(param++, tourInfo.fund);
            if (stm.executeUpdate() == 1) {
                try (ResultSet generatedKeys = stm.getGeneratedKeys();){
                    if (generatedKeys.next()) {
                        int tourId;
                        tourInfo.tourId = tourId = generatedKeys.getInt(1);
                    }
                }
            }
            stm.close();
        }
        return tourInfo;
    }

    @Override
    public PokerTourInfo getPokerTour(int tourId) throws ParseException, SQLException {
        PokerTourInfo res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_tour WHERE `id`=?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM poker_tour WHERE `id`=?");
            stm.setInt(1, tourId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                Calendar startTimeSchedule = VinPlayUtils.getCalendar((String)rs.getString("start_time_schedule"));
                Calendar endRegisterSchedule = VinPlayUtils.getCalendar((String)rs.getString("end_register_schedule"));
                Calendar startTime = VinPlayUtils.getCalendar((String)rs.getString("start_time"));
                Calendar endRegisterTime = VinPlayUtils.getCalendar((String)rs.getString("end_register_time"));
                Calendar cancelTime = VinPlayUtils.getCalendar((String)rs.getString("cancel_time"));
                Calendar endTime = VinPlayUtils.getCalendar((String)rs.getString("end_time"));
                PokerTourState tourState = PokerTourState.getById(rs.getInt("tour_state"));
                PokerTourType tourType = PokerTourType.getById(rs.getInt("tour_type"));
                int level = rs.getInt("level");
                int ticket = rs.getInt("ticket");
                int countTimeUpLevel = rs.getInt("count_time_up_level");
                long fund = rs.getLong("fund");
                res = new PokerTourInfo(tourId, startTimeSchedule, endRegisterSchedule, tourState, tourType, startTime, endRegisterTime, cancelTime, endTime, level, ticket, countTimeUpLevel, fund);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean updatePokerTour(PokerTourInfo tourInfo) throws ParseException, SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "UPDATE poker_tour SET start_time_schedule = ?, end_register_schedule = ?, start_time = ?, end_register_time = ?, cancel_time = ?, end_time = ?, tour_state = ?, tour_type = ?, level = ?, ticket = ?, count_time_up_level = ?, fund=? WHERE id=?";
            PreparedStatement stm = conn.prepareStatement("UPDATE poker_tour SET start_time_schedule = ?, end_register_schedule = ?, start_time = ?, end_register_time = ?, cancel_time = ?, end_time = ?, tour_state = ?, tour_type = ?, level = ?, ticket = ?, count_time_up_level = ?, fund=? WHERE id=?");
            int param = 1;
            stm.setString(param++, tourInfo.startTimeSchedule == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.startTimeSchedule.getTime()));
            stm.setString(param++, tourInfo.endRegisterSchedule == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.endRegisterSchedule.getTime()));
            stm.setString(param++, tourInfo.startTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.startTime.getTime()));
            stm.setString(param++, tourInfo.endRegisterTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.endRegisterTime.getTime()));
            stm.setString(param++, tourInfo.cancelTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.cancelTime.getTime()));
            stm.setString(param++, tourInfo.endTime == null ? null : VinPlayUtils.getDateTimeStr((Date)tourInfo.endTime.getTime()));
            stm.setInt(param++, tourInfo.tourState != null ? tourInfo.tourState.getId() : 0);
            stm.setInt(param++, tourInfo.tourType != null ? tourInfo.tourType.getId() : 0);
            stm.setInt(param++, tourInfo.level);
            stm.setInt(param++, tourInfo.ticket);
            stm.setInt(param++, tourInfo.countTimeUpLevel);
            stm.setLong(param++, tourInfo.fund);
            stm.setInt(param++, tourInfo.tourId);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public boolean updatePokerTourPlayer(PokerTourPlayer player) throws ParseException, SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "INSERT INTO poker_tour_user  (tour_id, nick_name, current_chip, ticket_count, out_tour_count, out_tour_time, last_chip, rank, mark, prize) VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE  current_chip=?, ticket_count=?, out_tour_count=?, out_tour_time=?, last_chip=?, rank=?, mark=?, prize=?";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO poker_tour_user  (tour_id, nick_name, current_chip, ticket_count, out_tour_count, out_tour_time, last_chip, rank, mark, prize) VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE  current_chip=?, ticket_count=?, out_tour_count=?, out_tour_time=?, last_chip=?, rank=?, mark=?, prize=?");
            int param = 1;
            stm.setInt(param++, player.tourId);
            stm.setString(param++, player.nickname);
            stm.setLong(param++, player.currentChip);
            stm.setInt(param++, player.ticketCount);
            stm.setInt(param++, player.outTourCount);
            stm.setLong(param++, player.outTourTimestamp);
            stm.setLong(param++, player.lastChip);
            stm.setInt(param++, player.rank);
            stm.setInt(param++, player.mark);
            stm.setInt(param++, player.prize);
            stm.setLong(param++, player.currentChip);
            stm.setInt(param++, player.ticketCount);
            stm.setInt(param++, player.outTourCount);
            stm.setLong(param++, player.outTourTimestamp);
            stm.setLong(param++, player.lastChip);
            stm.setInt(param++, player.rank);
            stm.setInt(param++, player.mark);
            stm.setInt(param++, player.prize);
            if (stm.executeUpdate() == 1) {
                res = true;
            }
            stm.close();
        }
        return res;
    }

    @Override
    public PokerTourPlayer getPokerTourPlayer(int tourId, String nickname) throws ParseException, SQLException {
        PokerTourPlayer res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_tour_user WHERE `tour_id`=? AND `nick_name`=?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM poker_tour_user WHERE `tour_id`=? AND `nick_name`=?");
            stm.setInt(1, tourId);
            stm.setString(2, nickname);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                long currentChip = rs.getLong("current_chip");
                long lastChip = rs.getLong("last_chip");
                int ticketCount = rs.getInt("ticket_count");
                int outTourCount = rs.getInt("out_tour_count");
                long outTourTimestamp = rs.getLong("out_tour_time");
                int rank = rs.getInt("rank");
                int mark = rs.getInt("mark");
                int prize = rs.getInt("prize");
                res = new PokerTourPlayer(tourId, nickname, currentChip, lastChip, ticketCount, outTourCount, outTourTimestamp, rank, mark, prize);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<PokerTourInfo> getPokerTourList(Calendar startTimeC, Calendar endTimeC, PokerTourType type) throws ParseException, SQLException {
        ArrayList<PokerTourInfo> res = new ArrayList<PokerTourInfo>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_tour WHERE `start_time_schedule` >=? AND `start_time_schedule` <=? ";
            if (type != null) {
                sql = sql + " AND tour_type = ?";
            }
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, VinPlayUtils.getDateTimeStr((Date)startTimeC.getTime()));
            stm.setString(2, VinPlayUtils.getDateTimeStr((Date)endTimeC.getTime()));
            if (type != null) {
                stm.setInt(3, type.getId());
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int tourId = rs.getInt("id");
                Calendar startTimeSchedule = VinPlayUtils.getCalendar((String)rs.getString("start_time_schedule"));
                Calendar endRegisterSchedule = VinPlayUtils.getCalendar((String)rs.getString("end_register_schedule"));
                Calendar startTime = VinPlayUtils.getCalendar((String)rs.getString("start_time"));
                Calendar endRegisterTime = VinPlayUtils.getCalendar((String)rs.getString("end_register_time"));
                Calendar cancelTime = VinPlayUtils.getCalendar((String)rs.getString("cancel_time"));
                Calendar endTime = VinPlayUtils.getCalendar((String)rs.getString("end_time"));
                PokerTourState tourState = PokerTourState.getById(rs.getInt("tour_state"));
                PokerTourType tourType = PokerTourType.getById(rs.getInt("tour_type"));
                int level = rs.getInt("level");
                int ticket = rs.getInt("ticket");
                int countTimeUpLevel = rs.getInt("count_time_up_level");
                long fund = rs.getInt("fund");
                PokerTourInfo model = new PokerTourInfo(tourId, startTimeSchedule, endRegisterSchedule, tourState, tourType, startTime, endRegisterTime, cancelTime, endTime, level, ticket, countTimeUpLevel, fund);
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<PokerTourPlayer> getPokerTourPlayers(int tourId) throws ParseException, SQLException {
        ArrayList<PokerTourPlayer> res = new ArrayList<PokerTourPlayer>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_tour_user WHERE `tour_id`=?";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM poker_tour_user WHERE `tour_id`=?");
            stm.setInt(1, tourId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String nickname = rs.getString("nick_name");
                long currentChip = rs.getLong("current_chip");
                long lastChip = rs.getLong("last_chip");
                int ticketCount = rs.getInt("ticket_count");
                int outTourCount = rs.getInt("out_tour_count");
                long outTourTimestamp = rs.getLong("out_tour_time");
                int rank = rs.getInt("rank");
                int mark = rs.getInt("mark");
                int prize = rs.getInt("prize");
                PokerTourPlayer model = new PokerTourPlayer(tourId, nickname, currentChip, lastChip, ticketCount, outTourCount, outTourTimestamp, rank, mark, prize);
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public PokerFreeTicket createPokerFreeTicket(PokerFreeTicket freeTicket) throws ParseException, SQLException {
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "INSERT INTO poker_free_ticket(nick_name, ticket, tour_type, used, create_time, available_time, limit_time, create_type, add_info, is_bot) VALUES(?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO poker_free_ticket(nick_name, ticket, tour_type, used, create_time, available_time, limit_time, create_type, add_info, is_bot) VALUES(?,?,?,?,?,?,?,?,?,?)", 1);
            int param = 1;
            stm.setString(param++, freeTicket.nickname);
            stm.setInt(param++, freeTicket.ticket);
            stm.setInt(param++, freeTicket.tourType != null ? freeTicket.tourType.getId() : 0);
            stm.setBoolean(param++, freeTicket.used);
            Calendar cal = Calendar.getInstance();
            stm.setString(param++, VinPlayUtils.getDateTimeStr((Date)cal.getTime()));
            stm.setString(param++, freeTicket.availableTime == null ? null : VinPlayUtils.getDateTimeStr((Date)freeTicket.availableTime.getTime()));
            stm.setString(param++, freeTicket.limitTime == null ? null : VinPlayUtils.getDateTimeStr((Date)freeTicket.limitTime.getTime()));
            stm.setInt(param++, freeTicket.createType != null ? freeTicket.createType.getId() : 0);
            stm.setString(param++, freeTicket.addInfo);
            stm.setBoolean(param++, freeTicket.isBot);
            if (stm.executeUpdate() == 1) {
                try (ResultSet generatedKeys = stm.getGeneratedKeys();){
                    if (generatedKeys.next()) {
                        int id;
                        freeTicket.id = id = generatedKeys.getInt(1);
                        freeTicket.createTime = cal;
                    }
                }
            }
            stm.close();
        }
        return freeTicket;
    }

    @Override
    public boolean canCreateFreeTicket(String nickname, PokerTourType tourType, String startTime, String endTime) throws ParseException, SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT COUNT(*) as cnt FROM poker_free_ticket WHERE `nick_name`=? AND tour_type = ? AND create_time >= ? AND create_time <= ?";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt FROM poker_free_ticket WHERE `nick_name`=? AND tour_type = ? AND create_time >= ? AND create_time <= ?");
            int param = 1;
            stm.setString(param++, nickname);
            stm.setInt(param++, tourType.getId());
            stm.setString(param++, startTime);
            stm.setString(param++, endTime);
            ResultSet rs = stm.executeQuery();
            if (!rs.next() || rs.getInt("cnt") == 0) {
                res = true;
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public PokerFreeTicket usePokerFreeTicket(String nickname, int ticket, PokerTourType tourType, int tourId) throws ParseException, SQLException {
        PokerFreeTicket res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_free_ticket WHERE `nick_name`=? AND `ticket`=? AND tour_type=? AND used = ?  AND available_time <= CURRENT_TIMESTAMP() AND (limit_time is null OR limit_time >= CURRENT_TIMESTAMP())";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM poker_free_ticket WHERE `nick_name`=? AND `ticket`=? AND tour_type=? AND used = ?  AND available_time <= CURRENT_TIMESTAMP() AND (limit_time is null OR limit_time >= CURRENT_TIMESTAMP())");
            int param = 1;
            stm.setString(param++, nickname);
            stm.setInt(param++, ticket);
            stm.setInt(param++, tourType.getId());
            stm.setBoolean(param++, false);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String sql2 = "UPDATE poker_free_ticket SET used = ?, use_time = ?, tour_id =?  WHERE id=?";
                PreparedStatement stm2 = conn.prepareStatement("UPDATE poker_free_ticket SET used = ?, use_time = ?, tour_id =?  WHERE id=?");
                param = 1;
                stm2.setBoolean(param++, true);
                stm2.setString(param++, VinPlayUtils.getCurrentDateTime());
                stm2.setInt(param++, tourId);
                stm2.setInt(param++, id);
                if (stm2.executeUpdate() == 1) {
                    Calendar createTime = VinPlayUtils.getCalendar((String)rs.getString("create_time"));
                    Calendar availableTime = VinPlayUtils.getCalendar((String)rs.getString("available_time"));
                    Calendar limitTime = VinPlayUtils.getCalendar((String)rs.getString("limit_time"));
                    Calendar useTime = VinPlayUtils.getCalendar((String)rs.getString("use_time"));
                    PokerTourFreeCreateType createType = PokerTourFreeCreateType.getById(rs.getInt("create_type"));
                    String addInfo = rs.getString("add_info");
                    boolean isBot = rs.getBoolean("is_bot");
                    res = new PokerFreeTicket(id, nickname, ticket, tourType, true, createTime, availableTime, limitTime, createType, useTime, addInfo, isBot, tourId);
                    stm2.close();
                    break;
                }
                stm2.close();
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<PokerTicketCount> getPokerFreeTicket(String nickname) throws ParseException, SQLException {
        ArrayList<PokerTicketCount> res = new ArrayList<PokerTicketCount>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT COUNT(*) as cnt, ticket, tour_type FROM poker_free_ticket WHERE `nick_name`=? AND used = ? AND available_time <= CURRENT_TIMESTAMP() AND (limit_time is null OR limit_time >= CURRENT_TIMESTAMP()) GROUP BY ticket, tour_type ORDER BY ticket ASC";
            PreparedStatement stm = conn.prepareStatement("SELECT COUNT(*) as cnt, ticket, tour_type FROM poker_free_ticket WHERE `nick_name`=? AND used = ? AND available_time <= CURRENT_TIMESTAMP() AND (limit_time is null OR limit_time >= CURRENT_TIMESTAMP()) GROUP BY ticket, tour_type ORDER BY ticket ASC");
            int param = 1;
            stm.setString(param++, nickname);
            stm.setBoolean(param++, false);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int quantity = rs.getInt("cnt");
                int ticket = rs.getInt("ticket");
                int tourType = rs.getInt("tour_type");
                PokerTicketCount tk = new PokerTicketCount(ticket, tourType, quantity);
                res.add(tk);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<PokerTourInfoGeneral> getPokerTourListGeneral(Integer ticket, PokerTourState state, PokerTourType type, int page, int rows) throws ParseException, SQLException {
        ArrayList<PokerTourInfoGeneral> res = new ArrayList<PokerTourInfoGeneral>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_tour WHERE 1 = 1 ";
            if (ticket != null) {
                sql = sql + " AND ticket = ?";
            }
            sql = state != null ? (state == PokerTourState.TourNotReady ? sql + " AND tour_state = ? AND start_time_schedule > addtime(current_timestamp(), '01:00:00')" : (state == PokerTourState.Onschedule ? sql + " AND tour_state = ? AND start_time_schedule <= addtime(current_timestamp(), '01:00:00') AND start_time_schedule >= current_timestamp()" : sql + " AND tour_state = ? AND start_time_schedule <= addtime(current_timestamp(), '01:00:00')")) : sql + "AND ((tour_state <= 1 AND start_time_schedule >= current_timestamp()) OR tour_state > 1) AND start_time_schedule <= addtime(current_timestamp(), '01:00:00')";
            if (type != null) {
                sql = sql + " AND tour_type = ?";
            }
            sql = sql + " ORDER BY start_time_schedule DESC LIMIT ?, ?";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (ticket != null) {
                stm.setInt(param++, ticket);
            }
            if (state != null) {
                if (state == PokerTourState.TourNotReady) {
                    stm.setInt(param++, PokerTourState.Onschedule.getId());
                } else {
                    stm.setInt(param++, state.getId());
                }
            }
            if (type != null) {
                stm.setInt(param++, type.getId());
            }
            stm.setInt(param++, (page - 1) * rows);
            stm.setInt(param++, rows);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int tourState = 0;
                String startTimeSchedule = rs.getString("start_time_schedule");
                long timeSchedule = VinPlayUtils.getDateTime((String)startTimeSchedule).getTime();
                if (state == null) {
                    tourState = rs.getInt("tour_state");
                    if (tourState <= PokerTourState.Onschedule.getId() && startTimeSchedule != null && timeSchedule - System.currentTimeMillis() >= 3600000L) {
                        tourState = PokerTourState.TourNotReady.getId();
                    }
                } else {
                    tourState = state.getId();
                }
                int tourId = rs.getInt("id");
                String startTime = rs.getString("start_time");
                int tk = rs.getInt("ticket");
                long fund = rs.getLong("fund");
                int tourType = 0;
                tourType = type == null ? rs.getInt("tour_type") : type.getId();
                int userCount = 0;
                int ticketCount = 0;
                String sql2 = "SELECT COUNT(1) as u, SUM(ticket_count) as t FROM poker_tour_user WHERE tour_id = ? ";
                PreparedStatement stm2 = conn.prepareStatement("SELECT COUNT(1) as u, SUM(ticket_count) as t FROM poker_tour_user WHERE tour_id = ? ");
                stm2.setInt(1, tourId);
                ResultSet rs2 = stm2.executeQuery();
                if (rs2.next()) {
                    userCount = rs2.getInt("u");
                    ticketCount = rs2.getInt("t");
                }
                PokerTourInfoGeneral model = new PokerTourInfoGeneral(tourId, startTime, startTimeSchedule, tk, fund, tourState, tourType, userCount, ticketCount);
                res.add(model);
                rs2.close();
                stm2.close();
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public PokerTourInfoDetail getPokerTourDetail(int tourId) throws ParseException, SQLException {
        PokerTourInfoDetail res = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_tour WHERE id= ? ";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM poker_tour WHERE id= ? ");
            int param = 1;
            stm.setInt(param++, tourId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                int ticket = rs.getInt("ticket");
                long fund = rs.getLong("fund");
                int tourState = rs.getInt("tour_state");
                Calendar endTime = VinPlayUtils.getCalendar((String)rs.getString("end_time"));
                Calendar startTime = VinPlayUtils.getCalendar((String)rs.getString("start_time"));
                int userCount = 0;
                int ticketCount = 0;
                ArrayList<PokerTourUserDetail> playerList = new ArrayList<PokerTourUserDetail>();
                String sql2 = "SELECT * FROM poker_tour_user WHERE tour_id = ? ORDER BY rank ASC";
                PreparedStatement stm2 = conn.prepareStatement("SELECT * FROM poker_tour_user WHERE tour_id = ? ORDER BY rank ASC");
                stm2.setInt(1, tourId);
                ResultSet rs2 = stm2.executeQuery();
                while (rs2.next()) {
                    ++userCount;
                    ticketCount += rs2.getInt("ticket_count");
                    String nickname = rs2.getString("nick_name");
                    long currentChip = rs2.getLong("current_chip");
                    long lastChip = rs2.getLong("last_chip");
                    int rank = rs2.getInt("rank");
                    int mark = rs2.getInt("mark");
                    int prize = rs2.getInt("prize");
                    long outTourTimestamp = rs2.getLong("out_tour_time");
                    outTourTimestamp = outTourTimestamp == Long.MAX_VALUE && endTime != null ? endTime.getTimeInMillis() : (outTourTimestamp == 0L && startTime != null ? startTime.getTimeInMillis() : (outTourTimestamp *= 1000L));
                    PokerTourUserDetail model = new PokerTourUserDetail(rank, nickname, currentChip, lastChip, mark, prize, String.valueOf(outTourTimestamp));
                    playerList.add(model);
                }
                rs2.close();
                stm2.close();
                res = new PokerTourInfoDetail(tourId, ticket, fund, tourState, userCount, ticketCount, playerList);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public PokerFreeTicketStatistic getPokerFreeTicketStatistic(int id, String nickname, int createType, int amount, int status, int isBot, int isUse, int tourId, int tourType, String startTime, String endTime, int page, int rows) throws ParseException, SQLException {
        ArrayList<PokerFreeTicket> tickets = new ArrayList<PokerFreeTicket>();
        int total = 0;
        int totalAmount = 0;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM poker_free_ticket WHERE 1 = 1 ";
            String sqlCount = "SELECT COUNT(*) as total, SUM(ticket) as totalAmount FROM poker_free_ticket WHERE 1 = 1 ";
            String sqlWhere = "";
            if (id >= 0) {
                sqlWhere = sqlWhere + " AND id=?";
            }
            if (nickname != null && !nickname.isEmpty()) {
                sqlWhere = sqlWhere + " AND nick_name = ?";
            }
            if (createType >= 0) {
                sqlWhere = sqlWhere + " AND create_type = ?";
            }
            if (amount >= 0) {
                sqlWhere = sqlWhere + " AND ticket = ?";
            }
            if (isBot >= 0) {
                sqlWhere = sqlWhere + " AND is_bot = ?";
            }
            if (isUse >= 0) {
                sqlWhere = sqlWhere + " AND used = ?";
            }
            if (tourId >= 0) {
                sqlWhere = sqlWhere + " AND tour_id = ?";
            }
            if (tourType >= 0) {
                sqlWhere = sqlWhere + " AND tour_type = ?";
            }
            if (startTime != null && !startTime.isEmpty()) {
                sqlWhere = sqlWhere + " AND create_time >= ?";
            }
            if (endTime != null && !endTime.isEmpty()) {
                sqlWhere = sqlWhere + " AND create_time <= ?";
            }
            switch (status) {
                case 0: {
                    sqlWhere = sqlWhere + " AND available_time > CURRENT_TIMESTAMP()";
                    break;
                }
                case 1: {
                    sqlWhere = sqlWhere + " AND available_time <= CURRENT_TIMESTAMP() AND (limit_time is null OR limit_time >= CURRENT_TIMESTAMP())";
                    break;
                }
                case 2: {
                    sqlWhere = sqlWhere + " AND limit_time is not null AND limit_time < CURRENT_TIMESTAMP()";
                }
            }
            sql = sql + sqlWhere;
            sql = sql + " ORDER BY create_time DESC LIMIT ?, ?";
            sqlCount = sqlCount + sqlWhere;
            PreparedStatement stm = conn.prepareStatement(sql);
            PreparedStatement stmCount = conn.prepareStatement(sqlCount);
            int param = 1;
            int paramCount = 1;
            if (id >= 0) {
                stm.setInt(param++, id);
                stmCount.setInt(paramCount++, id);
            }
            if (nickname != null && !nickname.isEmpty()) {
                stm.setString(param++, nickname);
                stmCount.setString(paramCount++, nickname);
            }
            if (createType >= 0) {
                stm.setInt(param++, createType);
                stmCount.setInt(paramCount++, createType);
            }
            if (amount >= 0) {
                stm.setInt(param++, amount);
                stmCount.setInt(paramCount++, amount);
            }
            if (isBot >= 0) {
                stm.setBoolean(param++, isBot == 1);
                stmCount.setBoolean(paramCount++, isBot == 1);
            }
            if (isUse >= 0) {
                stm.setBoolean(param++, isUse == 1);
                stmCount.setBoolean(paramCount++, isUse == 1);
            }
            if (tourId >= 0) {
                stm.setInt(param++, tourId);
                stmCount.setInt(paramCount++, tourId);
            }
            if (tourType >= 0) {
                stm.setInt(param++, tourType);
                stmCount.setInt(paramCount++, tourType);
            }
            if (startTime != null && !startTime.isEmpty()) {
                stm.setString(param++, startTime);
                stmCount.setString(paramCount++, startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                stm.setString(param++, endTime);
                stmCount.setString(paramCount++, endTime);
            }
            stm.setInt(param++, (page - 1) * rows);
            stm.setInt(param++, rows);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int idTk = rs.getInt("id");
                int ticketTk = rs.getInt("ticket");
                PokerTourType tourTypeTk = PokerTourType.getById(rs.getInt("tour_type"));
                boolean useTk = rs.getBoolean("used");
                int tourIdTk = rs.getInt("tour_id");
                String nicknameTk = rs.getString("nick_name");
                Calendar createTime = VinPlayUtils.getCalendar((String)rs.getString("create_time"));
                Calendar availableTime = VinPlayUtils.getCalendar((String)rs.getString("available_time"));
                Calendar limitTime = VinPlayUtils.getCalendar((String)rs.getString("limit_time"));
                Calendar useTime = VinPlayUtils.getCalendar((String)rs.getString("use_time"));
                PokerTourFreeCreateType createTypeTk = PokerTourFreeCreateType.getById(rs.getInt("create_type"));
                String addInfo = rs.getString("add_info");
                boolean isBotTk = rs.getBoolean("is_bot");
                tickets.add(new PokerFreeTicket(idTk, nicknameTk, ticketTk, tourTypeTk, useTk, createTime, availableTime, limitTime, createTypeTk, useTime, addInfo, isBotTk, tourIdTk));
            }
            rs.close();
            stm.close();
            ResultSet rsCount = stmCount.executeQuery();
            if (rsCount.next()) {
                total = rsCount.getInt("total");
                totalAmount += rsCount.getInt("totalAmount");
            }
            rsCount.close();
            stmCount.close();
        }
        return new PokerFreeTicketStatistic(tickets, totalAmount, total);
    }

    @Override
    public int exportFreeCode(String gamename, int quantity, int amount, int codeType, Calendar expire, String creater, Set<String> codes) throws ParseException, SQLException {
        int packageId = 0;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
        try {
            conn.setAutoCommit(false);
            String now = VinPlayUtils.getCurrentDateTime();
            String sql = "INSERT INTO game_free_code_package(game_name, type, quantity, amount, expire, create_time, creater) VALUES(?,?,?,?,?,?,?)";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO game_free_code_package(game_name, type, quantity, amount, expire, create_time, creater) VALUES(?,?,?,?,?,?,?)", 1);
            int param = 1;
            stm.setString(param++, gamename);
            stm.setInt(param++, codeType);
            stm.setInt(param++, quantity);
            stm.setInt(param++, amount);
            stm.setString(param++, expire == null ? null : VinPlayUtils.getDateTimeStr((Date)expire.getTime()));
            stm.setString(param++, now);
            stm.setString(param++, creater);
            String sql2 = "INSERT INTO game_free_code_detail(code, package_id, game_name, type, amount, status, expire, create_time) VALUES(?,?,?,?,?,?,?,?)";
            PreparedStatement stm2 = conn.prepareStatement("INSERT INTO game_free_code_detail(code, package_id, game_name, type, amount, status, expire, create_time) VALUES(?,?,?,?,?,?,?,?)");
            if (stm.executeUpdate() == 1) {
                try (ResultSet generatedKeys = stm.getGeneratedKeys();){
                    if (generatedKeys.next()) {
                        packageId = generatedKeys.getInt(1);
                        for (String code : codes) {
                            stm2.clearParameters();
                            param = 1;
                            stm2.setString(param++, code);
                            stm2.setInt(param++, packageId);
                            stm2.setString(param++, gamename);
                            stm2.setInt(param++, codeType);
                            stm2.setInt(param++, amount);
                            stm2.setInt(param++, 0);
                            stm2.setString(param++, expire == null ? null : VinPlayUtils.getDateTimeStr((Date)expire.getTime()));
                            stm2.setString(param++, now);
                            stm2.executeUpdate();
                        }
                    }
                }
            }
            conn.commit();
            stm2.close();
            stm.close();
        }
        catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
        finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
        return packageId;
    }

    @Override
    public Set<String> getAllCode() throws SQLException {
        HashSet<String> res = new HashSet<String>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT code FROM game_free_code_detail";
            PreparedStatement stm = conn.prepareStatement("SELECT code FROM game_free_code_detail");
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                res.add(rs.getString("code"));
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<GameFreeCodePackage> getFreeCodePackage(int id, String gamename, int amount, int codeType, String startTime, String endTime, String creater) throws ParseException, SQLException {
        ArrayList<GameFreeCodePackage> res = new ArrayList<GameFreeCodePackage>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM game_free_code_package WHERE 1=1 ";
            if (id >= 0) {
                sql = sql + " AND id = ? ";
            }
            if (gamename != null && !gamename.isEmpty()) {
                sql = sql + " AND game_name = ? ";
            }
            if (amount >= 0) {
                sql = sql + " AND amount = ? ";
            }
            if (codeType >= 0) {
                sql = sql + " AND type = ? ";
            }
            if (startTime != null && !startTime.isEmpty()) {
                sql = sql + " AND create_time >=? ";
            }
            if (endTime != null && !endTime.isEmpty()) {
                sql = sql + " AND create_time <= ? ";
            }
            if (creater != null && !creater.isEmpty()) {
                sql = sql + " AND creater = ? ";
            }
            sql = sql + " ORDER BY create_time DESC";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (id >= 0) {
                stm.setInt(param++, id);
            }
            if (gamename != null && !gamename.isEmpty()) {
                stm.setString(param++, gamename);
            }
            if (amount >= 0) {
                stm.setInt(param++, amount);
            }
            if (codeType >= 0) {
                stm.setInt(param++, codeType);
            }
            if (startTime != null && !startTime.isEmpty()) {
                stm.setString(param++, startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                stm.setString(param++, endTime);
            }
            if (creater != null && !creater.isEmpty()) {
                stm.setString(param++, creater);
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int idM = rs.getInt("id");
                int typeM = rs.getInt("type");
                int quantityM = rs.getInt("quantity");
                int amountM = rs.getInt("amount");
                String gamenameM = rs.getString("game_name");
                Calendar expireM = VinPlayUtils.getCalendar((String)rs.getString("expire"));
                Calendar createTimeM = VinPlayUtils.getCalendar((String)rs.getString("create_time"));
                String createrM = rs.getString("creater");
                GameFreeCodePackage model = new GameFreeCodePackage(idM, gamenameM, typeM, quantityM, amountM, expireM, createTimeM, createrM);
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public List<GameFreeCodeDetail> getFreeCodeDetails(int id, String code, int packageId, String gamename, int amount, int codeType, int status, String nickname, String addInfo, String startTime, String endTime, int timeType) throws ParseException, SQLException {
        ArrayList<GameFreeCodeDetail> res = new ArrayList<GameFreeCodeDetail>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM game_free_code_detail WHERE 1=1 ";
            if (id >= 0) {
                sql = sql + " AND id = ? ";
            }
            if (packageId >= 0) {
                sql = sql + " AND package_id = ? ";
            }
            if (gamename != null && !gamename.isEmpty()) {
                sql = sql + " AND game_name = ? ";
            }
            if (code != null && !code.isEmpty()) {
                sql = sql + " AND code = ? ";
            }
            if (amount >= 0) {
                sql = sql + " AND amount = ? ";
            }
            if (codeType >= 0) {
                sql = sql + " AND type = ? ";
            }
            if (status >= 0) {
                sql = status == 4 ? sql + " AND status = ? AND expire is not null AND expire < CURRENT_TIMESTAMP()" : sql + " AND status = ? ";
            }
            if (nickname != null && !nickname.isEmpty()) {
                sql = sql + " AND nick_name =? ";
            }
            if (addInfo != null && !addInfo.isEmpty()) {
                sql = sql + " AND addInfo =? ";
            }
            if (startTime != null && !startTime.isEmpty()) {
                sql = timeType == 0 ? sql + " AND create_time >=? " : sql + " AND use_time is not null AND use_time >=? ";
            }
            if (endTime != null && !endTime.isEmpty()) {
                sql = timeType == 0 ? sql + " AND create_time <= ? " : sql + " AND use_time is not null AND use_time <=? ";
            }
            sql = sql + " ORDER BY create_time DESC";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (id >= 0) {
                stm.setInt(param++, id);
            }
            if (packageId >= 0) {
                stm.setInt(param++, packageId);
            }
            if (gamename != null && !gamename.isEmpty()) {
                stm.setString(param++, gamename);
            }
            if (code != null && !code.isEmpty()) {
                stm.setString(param++, code);
            }
            if (amount >= 0) {
                stm.setInt(param++, amount);
            }
            if (codeType >= 0) {
                stm.setInt(param++, codeType);
            }
            if (status >= 0) {
                if (status == 4) {
                    stm.setInt(param++, 1);
                } else {
                    stm.setInt(param++, status);
                }
            }
            if (nickname != null && !nickname.isEmpty()) {
                stm.setString(param++, nickname);
            }
            if (addInfo != null && !addInfo.isEmpty()) {
                stm.setString(param++, addInfo);
            }
            if (startTime != null && !startTime.isEmpty()) {
                stm.setString(param++, startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                stm.setString(param++, endTime);
            }
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int idM = rs.getInt("id");
                int packageIdM = rs.getInt("package_id");
                String codeM = rs.getString("code");
                String gamenameM = rs.getString("game_name");
                int typeM = rs.getInt("type");
                int amountM = rs.getInt("amount");
                int statusM = rs.getInt("status");
                Calendar expireM = VinPlayUtils.getCalendar((String)rs.getString("expire"));
                Calendar createTimeM = VinPlayUtils.getCalendar((String)rs.getString("create_time"));
                String nicknameM = rs.getString("nick_name");
                String addInfoM = rs.getString("add_info");
                Calendar useTimeM = VinPlayUtils.getCalendar((String)rs.getString("use_time"));
                if (statusM == 1 && expireM != null && expireM.getTime().getTime() < new Date().getTime()) {
                    statusM = 4;
                }
                GameFreeCodeDetail model = new GameFreeCodeDetail(idM, packageIdM, codeM, gamenameM, typeM, amountM, statusM, expireM, createTimeM, nicknameM, addInfoM, useTimeM);
                res.add(model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public GameFreeCodeDetail getFreeCodeDetail(String code, String gamename) throws ParseException, SQLException {
        GameFreeCodeDetail model = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT * FROM game_free_code_detail WHERE code=? AND game_name=? ";
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM game_free_code_detail WHERE code=? AND game_name=? ");
            int param = 1;
            stm.setString(param++, code);
            stm.setString(param++, gamename);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                int idM = rs.getInt("id");
                int packageIdM = rs.getInt("package_id");
                int typeM = rs.getInt("type");
                int amountM = rs.getInt("amount");
                int statusM = rs.getInt("status");
                Calendar expireM = VinPlayUtils.getCalendar((String)rs.getString("expire"));
                Calendar createTimeM = VinPlayUtils.getCalendar((String)rs.getString("create_time"));
                String nicknameM = rs.getString("nick_name");
                String addInfoM = rs.getString("add_info");
                Calendar useTimeM = VinPlayUtils.getCalendar((String)rs.getString("use_time"));
                model = new GameFreeCodeDetail(idM, packageIdM, code, gamename, typeM, amountM, statusM, expireM, createTimeM, nicknameM, addInfoM, useTimeM);
            }
            rs.close();
            stm.close();
        }
        return model;
    }

    @Override
    public boolean updateFreeCodeDetail(int status, String gamename, int id, String code, int packageId, int type, int amount, List<Integer> statusW) throws ParseException, SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "UPDATE game_free_code_detail SET status = ? WHERE 1=1 ";
            if (id >= 0) {
                sql = sql + " AND id = ? ";
            }
            if (packageId >= 0) {
                sql = sql + " AND package_id = ? ";
            }
            if (gamename != null && !gamename.isEmpty()) {
                sql = sql + " AND game_name = ? ";
            }
            if (code != null && !code.isEmpty()) {
                sql = sql + " AND code = ? ";
            }
            if (amount >= 0) {
                sql = sql + " AND amount = ? ";
            }
            if (type >= 0) {
                sql = sql + " AND type = ? ";
            }
            if (statusW.size() > 0) {
                sql = sql + " AND ( status = ? ";
                for (int i = 1; i < statusW.size(); ++i) {
                    sql = sql + " OR status = ? ";
                }
                sql = sql + ")";
            }
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setInt(param++, status);
            if (id >= 0) {
                stm.setInt(param++, id);
            }
            if (packageId >= 0) {
                stm.setInt(param++, packageId);
            }
            if (gamename != null && !gamename.isEmpty()) {
                stm.setString(param++, gamename);
            }
            if (code != null && !code.isEmpty()) {
                stm.setString(param++, code);
            }
            if (amount >= 0) {
                stm.setInt(param++, amount);
            }
            if (type >= 0) {
                stm.setInt(param++, type);
            }
            if (statusW.size() >= 0) {
                Iterator<Integer> iterator = statusW.iterator();
                while (iterator.hasNext()) {
                    int st = iterator.next();
                    stm.setInt(param++, st);
                }
            }
            stm.executeUpdate();
            stm.close();
            res = true;
        }
        return res;
    }

    @Override
    public PokerFreeTicket getTicketFromFreeCode(String code, String nickname, GameFreeCodeDetail freeCode, boolean isBot) throws ParseException, SQLException {
        Calendar cal = Calendar.getInstance();
        PokerFreeTicket freeTicket = new PokerFreeTicket(0, nickname, freeCode.amount, PokerTourType.getById(freeCode.type), false, cal, cal, null, PokerTourFreeCreateType.Code, null, code, isBot, 0);
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");
        try {
            conn.setAutoCommit(false);
            String sql = "INSERT INTO poker_free_ticket(nick_name, ticket, tour_type, used, create_time, available_time, limit_time, create_type, add_info, is_bot) VALUES(?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement stm = conn.prepareStatement("INSERT INTO poker_free_ticket(nick_name, ticket, tour_type, used, create_time, available_time, limit_time, create_type, add_info, is_bot) VALUES(?,?,?,?,?,?,?,?,?,?)", 1);
            int param = 1;
            stm.setString(param++, freeTicket.nickname);
            stm.setInt(param++, freeTicket.ticket);
            stm.setInt(param++, freeTicket.tourType != null ? freeTicket.tourType.getId() : 0);
            stm.setBoolean(param++, freeTicket.used);
            stm.setString(param++, VinPlayUtils.getDateTimeStr((Date)cal.getTime()));
            stm.setString(param++, freeTicket.availableTime == null ? null : VinPlayUtils.getDateTimeStr((Date)freeTicket.availableTime.getTime()));
            stm.setString(param++, freeTicket.limitTime == null ? null : VinPlayUtils.getDateTimeStr((Date)freeTicket.limitTime.getTime()));
            stm.setInt(param++, freeTicket.createType != null ? freeTicket.createType.getId() : 0);
            stm.setString(param++, freeTicket.addInfo);
            stm.setBoolean(param++, freeTicket.isBot);
            if (stm.executeUpdate() == 1) {
                try (ResultSet generatedKeys = stm.getGeneratedKeys();){
                    if (generatedKeys.next()) {
                        int id;
                        freeTicket.id = id = generatedKeys.getInt(1);
                        String sql2 = "UPDATE game_free_code_detail SET status=?, nick_name=?, add_info=?, use_time=? WHERE code=? ";
                        PreparedStatement stm2 = conn.prepareStatement("UPDATE game_free_code_detail SET status=?, nick_name=?, add_info=?, use_time=? WHERE code=? ");
                        param = 1;
                        stm2.setInt(param++, 3);
                        stm2.setString(param++, nickname);
                        stm2.setString(param++, String.valueOf(id));
                        stm2.setString(param++, VinPlayUtils.getCurrentDateTime());
                        stm2.setString(param++, code);
                        if (stm2.executeUpdate() == 1) {
                            conn.commit();
                        }
                        stm2.close();
                    }
                }
            }
            stm.close();
        }
        catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
        finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
        return freeTicket;
    }

    @Override
    public Map<Integer, GameFreeCodeStatistic> getFreeCodeStatistic(String gamename, String startTime, String endTime, int timeType) throws ParseException, SQLException {
        HashMap<Integer, GameFreeCodeStatistic> res = new HashMap<Integer, GameFreeCodeStatistic>();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "SELECT amount, status, expire, COUNT(*) as cnt FROM game_free_code_detail WHERE 1=1 ";
            if (gamename != null && !gamename.isEmpty()) {
                sql = sql + " AND game_name = ? ";
            }
            if (startTime != null && !startTime.isEmpty()) {
                sql = timeType == 0 ? sql + " AND create_time >=? " : sql + " AND use_time is not null AND use_time >=? ";
            }
            if (endTime != null && !endTime.isEmpty()) {
                sql = timeType == 0 ? sql + " AND create_time <= ? " : sql + " AND use_time is not null AND use_time <=? ";
            }
            sql = sql + " GROUP BY amount, status, expire ORDER BY amount";
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            if (gamename != null && !gamename.isEmpty()) {
                stm.setString(param++, gamename);
            }
            if (startTime != null && !startTime.isEmpty()) {
                stm.setString(param++, startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                stm.setString(param++, endTime);
            }
            ResultSet rs = stm.executeQuery();
            Date now = new Date();
            while (rs.next()) {
                int amount = rs.getInt("amount");
                int status = rs.getInt("status");
                Calendar expire = VinPlayUtils.getCalendar((String)rs.getString("expire"));
                int cnt = rs.getInt("cnt");
                GameFreeCodeStatistic model = null;
                model = res.containsKey(amount) ? (GameFreeCodeStatistic)res.get(amount) : new GameFreeCodeStatistic(amount, 0, 0, 0, 0, 0, 0);
                switch (status) {
                    case 0: {
                        GameFreeCodeStatistic gameFreeCodeStatistic = model;
                        gameFreeCodeStatistic.totalInactive += cnt;
                        break;
                    }
                    case 2: {
                        GameFreeCodeStatistic gameFreeCodeStatistic2 = model;
                        gameFreeCodeStatistic2.totalLocked += cnt;
                        break;
                    }
                    case 3: {
                        GameFreeCodeStatistic gameFreeCodeStatistic3 = model;
                        gameFreeCodeStatistic3.totalUsed += cnt;
                        break;
                    }
                    default: {
                        if (expire != null && expire.getTime().getTime() < now.getTime()) {
                            GameFreeCodeStatistic gameFreeCodeStatistic4 = model;
                            gameFreeCodeStatistic4.totalExpired += cnt;
                            break;
                        }
                        GameFreeCodeStatistic gameFreeCodeStatistic5 = model;
                        gameFreeCodeStatistic5.totalRemain += cnt;
                        break;
                    }
                }
                GameFreeCodeStatistic gameFreeCodeStatistic6 = model;
                gameFreeCodeStatistic6.total += cnt;
                res.put(amount, model);
            }
            rs.close();
            stm.close();
        }
        return res;
    }

    @Override
    public boolean clearUserInTour(int tourId) throws SQLException {
        boolean res = false;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_gamebai");){
            String sql = "DELETE FROM poker_tour_user WHERE tour_id=? ";
            PreparedStatement stm = conn.prepareStatement("DELETE FROM poker_tour_user WHERE tour_id=? ");
            int param = 1;
            stm.setInt(param++, tourId);
            stm.executeUpdate();
            stm.close();
            res = true;
        }
        return res;
    }
}

