package com.vinplay.dao.cmd.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.bson.Document;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.dao.cmd.CmdDao;
import com.vinplay.dao.wm.impl.WmDaoImpl;
import com.vinplay.dto.sportsbook.SportsbookMemberBetTicketInformationDetail;
import com.vinplay.dto.wm.CreateMemberReqDto;
import com.vinplay.item.SportsbookItem;
import com.vinplay.logic.CommonMethod;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameThirdPartyInit;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.rmq.RMQPublishTask;

public class CmdDaoImpl implements CmdDao{

    private static final Logger LOGGER = Logger.getLogger(CmdDaoImpl.class);
    private static final String COLLECTION_GAMERECORD = "cmdgamerecord";
    private static final long START_DATE = 621355968000000000L;
	private static final String GMT_8_FORMAT = "GMT+15:00";
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final Gson gsons = new Gson();
    
	@Override
	public List<Integer> maxCmdUser() throws Exception {
		List<Integer> result = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
			CallableStatement call = conn.prepareCall("CALL p_AG_MaxCmdUser(?,?)");
			call.registerOutParameter(1, java.sql.Types.INTEGER);
			call.registerOutParameter(2, java.sql.Types.INTEGER);
			call.execute();
			result.add(call.getInt(1));
			result.add(call.getInt(2));
		} catch (SQLException e) {
			LOGGER.error(e);
		}
		return result;
	}

	@Override
	public SportsbookItem mappingUserCmd(String nickName) throws SQLException {
        int param = 1;
        try ( Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
        	CallableStatement call = conn.prepareCall("CALL p_AG_Mappingcmduser(?)");
            call.setString(param++, nickName);
            ResultSet rs = call.executeQuery();
            if (rs.next()) {
                String cmdid = rs.getString("cmdid");
                if (cmdid != null && !"".equals(cmdid)) {
                    return new SportsbookItem(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        } 
        return null;
	}

	@Override
	public boolean generateCmdUser(String sportsBookId, int sportsBookCountId, String sportsBookUserName) throws SQLException {
	        String sql="INSERT INTO cmduser(cmdid,cmd_username,cmd_countid) VALUES(?,?,?) ";
	        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
	        	PreparedStatement stm = conn.prepareStatement(sql);
	        	stm.setString(1, sportsBookId);
	        	stm.setString(2, sportsBookUserName);
	        	stm.setInt(3, sportsBookCountId);
	        	int result = stm.executeUpdate();
				return result > 0;
	        } catch (SQLException e) {
				LOGGER.error("[TaskAutoCreateUserCmd] Insert cmdid to cmduser tbl error!" + e);
	            return false;
	        }
	}
	
//	@Override
//	public boolean generateCmdUser(String sportsBookId, int sportsBookCountId, String sportsBookUserName) throws SQLException {
//        CallableStatement call = null;
//        ResultSet rs = null;
//        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
//        int param = 1;
//        try {
//            call = conn.prepareCall("CALL p_AG_GeneralCmdUser(?,?,?)");
//            call.setString(param++, sportsBookId);
//            call.setString(param++, sportsBookUserName);
//            call.setInt(param++, sportsBookCountId);
//            rs = call.executeQuery();
//            if (rs.next()) {
//                String wmid = rs.getString("cmdid");
//                if (wmid != null && !"".equals(wmid)) {
//                    LOGGER.info("[TaskAutoCreateUserCmd] create cmdid to cmduser tbl ok!");
//                    return true;
//                }
//            }
//            LOGGER.error("[TaskAutoCreateUserCmd] Insert cmdid to cmduser tbl error!");
//        } catch (SQLException e) {
//            LOGGER.error(e);
//            return false;
//        } finally {
//            if (rs != null) {
//                rs.close();
//            }
//            if (call != null) {
//                call.close();
//            }
//            if (conn != null) {
//                conn.close();
//            }
//        }
//        return false;
//    }

	private static String convertLongToDate(long dateValue) {
		long currentTime = dateValue - START_DATE;
		DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
		formatter.setTimeZone(TimeZone.getTimeZone("Asia/Bangkok"));
		String fromTime = formatter.format(new Date(currentTime / 10000)); // GMT+08:00
		String utc7Plus = CommonMethod.convertTime(fromTime, GMT_8_FORMAT, DATE_FORMAT); // UTC+7
		return utc7Plus;
	}
	
	@Override
	public boolean saveCmdBetLog(SportsbookMemberBetTicketInformationDetail betDetail) throws Exception {
		String betDate = convertLongToDate(betDetail.getTransDate());
		String settledDate = convertLongToDate(betDetail.getStateUpdateTs());
		double winAmount = betDetail.getWinAmount() * GameThirdPartyInit.SPORTS_BOOK_RATE;
		double betAmount = betDetail.getBetAmount() * GameThirdPartyInit.SPORTS_BOOK_RATE;
		String cmdId = betDetail.getSourceName();
		
		LOGGER.info("[TaskAutoCollectSportsbookBetLogs] starting save bet logs: " + gsons.toJson(betDetail));
		SportsbookItem cmdUser = findUserByCmdId(cmdId);
        if (cmdUser == null) {
            return false;
        }
        double p_odds = betDetail.getOdds();
        Double v_realbet = 0d;
        
		if (p_odds > 0)
			v_realbet = betAmount;
		else
			v_realbet = betAmount * p_odds * (-1);
		
        MongoDatabase db = MongoDBConnectionFactory.getDB();
		try {
			MongoCollection<Document> col = db.getCollection(COLLECTION_GAMERECORD);
			Document conditions = new Document();
			conditions.put("matchid", betDetail.getMatchID());
			conditions.put("referenceno", betDetail.getReferenceNo());
			conditions.put("sourcename", betDetail.getSourceName());
			conditions.put("nick_name", cmdUser.getLoginname());
			long count = col.count(conditions);
			
			BasicDBObject doc = new BasicDBObject();
			doc.append("id", betDetail.getId());
			doc.append("sourcename", cmdId);
			doc.append("referenceno", betDetail.getReferenceNo());
			doc.append("soctransid", betDetail.getSocTransId());
			doc.append("isfirsthalf", betDetail.isIsFirstHalf());
			doc.append("transdate", betDetail.getTransDate());
			doc.append("ishomegive", betDetail.isIsHomeGive());
			doc.append("isbethome", betDetail.isIsBetHome());
			doc.append("betamount", betAmount);
			doc.append("outstanding", betDetail.getOutstanding());
			doc.append("hdp", betDetail.getHdp());
			doc.append("odds", betDetail.getOdds());
			doc.append("currency", betDetail.getCurrency());
			doc.append("winamount", winAmount);
			doc.append("exchangerate", betDetail.getExchangeRate());
			doc.append("winlosestatus", betDetail.getWinLoseStatus());
			doc.append("transtype", betDetail.getTransType());
			doc.append("dangerstatus", betDetail.getDangerStatus());
			doc.append("memcommission", betDetail.getMemCommission());
			doc.append("betip", betDetail.getBetIp());
			doc.append("homescore", betDetail.getHomeScore());
			doc.append("awayscore", betDetail.getAwayScore());
			doc.append("runhomescore", betDetail.getRunHomeScore());
			doc.append("runawayscore", betDetail.getRunAwayScore());
			doc.append("isrunning", betDetail.isIsRunning());
			doc.append("rejectreason", betDetail.getRejectReason());
			doc.append("sporttype", betDetail.getSportType());
			doc.append("choice", betDetail.getChoice());
			doc.append("workingdate", betDetail.getWorkingDate());
			doc.append("oddstype", betDetail.getOddsType());
			doc.append("matchdate", betDetail.getMatchDate());
			doc.append("hometeamid", betDetail.getHomeTeamId());
			doc.append("awayteamid", betDetail.getAwayTeamId());
			doc.append("leagueid", betDetail.getLeagueId());
			doc.append("specialid", betDetail.getSpecialId());
			doc.append("statuschange", betDetail.getStatusChange());
			doc.append("stateupdatets", betDetail.getStateUpdateTs());
			doc.append("memcommissionset", betDetail.getMemCommissionSet());
			doc.append("iscashout", betDetail.isIsCashOut());
			doc.append("cashouttotal", betDetail.getCashOutTotal());
			doc.append("cashouttakeback", betDetail.getCashOutTakeBack());
			doc.append("cashoutwinloseamount", betDetail.getCashOutWinLoseAmount());
			doc.append("betsource", betDetail.getBetSource());
			doc.append("aosexcluding", betDetail.getAOSExcluding());
			doc.append("mmrpercent", betDetail.getMMRPercent());
			doc.append("matchid", betDetail.getMatchID());
			doc.append("matchgroupid", betDetail.getMatchGroupID());
			doc.append("betremarks", betDetail.getBetRemarks());
			doc.append("isspecial", betDetail.isIsSpecial());
			doc.append("betdate", betDate);
			doc.append("settleddate", settledDate);
			doc.append("nick_name", cmdUser.getLoginname());
			doc.append("realBet", v_realbet);
            if (count > 0) {
                // update
                col.updateOne(conditions, new Document("$set", doc));
                LOGGER.info("[TaskCollectCMDLog] Process update log success ! ,id = " + betDetail.getId());
                if (winAmount > 0) {
                	UserService userService = new UserServiceImpl();
                    UserModel userModel = userService.getUserByNickName(cmdUser.getLoginname());
                    LogMoneyUserMessage message = new LogMoneyUserMessage(userModel.getId(), cmdUser.getLoginname(), "CMD",
                            Games.CMD_GAMES.getId() + "", 0,
                            Math.abs((long) winAmount), "vin",
                            "", 0, false, false);
                    message.setReferralCode(userModel.getReferralCode());
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                }
            } else {
                // insert
                col.insertOne(new Document(doc));
                LOGGER.info("[TaskCollectCMDLog]  Process insert log success! ,id= " + betDetail.getId());
                UserService userService = new UserServiceImpl();
                if (v_realbet != 0) {
                    UserModel userModel = userService.getUserByNickName(cmdUser.getLoginname());
                    LogMoneyUserMessage message = new LogMoneyUserMessage(userModel.getId(), cmdUser.getLoginname(), "CMD",
                            Games.CMD_GAMES.getId() + "", 0,
                            -Math.abs(v_realbet.longValue()), "vin",
                            "", 0, false, false);
                    message.setReferralCode(userModel.getReferralCode());
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                }

                if (winAmount > 0) {
                    UserModel userModel = userService.getUserByNickName(cmdUser.getLoginname());
                    LogMoneyUserMessage message = new LogMoneyUserMessage(userModel.getId(), cmdUser.getLoginname(), "CMD",
                            Games.CMD_GAMES.getId() + "", 0,
                            Math.abs((long) winAmount), "vin",
                            "", 0, false, false);
                    message.setReferralCode(userModel.getReferralCode());
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                }
            }
            LOGGER.info("[TaskCollectCMDLog] saveBetLog success");
            return true;
        } catch (Exception ex) {
            LOGGER.error("[TaskCollectCMDLog] Process saving log had error! ", ex);
            return false;
        }
	}

	@Override
	public SportsbookItem findUserByNickName(String nickName) throws SQLException {
		 SportsbookItem cmd = null;
	        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
	            String sql = "SELECT * FROM cmduser WHERE nick_name=?";
	            PreparedStatement stm = conn.prepareStatement(sql);
	            stm.setString(1, nickName);
	            ResultSet rs = stm.executeQuery();
	            if (rs.next()) {
	            	cmd = new SportsbookItem(nickName, rs.getInt("cmd_countid"), rs.getString("cmdid"),
							rs.getString("cmd_username"));
	            }
	            rs.close();
	            stm.close();
	        } catch (Exception e) {
	            LOGGER.error(e);
	        }
	        return cmd;
	}

	@Override
	public SportsbookItem findUserByCmdId(String cmdid) throws SQLException {
		SportsbookItem cmd = null;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			String sql = "SELECT * FROM cmduser WHERE cmdid=?";
			PreparedStatement stm = conn.prepareStatement(sql);
			stm.setString(1, cmdid);
			ResultSet rs = stm.executeQuery();
			if (rs.next()) {
				cmd = new SportsbookItem(rs.getString("nick_name"), rs.getInt("cmd_countid"), cmdid,
						rs.getString("cmd_username"));
			}
			rs.close();
			stm.close();
		} catch (Exception e) {
			LOGGER.error(e);
		}
		return cmd;
	}

}
