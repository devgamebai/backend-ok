package com.vinplay.dao.wm.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.rmq.RMQPublishTask;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dao.wm.WmDao;
import com.vinplay.dto.wm.CreateMemberReqDto;
import com.vinplay.dto.wm.GetDateTimeReportResult;
import com.vinplay.item.WMUserItem;
import com.vinplay.logic.CommonMethod;
import com.vinplay.service.GameDaoService;
import com.vinplay.service.impl.GameDaoServiceImpl;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;

public class WmDaoImpl implements WmDao {

    private static final Logger LOGGER = Logger.getLogger(WmDaoImpl.class);

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final String COLLECTION_GAMERECORD = "wmgamerecord";

	@Override
	public List<Integer> maxWmUser() throws Exception {
		List<Integer> result = new ArrayList<>();
		CallableStatement call = null;
		Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
		try {
			call = conn.prepareCall("CALL p_AG_MaxWmUser(?,?)");
			call.registerOutParameter(1, java.sql.Types.INTEGER);
			call.registerOutParameter(2, java.sql.Types.INTEGER);
			call.execute();
			result.add(call.getInt(1));
			result.add(call.getInt(2));
		} catch (SQLException e) {
			LOGGER.error(e);
		} finally {
			if (call != null) {
				call.close();
			}
			if (conn != null) {
				conn.close();
			}
		}
		return result;
	}

    @Override
    public boolean generateWmUser(CreateMemberReqDto reqDto, int wmcountid) throws Exception {
        int param = 1;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        		 CallableStatement call = conn.prepareCall("CALL p_AG_GeneralWmUser(?,?,?,?)");){
            call.setString(param++, reqDto.getUser());
            call.setString(param++, reqDto.getUsername());
            call.setString(param++, reqDto.getPassword());
            call.setInt(param++, wmcountid);
            int rs = call.executeUpdate();
			if (rs > 0) {
				LOGGER.info("[TaskAutoCreateUserWm] create wmid to wmuser tbl ok!");
                return true;
            }
            LOGGER.error("[TaskAutoCreateUserWm] Insert wmid to wmuser tbl error!");
        } catch (SQLException e) {
            LOGGER.error(e);
            return false;
        }
        return false;
    }

    @Override
    public boolean saveWMBetLog(GetDateTimeReportResult data) throws Exception {
        long betID = Long.parseLong(data.getBetId());
        String userID = data.getUser() == null ? "" : data.getUser();
        String betTime = data.getBetTime() == null ? "" : data.getBetTime();
        String setTime = data.getSettime() == null ? "" : data.getSettime();
        Double bet = data.getBet() == null ? 0.0 : Double.valueOf(data.getBet()) * GameThirdPartyInit.WM_RATE;
        Double validBet = data.getValidbet() == null ? 0.0 : Double.valueOf(data.getValidbet()) * GameThirdPartyInit.WM_RATE;
        Double winLoss = data.getWinLoss() == null ? 0.0 : Double.valueOf(data.getWinLoss()) * GameThirdPartyInit.WM_RATE;
        Double water = data.getWater() == null ? 0.0 : Double.valueOf(data.getWater()) * GameThirdPartyInit.WM_RATE;
        Double result = data.getResult() == null ? 0.0 : Double.valueOf(data.getResult()) * GameThirdPartyInit.WM_RATE;
        String betCode = data.getBetCode() == null ? "" : data.getBetCode();
        String betResult = data.getBetResult() == null ? "" : data.getBetResult();
        Double waterBet = data.getWaterbet() == null ? 0.0 : Double.valueOf(data.getWaterbet()) * GameThirdPartyInit.WM_RATE;
        int gameID = data.getGid();
        String ip = data.getIp() == null ? "" : data.getIp();
        int event = data.getEvent();
        int eventChild = data.getEventChild();
        int tableID = data.getTableId();
        String gameResult = data.getGameResult() == null ? "" : data.getGameResult();
        String gameName = data.getGname() == null ? "" : data.getGname();
        int commission = data.getCommission();
        String reset = data.getReset() == null ? "" : data.getReset();

        String saveBetTime = "";
        String saveSetTime = "";
        if (StringUtils.isNotEmpty(betTime)) {
            saveBetTime = CommonMethod.convertToCurrentSysTime(CommonMethod.convertFromThirdPartiesTimeZoneToUTC(
                    betTime, GameThirdPartyInit.WM_TIMEZONE, DATE_TIME_FORMAT), DATE_TIME_FORMAT);
        }
        if (StringUtils.isNotEmpty(setTime)) {
            saveSetTime = CommonMethod.convertToCurrentSysTime(CommonMethod.convertFromThirdPartiesTimeZoneToUTC(
                    setTime, GameThirdPartyInit.WM_TIMEZONE, DATE_TIME_FORMAT), DATE_TIME_FORMAT);
        }
        WMUserItem wmUser = findWmUserByWmId(userID);
        if (wmUser == null) {
            return false;
        }
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        try {
            MongoCollection<Document> col = db.getCollection(COLLECTION_GAMERECORD);
            Document conditions = new Document();
            conditions.put("betid", betID);
            long count = col.count(conditions);
            BasicDBObject doc = new BasicDBObject();
            doc.append("betid", betID);
            doc.append("nick_name", wmUser.getLoginname());
            doc.append("user", userID);
            doc.append("bettime", saveBetTime);
            doc.append("settime", saveSetTime);
            doc.append("rootbettime", betTime);
            doc.append("rootsettime", setTime);
            doc.append("bet", bet);
            doc.append("validbet", validBet);
            doc.append("winloss", winLoss);
            doc.append("water", water);
            doc.append("waterbet", waterBet);
            doc.append("result", result);
            doc.append("betcode", betCode);
            doc.append("betresult", betResult);
            doc.append("gameid", gameID);
            doc.append("ip", ip);
            doc.append("event", event);
            doc.append("eventchild", eventChild);
            doc.append("tableid", tableID);
            doc.append("gameresult", gameResult);
            doc.append("gamename", gameName);
            doc.append("commission", commission);
            doc.append("reset", reset);
            if (count > 0) {
                // update
                col.updateOne(conditions, new Document("$set", doc));
                LOGGER.info("[TaskCollectWMLog] Process update log success ! ,betID = " + betID);
            } else {
                // insert
                col.insertOne(new Document(doc));
                LOGGER.info("[TaskCollectWMLog]  Process insert log success! ,betID= " + betID);
                UserService userService = new UserServiceImpl();
                long svalidBet = validBet.longValue();
                long spayout = winLoss.longValue();

                if (svalidBet != 0) {
                    UserModel userModel = userService.getUserByNickName(wmUser.getLoginname());
                    LogMoneyUserMessage message = new LogMoneyUserMessage(userModel.getId(), wmUser.getLoginname(), "WM",
                            Games.WM_GAMES.getId() + "", 0,
                            -Math.abs(svalidBet), "vin",
                            "", 0, false, false);
                    message.setReferralCode(userModel.getReferralCode());
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                }

                if (spayout > 0) {
                    UserModel userModel = userService.getUserByNickName(wmUser.getLoginname());
                    LogMoneyUserMessage message = new LogMoneyUserMessage(userModel.getId(), wmUser.getLoginname(), "WM",
                            Games.WM_GAMES.getId() + "", 0,
                            Math.abs(spayout), "vin",
                            "", 0, false, false);
                    message.setReferralCode(userModel.getReferralCode());
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                }
            }

            return true;
        } catch (Exception ex) {
            LOGGER.error("[TaskCollectWMLog] Process saving log had error! ", ex);
            return false;
        }
    }

    @Override
    public WMUserItem findWmUserByNickName(String nickName) throws SQLException {
        WMUserItem wm = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
            String sql = "SELECT * FROM wmuser WHERE nick_name=?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, nickName);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                wm = new WMUserItem(rs.getString("wmid"), nickName, rs.getString("username"), rs.getString("password"),
                        rs.getInt("wmcountid"));
            }else {
            	//add user 
            	return mappingUserWm(nickName);//update nickName
			}
            rs.close();
            stm.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return wm;
    }

    @Override
    public WMUserItem findWmUserByWmId(String wmId) throws SQLException {
        WMUserItem wm = null;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
            String sql = "SELECT * FROM wmuser WHERE wmid=?";
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, wmId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                wm = new WMUserItem(wmId, rs.getString("nick_name"), rs.getString("username"), rs.getString("password"),
                        rs.getInt("wmcountid"));
            }
            rs.close();
            stm.close();
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return wm;
    }

    @Override
    public WMUserItem mappingUserWm(String nickName) throws SQLException {
        CallableStatement call = null;
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        ResultSet rs = null;
        int param = 1;
        try {
            call = conn.prepareCall("CALL p_AG_MappingWMUser(?)");
            call.setString(param++, nickName);
            rs = call.executeQuery();
            if (rs.next()) {
                String wmid = rs.getString("wmid");
                if (wmid != null && !"".equals(wmid)) {
                    return new WMUserItem(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (call != null) {
                call.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
        return null;
    }

}
