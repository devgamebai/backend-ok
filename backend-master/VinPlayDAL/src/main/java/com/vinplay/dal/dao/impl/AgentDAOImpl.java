/*
 * Decompiled with CFR 0.144.
 *
 * Could not load the following classes:
 *  com.mongodb.BasicDBList
 *  com.mongodb.BasicDBObject
 *  com.mongodb.Block
 *  com.mongodb.client.AggregateIterable
 *  com.mongodb.client.FindIterable
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.mongodb.client.result.UpdateResult
 *  com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage
 *  com.vinplay.vbee.common.models.AgentModel
 *  com.vinplay.vbee.common.models.cache.AgentDSModel
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  com.vinplay.vbee.common.pools.ConnectionPool
 *  com.vinplay.vbee.common.response.AgentResponse
 *  com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse
 *  com.vinplay.vbee.common.response.TranferAgentResponse
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.bson.Document
 *  org.bson.conversions.Bson
 */
package com.vinplay.dal.dao.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.*;
import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.entities.agent.*;
import com.vinplay.dal.utils.DataUtils;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage;
import com.vinplay.vbee.common.models.AgentModel;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.models.cache.AgentDSModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse;
import com.vinplay.vbee.common.response.TranferAgentResponse;
import com.vinplay.vbee.common.utils.DateTimeUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AgentDAOImpl implements AgentDAO {
	private static final Logger logger = Logger.getLogger("backend");

	@Override
	public Map<String, Object> loginSystemAgent(String username, String password) throws SQLException {
		Map<String, Object> map = new HashMap<>();
		UserAgentLoginModel userAgentModel = new UserAgentLoginModel();
		String mess = "Sai tên đăng nhập hoặc mật khẩu.";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT id, username, nickname, nameagent, address, phone, email, active, site, level, login_times, last_login_time, code "
					+ "FROM vinplay_admin.useragent WHERE username = ? and password = ?";
			String updateSql = "UPDATE vinplay_admin.useragent SET last_login_time = ?, login_times = login_times+1 WHERE username = ? ";
			try {
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setString(1, username);
				stmt.setString(2, password);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					if (rs.getInt("active") == 0) {
						map.put("user", userAgentModel);
						map.put("mess", "Tài khoản bị khóa");
						return map;
					}
					userAgentModel.setId(rs.getInt("id"));
					userAgentModel.setUsername(rs.getString("username"));
					userAgentModel.setNickname(rs.getString("nickname"));
					userAgentModel.setNameagent(rs.getString("nameagent"));
					userAgentModel.setAddress(rs.getString("address"));
					userAgentModel.setPhone(rs.getString("phone"));
					userAgentModel.setEmail(rs.getString("email"));
					userAgentModel.setActive(rs.getInt("active"));
					userAgentModel.setSite(rs.getString("site"));
					userAgentModel.setLevel(rs.getInt("level"));
					userAgentModel.setLogin_times(rs.getInt("login_times"));
					userAgentModel.setLast_login_time(rs.getDate("last_login_time"));
					userAgentModel.setCode(rs.getString("code"));

					mess = "Đăng nhập thành công";
					map.put("user", userAgentModel);
					map.put("mess", mess);
					// update lastloginTime
					PreparedStatement stmt1 = conn.prepareStatement(updateSql);
					Date date = new Date();
					stmt1.setString(1, new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(date));
					stmt1.setString(2, username);
					stmt1.executeUpdate();
					stmt1.close();
					return map;
				}

				rs.close();
			} catch (Exception e) {
				throw e;
			} finally {
				conn.close();
			}
			map.put("user", userAgentModel);
			map.put("mess", mess);
			return map;
		}
	}

	public boolean insertRefCodeAgent(int id, String refCode) throws SQLException {
		int value = 0;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "INSERT INTO vinplay.agency_code (user_id,referral_code)  VALUES  (?, ?)";
			try {
				PreparedStatement stmt = conn.prepareStatement(sql);
				int param = 1;
				stmt.setInt(param++, id);
				stmt.setString(param++, refCode);
				value = stmt.executeUpdate();
			} finally {
				try {
					conn.close();
				} catch (Exception e) {

				}
			}
			return value == 1;
		}
	}

	@Override
	public boolean updateUserIsDaily(String userName) throws SQLException {
		int value = 0;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "UPDATE vinplay.users SET dai_ly = ? WHERE nick_name = ?";
			try {
				PreparedStatement stmt = conn.prepareStatement(sql);
				int param = 1;
				stmt.setInt(param++, 1);
				stmt.setString(param++, userName);
				value = stmt.executeUpdate();
			} catch (Exception e) {
				throw e;
			} finally {
				try {
					conn.close();
				} catch (Exception e) {

				}
			}
			return value == 1;
		}

	}

	@Override
	public CheckDataAgentModel getUserID(String userName) throws SQLException {
		CheckDataAgentModel checkDataAgentModel = new CheckDataAgentModel();

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT id, dai_ly, is_bot FROM vinplay.users WHERE nick_name = ?";
			try {
				PreparedStatement stmt = conn.prepareStatement(sql);
				int param = 1;
				stmt.setString(param++, userName);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					checkDataAgentModel.id = rs.getInt("id");
					checkDataAgentModel.dai_ly = rs.getInt("dai_ly");
					checkDataAgentModel.is_bot = rs.getInt("is_bot");
				}
				rs.close();
			} catch (Exception e) {
				throw e;
			} finally {
				try {
					conn.close();
				} catch (Exception e) {

				}
			}
			return checkDataAgentModel;
		}
	}

	@Override
	public boolean isAgentExit(int userID, String agencyCode) throws SQLException {
		boolean valueReturn = false;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT * FROM vinplay.agency_code WHERE referral_code = ? OR user_id = ?";
			try {
				PreparedStatement stmt = conn.prepareStatement(sql);
				int param = 1;
				stmt.setString(param++, agencyCode);
				stmt.setInt(param++, userID);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					valueReturn = true;
				}
				rs.close();
			} catch (Exception e) {

			} finally {
				try {
					conn.close();
				} catch (Exception e) {

				}
			}
		}

		return valueReturn;
	}

	@Override
	public List<AgentResponse> listAgent() throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT nameagent,nickname,phone,address,parentid,`show`,`active`,`order`,`facebook`,commission_rate FROM useragent WHERE status='D' and parentid=-1 and `show`=1 and active=1 order by `order` asc";
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT nameagent,nickname,phone,address,parentid,`show`,`active`,`order`,`facebook`,commission_rate FROM useragent WHERE status='D' and parentid=-1 and `show`=1 and active=1 order by `order` asc");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.parentid = rs.getInt("parentid");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.orderNo = rs.getInt("order");
				agent.facebook = rs.getString("facebook");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(agent);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return results;
		} catch (SQLException e) {
			throw e;
		}

	}

	@Override
	public List<AgentResponse> listAgentByLevel(int parentId, int level) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
//            String sql = "SELECT nameagent,nickname,phone,address,parentid,`show`,`active`,`order`,`facebook` FROM useragent WHERE status='D' and parentid=-1 and `show`=1 and active=1 order by `order` asc";
			PreparedStatement stmt = conn.prepareStatement("SELECT id,nameagent,nickname,phone,address,parentid,code,`show`,`active`,`order`,`facebook`, `telegram`, `zalo`,commission_rate FROM useragent WHERE level = ? "
					+ ((parentId != -1) ? " AND parentid=?" : "")
					+ " order by `order` asc");
			stmt.setInt(1, level);
			if (parentId != -1) {
				stmt.setInt(2, parentId);
			}
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.id = rs.getInt("id");
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.parentid = rs.getInt("parentid");
				agent.code = rs.getString("code");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.orderNo = rs.getInt("order");
				agent.facebook = rs.getString("facebook");
				agent.telegram = rs.getString("telegram");
				agent.zalo = rs.getString("zalo");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(agent);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return results;
		} catch (SQLException e) {
			throw e;
		}

	}

	@Override
	public List<AgentResponse> listAgentByParent(int parentId) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
//            String sql = "SELECT nameagent,nickname,phone,address,parentid,`show`,`active`,`order`,`facebook` FROM useragent WHERE status='D' and parentid=-1 and `show`=1 and active=1 order by `order` asc";
			PreparedStatement stmt = conn.prepareStatement("SELECT nameagent,nickname,phone,address,parentid,code,`show`,`active`,`order`,`facebook`, `telegram`, `zalo`,commission_rate FROM useragent WHERE parentid=? and `show`=1 and active=1 order by `order` asc");
			stmt.setInt(1, parentId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.parentid = rs.getInt("parentid");
				agent.code = rs.getString("code");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.orderNo = rs.getInt("order");
				agent.facebook = rs.getString("facebook");
				agent.telegram = rs.getString("telegram");
				agent.zalo = rs.getString("zalo");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(agent);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return results;
		} catch (SQLException e) {
			throw e;
		}

	}

	@Override
	public List<AgentResponse> listAllAgent() throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			PreparedStatement stmt = conn.prepareStatement("SELECT id,nameagent,nickname,phone,address,parentid,code,`show`,`active`,`order`,`facebook`, `telegram`, `zalo`,commission_rate FROM useragent order by `order` asc");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.id = rs.getInt("id");
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.parentid = rs.getInt("parentid");
				agent.code = rs.getString("code");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.orderNo = rs.getInt("order");
				agent.facebook = rs.getString("facebook");
				agent.telegram = rs.getString("telegram");
				agent.zalo = rs.getString("zalo");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(agent);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return results;
		} catch (SQLException e) {
			throw e;
		}

	}

	@Override
	public List<UserAgentModel> listUserAgent(String username, String nickname, String password, String nameagent,
			String address, String phone, String email, String facebook, String key, String status, Integer parentid,
			String namebank, String nameaccount, String numberaccount, Integer show, Integer active, Date createtime,
			Date updatetime, Integer order, Integer sms, Integer percent_bonus_vincard, String site,
			Date last_login_time, Integer login_times, Integer level, String code, int page, int maxItem)
			throws SQLException {
		List<UserAgentModel> results = new ArrayList<UserAgentModel>();
		page = (page - 1) < 0 ? 0 : (page - 1);
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT * FROM vinplay_admin.useragent WHERE (1=1) and (parentid = -1 or parentid is null) "
					+ (username == null || username.isEmpty() ? "" : (" and username = ?"))
					+ (nickname == null || nickname.isEmpty() ? "" : (" and nickname = ?"))
					+ (nameagent == null || nameagent.isEmpty() ? "" : (" and nameagent = ?"))
					+ (address == null || address.isEmpty() ? "" : (" and address = ?"))
					+ (phone == null || phone.isEmpty() ? "" : (" and phone = ?"))
					+ (email == null || email.isEmpty() ? "" : (" and email = ?"))
					+ (facebook == null || facebook.isEmpty() ? "" : (" and facebook = ?"))
					+ (status == null || status.isEmpty() ? "" : (" and status = ?"))
					+ (namebank == null || namebank.isEmpty() ? "" : (" and namebank = ?"))
					+ (show == null ? "" : (" and show = ?")) + (active == null ? "" : (" and active = ?"))
					+ " order by id desc limit ?,?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(username == null || username.isEmpty())) {
				stmt.setString(index++, username);
			}
			if (!(nickname == null || nickname.isEmpty())) {
				stmt.setString(index++, nickname);
			}
			if (!(nameagent == null || nameagent.isEmpty())) {
				stmt.setString(index++, nameagent);
			}
			if (!(address == null || address.isEmpty())) {
				stmt.setString(index++, address);
			}
			if (!(phone == null || phone.isEmpty())) {
				stmt.setString(index++, phone);
			}
			if (!(email == null || email.isEmpty())) {
				stmt.setString(index++, email);
			}
			if (!(facebook == null || facebook.isEmpty())) {
				stmt.setString(index++, facebook);
			}
			if (!(status == null || status.isEmpty())) {
				stmt.setString(index++, status);
			}
			if (!(namebank == null || namebank.isEmpty())) {
				stmt.setString(index++, namebank);
			}
			if (!(show == null)) {
				stmt.setInt(index++, show);
			}
			if (!(active == null)) {
				stmt.setInt(index++, active);
			}
			stmt.setInt(index++, page * maxItem);
			stmt.setInt(index, maxItem);

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				UserAgentModel agent = new UserAgentModel();
				agent.setId(rs.getInt("id"));
				agent.setUsername(rs.getString("username"));
				agent.setNickname(rs.getString("nickname"));
				agent.setNameagent(rs.getString("nameagent"));
				agent.setAddress(rs.getString("address"));
				agent.setPhone(rs.getString("phone"));
				agent.setEmail(rs.getString("email"));
				agent.setFacebook(rs.getString("facebook"));
				agent.setKey(rs.getString("key"));
				agent.setStatus(rs.getString("status"));
				agent.setParentid(rs.getInt("parentid"));
				agent.setNamebank(rs.getString("namebank"));
				agent.setNameaccount(rs.getString("nameaccount"));
				agent.setNumberaccount(rs.getString("numberaccount"));
				agent.setShow(rs.getInt("show"));
				agent.setActive(rs.getInt("active"));
				agent.setCreatetime(rs.getDate("createtime"));
				agent.setUpdatetime(rs.getDate("updatetime"));
				agent.setOrder(rs.getInt("order"));
				agent.setSms(rs.getInt("sms"));
				agent.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				agent.setCommission_rate(rs.getDouble("commission_rate"));
				agent.setSite(rs.getString("site"));
				agent.setLast_login_time(rs.getDate("last_login_time"));
				agent.setLogin_times(rs.getInt("login_times"));
				agent.setLevel(rs.getInt("level"));
				agent.setCode(rs.getString("code"));
				results.add(agent);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return results;
		} catch (SQLException e) {
			throw e;
		}
	}

	@Override
	public UserAgentModel DetailUserAgent(Integer id) throws SQLException {
		UserAgentModel agent = new UserAgentModel();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT * FROM vinplay_admin.useragent WHERE id= ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			if (!(id == null)) {
				stmt.setInt(1, id);
			} else
				return null;

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				agent.setId(rs.getInt("id"));
				agent.setUsername(rs.getString("username"));
				agent.setNickname(rs.getString("nickname"));
				agent.setNameagent(rs.getString("nameagent"));
				agent.setAddress(rs.getString("address"));
				agent.setPhone(rs.getString("phone"));
				agent.setEmail(rs.getString("email"));
				agent.setFacebook(rs.getString("facebook"));
				agent.setKey(rs.getString("key"));
				agent.setStatus(rs.getString("status"));
				agent.setParentid(rs.getInt("parentid"));
				agent.setNamebank(rs.getString("namebank"));
				agent.setNameaccount(rs.getString("nameaccount"));
				agent.setNumberaccount(rs.getString("numberaccount"));
				agent.setShow(rs.getInt("show"));
				agent.setActive(rs.getInt("active"));
				agent.setCreatetime(rs.getDate("createtime"));
				agent.setUpdatetime(rs.getDate("updatetime"));
				agent.setOrder(rs.getInt("order"));
				agent.setSms(rs.getInt("sms"));
				agent.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				agent.setCommission_rate(rs.getDouble("commission_rate"));
				agent.setSite(rs.getString("site"));
				agent.setLast_login_time(rs.getDate("last_login_time"));
				agent.setLogin_times(rs.getInt("login_times"));
				agent.setLevel(rs.getInt("level"));
				agent.setCode(rs.getString("code"));
				agent.setAncestors(rs.getString("ancestors"));
				break;
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return agent;
		} catch (SQLException e) {
			throw e;
		}
	}
	
	@Override
	public UserAgentModel DetailUserAgentByCode(String agencyCode) throws SQLException {
		UserAgentModel agent = new UserAgentModel();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT * FROM vinplay_admin.useragent WHERE code= ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			if (!StringUtils.isBlank(agencyCode)) {
				stmt.setString(1, agencyCode);
			} else
				return null;

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				agent.setId(rs.getInt("id"));
				agent.setUsername(rs.getString("username"));
				agent.setNickname(rs.getString("nickname"));
				agent.setNameagent(rs.getString("nameagent"));
				agent.setAddress(rs.getString("address"));
				agent.setPhone(rs.getString("phone"));
				agent.setEmail(rs.getString("email"));
				agent.setFacebook(rs.getString("facebook"));
				agent.setKey(rs.getString("key"));
				agent.setStatus(rs.getString("status"));
				agent.setParentid(rs.getInt("parentid"));
				agent.setNamebank(rs.getString("namebank"));
				agent.setNameaccount(rs.getString("nameaccount"));
				agent.setNumberaccount(rs.getString("numberaccount"));
				agent.setShow(rs.getInt("show"));
				agent.setActive(rs.getInt("active"));
				agent.setCreatetime(rs.getDate("createtime"));
				agent.setUpdatetime(rs.getDate("updatetime"));
				agent.setOrder(rs.getInt("order"));
				agent.setSms(rs.getInt("sms"));
				agent.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				agent.setCommission_rate(rs.getDouble("commission_rate"));
				agent.setSite(rs.getString("site"));
				agent.setLast_login_time(rs.getDate("last_login_time"));
				agent.setLogin_times(rs.getInt("login_times"));
				agent.setLevel(rs.getInt("level"));
				agent.setCode(rs.getString("code"));
				agent.setAncestors(rs.getString("ancestors"));
				break;
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return agent.getId() == null ? null : agent;
		} catch (SQLException e) {
			logger.error("[DetailUserAgentByCode] exception: " + e.getMessage());
			return null;
		}
	}
	
	@Override
	public UserAgentModel DetailUserAgentByUserName(String username) throws SQLException {
		UserAgentModel agent = new UserAgentModel();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT * FROM vinplay_admin.useragent WHERE username= ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			if (!StringUtils.isBlank(username)) {
				stmt.setString(1, username);
			} else
				return null;

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				agent.setId(rs.getInt("id"));
				agent.setUsername(rs.getString("username"));
				agent.setNickname(rs.getString("nickname"));
				agent.setNameagent(rs.getString("nameagent"));
				agent.setAddress(rs.getString("address"));
				agent.setPhone(rs.getString("phone"));
				agent.setEmail(rs.getString("email"));
				agent.setFacebook(rs.getString("facebook"));
				agent.setKey(rs.getString("key"));
				agent.setStatus(rs.getString("status"));
				agent.setParentid(rs.getInt("parentid"));
				agent.setNamebank(rs.getString("namebank"));
				agent.setNameaccount(rs.getString("nameaccount"));
				agent.setNumberaccount(rs.getString("numberaccount"));
				agent.setShow(rs.getInt("show"));
				agent.setActive(rs.getInt("active"));
				agent.setCreatetime(rs.getDate("createtime"));
				agent.setUpdatetime(rs.getDate("updatetime"));
				agent.setOrder(rs.getInt("order"));
				agent.setSms(rs.getInt("sms"));
				agent.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				agent.setCommission_rate(rs.getDouble("commission_rate"));
				agent.setSite(rs.getString("site"));
				agent.setLast_login_time(rs.getDate("last_login_time"));
				agent.setLogin_times(rs.getInt("login_times"));
				agent.setLevel(rs.getInt("level"));
				agent.setCode(rs.getString("code"));
				agent.setAncestors(rs.getString("ancestors"));
				break;
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return agent.getId() == null ? null : agent;
		} catch (SQLException e) {
			logger.error("[DetailUserAgentByNickName] exception: " + e.getMessage());
			return null;
		}
	}
	
	@Override
	public UserAgentModel DetailUserAgentByNickName(String nickname) throws SQLException {
		UserAgentModel agent = new UserAgentModel();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT * FROM vinplay_admin.useragent WHERE nickname= ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			if (!StringUtils.isBlank(nickname)) {
				String nickTrim = nickname.trim();
				stmt.setString(1, nickTrim);
			} else {
				return null;
			}

			ResultSet rs = stmt.executeQuery();
			boolean found = false;
			while (rs.next()) {
				found = true;
				agent.setId(rs.getInt("id"));
				agent.setUsername(rs.getString("username"));
				agent.setNickname(rs.getString("nickname"));
				agent.setNameagent(rs.getString("nameagent"));
				agent.setAddress(rs.getString("address"));
				agent.setPhone(rs.getString("phone"));
				agent.setEmail(rs.getString("email"));
				agent.setFacebook(rs.getString("facebook"));
				agent.setKey(rs.getString("key"));
				agent.setStatus(rs.getString("status"));
				agent.setParentid(rs.getInt("parentid"));
				agent.setNamebank(rs.getString("namebank"));
				agent.setNameaccount(rs.getString("nameaccount"));
				agent.setNumberaccount(rs.getString("numberaccount"));
				agent.setShow(rs.getInt("show"));
				agent.setActive(rs.getInt("active"));
				agent.setCreatetime(rs.getDate("createtime"));
				agent.setUpdatetime(rs.getDate("updatetime"));
				agent.setOrder(rs.getInt("order"));
				agent.setSms(rs.getInt("sms"));
				agent.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				agent.setCommission_rate(rs.getDouble("commission_rate"));
				agent.setSite(rs.getString("site"));
				agent.setLast_login_time(rs.getDate("last_login_time"));
				agent.setLogin_times(rs.getInt("login_times"));
				agent.setLevel(rs.getInt("level"));
				agent.setCode(rs.getString("code"));
				agent.setAncestors(rs.getString("ancestors"));
				break;
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return agent.getId() == null ? null : agent;
		} catch (SQLException e) {
			logger.error("[DetailUserAgentByNickName] exception: " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	public String changePasswordUserAgent(String nickname, String old_password, String new_password)
			throws SQLException {
		try (Connection conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
			String sql = "select useragent.password from vinplay_admin.useragent where 1=1 "
					+ (nickname == null || nickname.isEmpty() ? "" : " and nickname = ? ");

			PreparedStatement stmt = conn.prepareStatement(sql);
			if (!(nickname == null || nickname.isEmpty())) {
				stmt.setString(1, nickname);
			}

			String password = null;
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				password = rs.getString("password");
			}
			if (password != null && !password.trim().isEmpty() && password.equals(old_password)) {
				String sql1 = "UPDATE vinplay_admin.useragent SET password = ? WHERE nickname = ?";
				PreparedStatement stmt1 = conn.prepareStatement(sql1);
				stmt1.setString(1, new_password);
				stmt1.setString(2, nickname);
				int rowAffected = -1;
				rowAffected = stmt1.executeUpdate();
				stmt1.close();
				if (rowAffected > 0)
					return "success";
				else
					return "not_found";
			}
			rs.close();
			stmt.close();
			return "not_same";
		}
	}

	@Override
	public String getNicknameById(int id) throws SQLException {
		try (Connection conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
			PreparedStatement stmt = conn.prepareStatement("SELECT nickname FROM vinplay_admin.useragent WHERE id = ?");
			stmt.setInt(1, id);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getString("nickname");
			}
			rs.close();
			stmt.close();
		}
		return null;
	}

	@Override
	public String resetPasswordUserAgent(String nickname, String newPassword) throws SQLException {
		try (Connection conn = ConnectionPool.getInstance().getConnection(ConnectionPool.ADMIN_POOL)) {
			PreparedStatement stmt = conn.prepareStatement(
				"UPDATE vinplay_admin.useragent SET password = ? WHERE nickname = ?");
			stmt.setString(1, newPassword);
			stmt.setString(2, nickname);
			int rows = stmt.executeUpdate();
			stmt.close();
			return rows > 0 ? "success" : "not_found";
		}
	}

	@Override
	public long countlistUserAgent(String username, String nickname, String password, String nameagent, String address,
			String phone, String email, String facebook, String key, String status, Integer parentid, String namebank,
			String nameaccount, String numberaccount, Integer show, Integer active, Date createtime, Date updatetime,
			Integer order, Integer sms, Integer percent_bonus_vincard, String site, Date last_login_time,
			Integer login_times, Integer level, String code) throws SQLException {
		long count = 0;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT count(*) as cnt FROM vinplay_admin.useragent WHERE 1=1"
					+ (username == null || username.isEmpty() ? "" : (" and username = ?"))
					+ (nickname == null || nickname.isEmpty() ? "" : (" and nickname = ?"))
					+ (nameagent == null || nameagent.isEmpty() ? "" : (" and nameagent = ?"))
					+ (address == null || address.isEmpty() ? "" : (" and address = ?"))
					+ (phone == null || phone.isEmpty() ? "" : (" and phone = ?"))
					+ (email == null || email.isEmpty() ? "" : (" and email = ?"))
					+ (facebook == null || facebook.isEmpty() ? "" : (" and facebook = ?"))
					+ (status == null || status.isEmpty() ? "" : (" and status = ?"))
					+ (namebank == null || namebank.isEmpty() ? "" : (" and namebank = ?"))
					+ (show == null ? "" : (" and show = ?")) + (active == null ? "" : (" and active = ?"));
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(username == null || username.isEmpty())) {
				stmt.setString(index++, username);
			}
			if (!(nickname == null || nickname.isEmpty())) {
				stmt.setString(index++, nickname);
			}
			if (!(nameagent == null || nameagent.isEmpty())) {
				stmt.setString(index++, nameagent);
			}
			if (!(address == null || address.isEmpty())) {
				stmt.setString(index++, address);
			}
			if (!(phone == null || phone.isEmpty())) {
				stmt.setString(index++, phone);
			}
			if (!(email == null || email.isEmpty())) {
				stmt.setString(index++, email);
			}
			if (!(facebook == null || facebook.isEmpty())) {
				stmt.setString(index++, facebook);
			}
			if (!(status == null || status.isEmpty())) {
				stmt.setString(index++, status);
			}
			if (!(namebank == null || namebank.isEmpty())) {
				stmt.setString(index++, namebank);
			}
			if (!(show == null)) {
				stmt.setInt(index++, show);
			}
			if (!(active == null)) {
				stmt.setInt(index++, active);
			}

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt("cnt");
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return count;
		} catch (SQLException e) {
			throw e;
		}
	}

	@Override
	public Boolean AddNewUserAgent(String username, String nickname, String password, String nameagent, String address,
			String phone, String email, String facebook, String key, String status, Integer parentid, String namebank,
			String nameaccount, String numberaccount, Integer show, Integer active, String createtime,
			String updatetime, Integer order, Integer sms, Integer percent_bonus_vincard, String site,
			Date last_login_time, Integer login_times, Integer level, String code, Double commission_rate) throws SQLException {
		parentid = parentid == null ? -1 : parentid;
		String sql = "INSERT INTO vinplay_admin.useragent (username, nickname, password"
				+ ((nameagent == null) ? "" : ", nameagent") + ((address == null) ? "" : ", address")
				+ ((phone == null) ? "" : ", phone") + ((email == null) ? "" : ", email")
				+ ((facebook == null) ? "" : ", facebook") + ((key == null) ? "" : ", key")
				+ ((status == null) ? "" : ", status") + ((namebank == null) ? "" : ", namebank")
				+ ((nameaccount == null) ? "" : ", nameaccount") + ((numberaccount == null) ? "" : ", numberaccount")
				+ ((show == null) ? "" : ", show") + ((active == null) ? "" : ", active") + ", createtime, updatetime"
				+ ((commission_rate == null) ? "" : ", commission_rate")
				+ ((site == null) ? "" : ", site") + ((code == null) ? "" : ", code") + ((parentid == null) ? "" : ", parentid") 
				+ ") VALUES(?,?,?"
				+ ((nameagent == null) ? "" : ",?") + ((address == null) ? "" : ",?") + ((phone == null) ? "" : ",?")
				+ ((email == null) ? "" : ",?") + ((facebook == null) ? "" : ",?") + ((key == null) ? "" : ",?")
				+ ((status == null) ? "" : ",?") + ((namebank == null) ? "" : ",?")
				+ ((nameaccount == null) ? "" : ",?") + ((numberaccount == null) ? "" : ",?")
				+ ((show == null) ? "" : ",?") + ((active == null) ? "" : ",?") + ",?,?"
				+ ((commission_rate == null) ? "" : ",?")
				+ ((site == null) ? "" : ",?")
				+ ((code == null) ? "" : ",?") + ((parentid == null) ? "" : ",?") + ")";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			Date date1, date2;
			if (createtime == null || createtime.trim().isEmpty()) {
				date1 = new Date();
			} else {
				date1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(createtime);
			}
			if (updatetime == null || updatetime.trim().isEmpty()) {
				date2 = new Date();
			} else {
				date2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(updatetime);
			}
			int index = 1;
			stm.setString(index++, username);
			stm.setString(index++, nickname);
			stm.setString(index++, password);
			if (nameagent != null) {
				stm.setString(index++, nameagent);
			}
			if (address != null) {
				stm.setString(index++, address);
			}
			if (phone != null) {
				stm.setString(index++, phone);
			}
			if (email != null) {
				stm.setString(index++, email);
			}
			if (facebook != null) {
				stm.setString(index++, facebook);
			}
			if (key != null) {
				stm.setString(index++, key);
			}
			if (status != null) {
				stm.setString(index++, status);
			}
			if (namebank != null) {
				stm.setString(index++, namebank);
			}
			if (nameaccount != null) {
				stm.setString(index++, nameaccount);
			}
			if (numberaccount != null) {
				stm.setString(index++, numberaccount);
			}
			if (show != null) {
				stm.setInt(index++, show);
			}
			if (active != null) {
				stm.setInt(index++, active);
			}
			stm.setDate(index++, new java.sql.Date(date1.getTime()));
			stm.setDate(index++, new java.sql.Date(date2.getTime()));
			if (commission_rate != null) {
				stm.setDouble(index++, commission_rate);
			}
			if (site != null) {
				stm.setString(index++, site);
			}
			if (code != null) {
				stm.setString(index++, code);
			}
			if (parentid != null) {
				stm.setInt(index++, parentid);
			}
			stm.executeUpdate();
			stm.close();
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			return false;
		} catch (ParseException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public Boolean AddNewUserAgent(UserAgentModel userAgentModel) throws SQLException {
		String sql = "INSERT INTO vinplay_admin.useragent (username, nickname, password,"
				+ " nameagent, address, phone, email, facebook, `key`, `status`, namebank, nameaccount, numberaccount,"
				+ " `show`, `active`, commission_rate, site, code, parentid,createtime,updatetime) VALUES"
				+ " (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, userAgentModel.getUsername());
			stm.setString(index++, userAgentModel.getNickname());
			stm.setString(index++, userAgentModel.getPassword());
			stm.setString(index++, userAgentModel.getNameagent());
			stm.setString(index++, userAgentModel.getAddress());
			stm.setString(index++, userAgentModel.getPhone());
			stm.setString(index++, userAgentModel.getEmail());
			stm.setString(index++, userAgentModel.getFacebook());
			stm.setString(index++, userAgentModel.getKey());
			stm.setString(index++, userAgentModel.getStatus());
			stm.setString(index++, userAgentModel.getNamebank());
			stm.setString(index++, userAgentModel.getNameaccount());
			stm.setString(index++, userAgentModel.getNumberaccount());
			stm.setInt(index++, userAgentModel.getShow());
			stm.setInt(index++, userAgentModel.getActive());
			stm.setDouble(index++, userAgentModel.getCommission_rate() == null ? 0d : userAgentModel.getCommission_rate());
			stm.setString(index++, userAgentModel.getSite());
			stm.setString(index++, userAgentModel.getCode());
			stm.setInt(index++, userAgentModel.getParentid());
			stm.setString(index++, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(userAgentModel.getCreatetime()));
			stm.setString(index++, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(userAgentModel.getUpdatetime()));
			stm.executeUpdate();
			stm.close();
			if (conn != null) {
				conn.close();
			}
			
		} catch (SQLException e) {
			return false;
		}

		return true;
	}
	
	@Override
	public Boolean AddNewUser(UserAgentModel userAgentModel) throws SQLException {
//		ControlContrain(0);
		String sql = "INSERT INTO vinplay.users (user_name,nick_name,`password`,email,mobile,address,"
				+ " vin,xu,vin_total,xu_total,avatar,"
				+ " dai_ly, `status`, usertype, is_bot, referral_code, create_time) VALUES"
				+ " (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, userAgentModel.getUsername());
			stm.setString(index++, userAgentModel.getNickname());
			stm.setString(index++, userAgentModel.getPassword());
			stm.setString(index++, userAgentModel.getEmail());
			stm.setString(index++, userAgentModel.getPhone());
			stm.setString(index++, userAgentModel.getAddress());
			
			stm.setInt(index++, 0);
			stm.setInt(index++, 0);
			stm.setInt(index++, 0);
			stm.setInt(index++, 0);
			stm.setString(index++, "0");
			
			stm.setInt(index++, 1);
			stm.setInt(index++, 0);
			stm.setInt(index++, 0);
			stm.setInt(index++, 0);
			stm.setString(index++, userAgentModel.getCode());
			stm.setString(index++, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(userAgentModel.getCreatetime()));
			stm.executeUpdate();
			stm.close();
			if (conn != null) {
				conn.close();
			}
			
//			ControlContrain(1);
		} catch (SQLException e) {
			logger.error("Add new user from agent info: " + e.getMessage());
			return false;
		}

		return true;
	}
	
	private Boolean ControlContrain(int type) throws SQLException {
		String sql = "SET FOREIGN_KEY_CHECKS=" + type;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			stm.executeUpdate();
			stm.close();
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Add new user from agent info: " + e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public Boolean checkCodeOfUserAgent(String code) throws SQLException {
		long count = 0;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "SELECT count(*) as cnt FROM vinplay_admin.useragent WHERE code = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, code);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt("cnt");
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			if (count > 0) {
				return true;
			} else
				return false;
		} catch (SQLException e) {
			throw e;
		}
	}

	@Override
	public Boolean UpdateUserAgent(Integer id, String username, String nickname, String password, String nameagent,
			String address, String phone, String email, String facebook, String key, String status, Integer parentid,
			String namebank, String nameaccount, String numberaccount, Integer show, Integer active, String createtime,
			String updatetime, Integer order, Integer sms, Integer percent_bonus_vincard, String site,
			Date last_login_time, Integer login_times, Integer level, String code, Double commission_rate) throws SQLException {
		String sql = "UPDATE vinplay_admin.useragent SET updatetime = ?" + ((nickname != null) ? ", " : " ")
				+ ((nickname == null) ? "" : " nickname = ?") + ((nameagent != null) ? ", " : " ")
				+ ((nameagent == null) ? "" : " nameagent = ?") + ((address != null) ? ", " : " ")
				+ ((address == null) ? "" : " address = ?") + ((phone != null) ? ", " : " ")
				+ ((phone == null) ? "" : " phone = ?") + ((email != null) ? ", " : " ")
				+ ((email == null) ? "" : " email = ?") + ((facebook != null) ? ", " : " ")
				+ ((facebook == null) ? "" : " facebook = ?") + ((key != null) ? ", " : " ")
				+ ((key == null) ? "" : " key = ?") + ((status != null) ? ", " : " ")
				+ ((status == null) ? "" : " status = ?") + ((namebank != null) ? ", " : " ")
				+ ((namebank == null) ? "" : " namebank = ?") + ((nameaccount != null) ? ", " : " ")
				+ ((nameaccount == null) ? "" : " nameaccount = ?") + ((numberaccount != null) ? ", " : " ")
				+ ((numberaccount == null) ? "" : " numberaccount = ?") + ((show != null) ? ", " : " ")
				+ ((show == null) ? "" : " show = ?") + ((active != null) ? ", " : " ")
				+ ((active == null) ? "" : " active = ?") + ((level != null) ? ", " : " ")
				+ ((level == null) ? "" : " level = ?") + ((code != null) ? ", " : " ")
				+ ((code == null) ? "" : " code = ?") + ((commission_rate != null) ? ", " : " ")
				+ ((commission_rate == null) ? "" : " commission_rate = ?") + " WHERE id = ?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			Date currentdate = new Date();
			stm.setDate(index++, new java.sql.Date(currentdate.getTime()));

			if (nickname != null) {
				stm.setString(index++, nickname);
			}
			if (nameagent != null) {
				stm.setString(index++, nameagent);
			}
			if (address != null) {
				stm.setString(index++, address);
			}
			if (phone != null) {
				stm.setString(index++, phone);
			}
			if (email != null) {
				stm.setString(index++, email);
			}
			if (facebook != null) {
				stm.setString(index++, facebook);
			}
			if (key != null) {
				stm.setString(index++, key);
			}
			if (status != null) {
				stm.setString(index++, status);
			}
			if (namebank != null) {
				stm.setString(index++, namebank);
			}
			if (nameaccount != null) {
				stm.setString(index++, nameaccount);
			}
			if (numberaccount != null) {
				stm.setString(index++, numberaccount);
			}
			if (show != null) {
				stm.setInt(index++, show);
			}
			if (active != null) {
				stm.setInt(index++, active);
			}
			if (level != null) {
				stm.setInt(index++, level);
			}
			if (code != null) {
				stm.setString(index++, code);
			}
			if (commission_rate != null) {
				stm.setDouble(index++, commission_rate);
			}

			stm.setInt(index++, id);
			stm.executeUpdate();
		} catch (SQLException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public Boolean UpdateUserAgent(UserAgentModel userAgentModel) throws SQLException {
		String sql = "UPDATE vinplay_admin.useragent SET nickname=?,nameagent=?,address=?,phone=?,email=?,"
				+ " facebook=?,key=?,status=?,namebank=?,nameaccount=?,numberaccount=?,show=?,active=?,commission_rate=? where id=?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, userAgentModel.getNickname());
			stm.setString(index++, userAgentModel.getNameagent());
			stm.setString(index++, userAgentModel.getAddress());
			stm.setString(index++, userAgentModel.getPhone());
			stm.setString(index++, userAgentModel.getEmail());
			stm.setString(index++, userAgentModel.getFacebook());
			stm.setString(index++, userAgentModel.getKey());
			stm.setString(index++, userAgentModel.getStatus());
			stm.setString(index++, userAgentModel.getNamebank());
			stm.setString(index++, userAgentModel.getNameaccount());
			stm.setString(index++, userAgentModel.getNumberaccount());
			stm.setInt(index++, userAgentModel.getShow());
			stm.setInt(index++, userAgentModel.getActive());
			stm.setDouble(index++, userAgentModel.getCommission_rate() == null ? 0d : userAgentModel.getCommission_rate());
			stm.setInt(index++, userAgentModel.getId());
			stm.executeUpdate();
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	@Override
	public Boolean deleteUserAgent(Integer id) throws SQLException {
		String sql = "DELETE FROM vinplay_admin.useragent where id = ?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			stm.setInt(1, id);
			stm.executeUpdate();
			stm.close();
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	@Override
	public Map<String, Object> listAgent1(String nick_name, String fromTime, String endTime, int page, int maxItem)
			throws SQLException {
		Map<String, Object> map = new HashMap<>();
		List<VinPlayAgentModel> results = new ArrayList<VinPlayAgentModel>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			page = (page - 1) < 0 ? 0 : (page - 1);
			page *= maxItem;
			String sql = "SELECT u.id, u.nick_name, ag.referral_code, u.create_time "
					+ "FROM vinplay.users u,vinplay.agency_code ag WHERE u.id = ag.user_id "
					+ (nick_name == null || nick_name.isEmpty() ? "" : ("and nick_name = '" + nick_name + "' "))
					+ ((fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty()) ? ""
							: (" and create_time >= '" + fromTime + " 00:00:00" + "' and create_time <= '" + endTime
									+ " 23:59:59" + "'"))
					+ "limit ?,?";
			String sql1 = "SELECT count(*) as cnt FROM vinplay.users,vinplay.agency_code WHERE users.id = agency_code.user_id "
					+ (nick_name == null || nick_name.isEmpty() ? "" : ("and nick_name = '" + nick_name + "' "))
					+ ((fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty()) ? ""
							: (" and create_time >= '" + fromTime + " 00:00:00" + "' and create_time <= '" + endTime
									+ " 23:59:59" + "'"));
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setInt(1, page);
			stmt.setInt(2, maxItem);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				VinPlayAgentModel agent = new VinPlayAgentModel();
				agent.setNick_name(rs.getString("nick_name"));
				agent.setAgent_id(rs.getInt("id"));
				agent.setCreate_time(rs.getDate("create_time"));
				agent.setReferral_code(rs.getString("referral_code"));
				results.add(agent);
			}
			rs.close();
			stmt.close();
			// count
			PreparedStatement stmt1 = conn.prepareStatement(sql1);
			ResultSet rs1 = stmt1.executeQuery();
			long count = 0;
			if (rs1.next()) {
				count = rs1.getInt("cnt");
			}
			rs1.close();
			stmt1.close();
			if (conn != null) {
				conn.close();
			}
			map.put("listData", results);
			map.put("totalData", count);
			return map;
		} catch (SQLException e) {
			throw e;
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<UserOfAgentModel> listUserOfAgent(String referral_code, String nick_name, String fromTime,
			String endTime, Long doanhThu, int page, int maxItem) throws SQLException {

		List<UserOfAgentModel> users = new ArrayList<>();
		page = (page - 1) < 0 ? 0 : (page - 1);

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "select u.id, u.nick_name, u.create_time, 0 AS vin_total, sum(l.deposit) as total_nap, sum(l.withdraw) as total_rut  FROM vinplay.users u " // SUN-13xx: vin_total dropped, kept alias for response parity
					+ " left join log_report_user l on u.nick_name = l.nick_name " + " where 1=1 "
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and referral_code = ? ")
					+ ((nick_name == null || nick_name.isEmpty()) ? "" : (" and u.nick_name = ? and l.nick_name = ?"))
					+ ((fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty()) ? ""
							: (" and u.create_time >= ?  and u.create_time <= ?"))
					+ " GROUP BY u.id, u.nick_name, u.create_time " + " order by u.id desc limit ?,?"; // SUN-13xx: removed vin_total from GROUP BY

			PreparedStatement stmt = conn.prepareStatement(sql);

			int param = 1;
			if (!(referral_code == null || referral_code.isEmpty())) {
				stmt.setString(param++, referral_code);
			}
			if (!(nick_name == null || nick_name.isEmpty())) {
				stmt.setString(param++, nick_name);
				stmt.setString(param++, nick_name);
			}
			if (!(fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty())) {
				stmt.setString(param++, fromTime + " 00:00:00");
				stmt.setString(param++, endTime + " 23:59:59");
			}
			stmt.setInt(param++, page * maxItem);
			stmt.setInt(param, maxItem);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				UserOfAgentModel user = new UserOfAgentModel();
				user.setNick_name(rs.getString("nick_name"));
				user.setId(rs.getInt("id"));
				user.setCreate_time(rs.getDate("create_time"));
				user.setBalance(0L);
				user.setTotal_nap(rs.getLong("total_nap"));
				user.setTotal_rut(rs.getLong("total_rut"));
				user.setTotal_bonus(0L);
				user.setDoanh_thu(user.getTotal_nap() - user.getTotal_rut());
				users.add(user);
			}

			return users;
		}
	}

	@Override
	public List<DetailUserModel> listUserOfUserAgent(String code, String nickname, String fromTime, String endTime,
			int page, int maxItem) throws SQLException {
		List<DetailUserModel> users = new ArrayList<>();
		page = (page - 1) < 0 ? 0 : (page - 1);
		String sql = "select *  FROM vinplay.users u where is_bot=false "
				+ ((code == null || code.isEmpty()) ? "" : " and referral_code = ? ")
				+ ((nickname == null || nickname.isEmpty()) ? "" : " and nick_name = ?")
				+ ((fromTime == null || fromTime.isEmpty()) ? "" : " and create_time >= ?")
				+ ((endTime == null || endTime.isEmpty()) ? "" : " and create_time <= ?") + " limit ?,?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			int param = 1;
			if (code != null && !code.isEmpty()) {
				stmt.setString(param++, code);
			}
			if (!(nickname == null || nickname.isEmpty())) {
				stmt.setString(param++, nickname);
			}
			if (!(fromTime == null || fromTime.isEmpty())) {
				stmt.setString(param++, fromTime + " 00:00:00");
			}
			if (!(endTime == null || endTime.isEmpty())) {
				stmt.setString(param++, endTime + " 23:59:59");
			}
			stmt.setInt(param++, page * maxItem);
			stmt.setInt(param, maxItem);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					DetailUserModel user = new DetailUserModel();
					user.setUser_name(rs.getString("user_name"));
					user.setNick_name(rs.getString("nick_name"));
					user.setEmail(rs.getString("email"));
					user.setGoogle_id(rs.getString("google_id"));
					user.setFacebook_id(rs.getString("facebook_id"));
					user.setMobile(rs.getString("mobile"));
					user.setIdentification(rs.getString("identification"));
					user.setAvatar(rs.getInt("avatar"));
					try {
						user.setBirthday(rs.getDate("birthday"));
					} catch (Exception e) {
					}
					user.setGender(rs.getBoolean("gender"));
					user.setAddress(rs.getString("address"));
					user.setVin(rs.getLong("vin"));
					user.setXu(0L);
					user.setVin_total(0L);
					user.setXu_total(0L);
					user.setSafe(0L);
					user.setRecharge_money(0L);
					user.setVip_point(0);
					user.setVip_point_save(0);
					user.setMoney_vp(0);
					user.setDai_ly(rs.getInt("dai_ly"));
					user.setStatus(rs.getInt("status"));
					try {
						user.setCreate_time(rs.getDate("create_time"));
					} catch (Exception e) {
					}
					try {
						user.setSecurity_time(rs.getDate("security_time"));
					} catch (Exception e) {
					}
					user.setLogin_otp(rs.getLong("login_otp"));
					user.setIs_bot(rs.getInt("is_bot"));
					try {
						user.setUpdate_pw_time(rs.getDate("update_pw_time"));
					} catch (Exception e) {
					}
					user.setIs_verify_mobile(rs.getBoolean("is_verify_mobile"));
					user.setReferral_code(rs.getString("referral_code"));
					user.setT_nap(rs.getLong("t_nap"));
					user.setT_rut(rs.getLong("t_rut"));
					try {
						user.setNap_times(rs.getDate("rut_times"));
					} catch (Exception e) {
					}
					try {
						user.setRut_times(rs.getDate("nap_times"));
					} catch (Exception e) {
					}
					users.add(user);
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return users;
	}

	@Override
	public long countUserOfUserAgent(String code, String nickname, String fromTime, String endTime)
			throws SQLException {
		long count = 0;
		String sql = "select count(*) as cnt  FROM vinplay.users u where is_bot=false "
				+ ((code == null || code.isEmpty()) ? "" : " and referral_code = ? ")
				+ ((nickname == null || nickname.isEmpty()) ? "" : " and nick_name = ?")
				+ ((fromTime == null || fromTime.isEmpty()) ? "" : " and create_time >= ?")
				+ ((endTime == null || endTime.isEmpty()) ? "" : " and create_time <= ?");
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			int param = 1;
			if (code != null && !code.isEmpty()) {
				stmt.setString(param++, code);
			}
			if (!(nickname == null || nickname.isEmpty())) {
				stmt.setString(param++, nickname);
			}
			if (!(fromTime == null || fromTime.isEmpty())) {
				stmt.setString(param++, fromTime + " 00:00:00");
			}
			if (!(endTime == null || endTime.isEmpty())) {
				stmt.setString(param++, endTime + " 23:59:59");
			}
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					count = rs.getInt("cnt");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return count;
	}

	private Long[] analyticsDepositWithdrawOfAllUserOfAgent_WRONG(String referral_code, String nick_name,
			String fromTime, String endTime, Long doanhThu) throws SQLException {
		Long[] num = { 0L, 0L, 0L };

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "select IFNULL(sum(l.deposit),0) as total_nap, IFNULL(sum(l.withdraw),0) as total_rut, IFNULL(sum(l.t_bonus),0) as total_km  FROM vinplay.users u "
					+ " left join log_report_user l on u.nick_name = l.nick_name " + " where 1=1 "
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and referral_code = ? ")
					+ ((nick_name == null || nick_name.isEmpty()) ? "" : (" and u.nick_name = ? and l.nick_name = ?"))
					+ ((fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty()) ? ""
							: (" and u.create_time >= ?  and u.create_time <= ?"));
//                    + " GROUP BY u.id, u.nick_name, u.create_time, u.vin_total ";

			PreparedStatement stmt = conn.prepareStatement(sql);

			int param = 1;
			if (!(referral_code == null || referral_code.isEmpty())) {
				stmt.setString(param++, referral_code);
			}
			if (!(nick_name == null || nick_name.isEmpty())) {
				stmt.setString(param++, nick_name);
				stmt.setString(param++, nick_name);
			}
			if (!(fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty())) {
				stmt.setString(param++, fromTime + " 00:00:00");
				stmt.setString(param++, endTime + " 23:59:59");
			}
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				num[0] = rs.getLong("total_nap");
				num[1] = rs.getLong("total_rut");
				num[2] = rs.getLong("total_km");
			}
			return num;
		}
	}

	@Override
	public Long[] analyticsDepositWithdrawOfAllUserOfAgent(String referral_code, String nick_name, String fromTime,
			String endTime, Long doanhThu) throws SQLException {
		Long[] num = { 0L, 0L, 0L };

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "select x.* , u.create_time from (" + " SELECT IFNULL(sum(deposit),0) total_nap,"
					+ " IFNULL(sum(withdraw),0) total_rut," + " IFNULL(sum(t_bonus),0) total_km, nick_name"
					+ " FROM log_report_user" + " where (1=1)"
					+ ((fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty()) ? ""
							: (" and time_report >= ?  and time_report <= ?"))
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and nick_name = ?"))
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and code = ? ")
					+ " GROUP BY nick_name )" + " x left join  users u on x. nick_name = u.nick_name";

			PreparedStatement stmt = conn.prepareStatement(sql);
			int param = 1;
			if (!(fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty())) {
				stmt.setString(param++, fromTime + " 00:00:00");
				stmt.setString(param++, endTime + " 23:59:59");
			}

			if (!(nick_name == null || nick_name.isEmpty())) {
				stmt.setString(param++, nick_name);
				stmt.setString(param++, nick_name);
			}

			if (!(referral_code == null || referral_code.isEmpty())) {
				stmt.setString(param++, referral_code);
			}

			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				num[0] = rs.getLong("total_nap");
				num[1] = rs.getLong("total_rut");
				num[2] = rs.getLong("total_km");
			}

			return num;
		} catch (Exception e) {
			logger.error("analyticsDepositWithdrawOfAllUserOfAgent: " + e.getMessage());
			return num;
		}
	}

	@Override
	public List<Map<String, Object>> reportUserPlay4Agent(String referral_code, String nick_name, String fromTime,
			String endTime, int page, int maxitem) throws SQLException {
		Long[] num = { 0L, 0L, 0L, 0L };
		List<Map<String, Object>> data = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sqlCount = "select count(*) total from (SELECT IFNULL(sum(deposit),0) t_nap,"
					+ " IFNULL(sum(withdraw),0) t_rut, IFNULL(sum(t_bonus),0) t_km, nick_name"
					+ " FROM log_report_user" + " where (1=1)"
					+ ((fromTime == null || endTime == null || fromTime.trim().isEmpty() || endTime.trim().isEmpty())
							? ""
							: (" and time_report >= ?  and time_report <= ?"))
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and nick_name = ?"))
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and code = ?")
					+ " GROUP BY nick_name) x right join users u on x.nick_name = u.nick_name"
					+ " where (1=1) "
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and u.nick_name = ?"))
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and referral_code = ?");

			String sql = "select IFNULL(x.t_nap,0) t_nap,IFNULL(x.t_rut,0) t_rut,IFNULL(x.t_km,0) t_km, u.create_time, u.vin , u.nick_name"
					+ " from (SELECT IFNULL(sum(deposit),0) t_nap,"
					+ " IFNULL(sum(withdraw),0) t_rut, IFNULL(sum(t_bonus),0) t_km, nick_name"
					+ " FROM log_report_user" + " where (1=1)"
					+ ((fromTime == null || endTime == null || fromTime.trim().isEmpty() || endTime.trim().isEmpty())
							? ""
							: (" and time_report >= ? and time_report <= ?"))
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and nick_name = ?"))
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and code = ?")
					+ " GROUP BY nick_name) x right join users u on x.nick_name = u.nick_name"
					+ " where (1=1) "
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and u.nick_name = ?"))
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and referral_code = ?")
					+ " order by t_nap desc, t_rut desc, t_km desc, create_time desc, nick_name asc"
					+ ((page == -1 || maxitem == -1) ? "" : " limit ?,?");

			String sqlTotal = "select IFNULL(x.t_nap,0) t_nap,IFNULL(x.t_rut,0) t_rut,IFNULL(x.t_km,0) t_km, u.create_time, u.vin , u.nick_name"
					+ " from (SELECT IFNULL(sum(deposit),0) t_nap,"
					+ " IFNULL(sum(withdraw),0) t_rut, IFNULL(sum(t_bonus),0) t_km, nick_name"
					+ " FROM log_report_user" + " where (1=1)"
					+ ((fromTime == null || endTime == null || fromTime.trim().isEmpty() || endTime.trim().isEmpty())
							? ""
							: (" and time_report >= ? and time_report <= ?"))
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and nick_name = ?"))
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and code = ?")
					+ " GROUP BY nick_name) x right join users u on x.nick_name = u.nick_name"
					+ " where (1=1) "
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and u.nick_name = ?"))
					+ ((referral_code == null || referral_code.isEmpty()) ? "" : " and referral_code = ?");

			PreparedStatement stmt = conn.prepareStatement(sql);
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmtTotal = conn.prepareStatement(sqlTotal);
			int index = 1;
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime + " 00:00:00");
				stmtCount.setString(index, fromTime + " 00:00:00");
				stmtTotal.setString(index, fromTime + " 00:00:00");
				index++;
			}

			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime + " 23:59:59");
				stmtCount.setString(index, endTime + " 23:59:59");
				stmtTotal.setString(index, endTime + " 23:59:59");
				index++;
			}

			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				stmtCount.setString(index, nick_name);
				stmtTotal.setString(index, nick_name);
				index++;
			}

			if (!(referral_code == null || referral_code.trim().isEmpty())) {
				stmt.setString(index, referral_code);
				stmtCount.setString(index, referral_code);
				stmtTotal.setString(index, referral_code);
				index++;
			}
			
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				stmtCount.setString(index, nick_name);
				stmtTotal.setString(index, nick_name);
				index++;
			}
			
			if (!(referral_code == null || referral_code.trim().isEmpty())) {
				stmt.setString(index, referral_code);
				stmtCount.setString(index, referral_code);
				stmtTotal.setString(index, referral_code);
				index++;
			}

			if (page != -1 && maxitem != -1) {
				page = page < 1 ? 0 : page - 1;
				stmt.setInt(index++, page * maxitem);
				stmt.setInt(index++, maxitem);
			}

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> map = new HashMap<>();
				long tNap = rs.getLong("t_nap");
				long tRut = rs.getLong("t_rut");
				long tKm = rs.getLong("t_km");
				long vin = rs.getLong("vin");

				map.put("t_nap", tNap);
				map.put("t_rut", tRut);
				map.put("t_km", tKm);
				map.put("create_time", rs.getString("create_time"));
				map.put("vin", vin);
				map.put("nick_name", rs.getString("nick_name"));
				map.put("net_loss", (tRut + vin) - (tNap + tKm));
				data.add(map);
			}

			ResultSet rsTotal = stmtTotal.executeQuery();
			while (rsTotal.next()) {
				num[0] += rsTotal.getLong("t_nap");
				num[1] += rsTotal.getLong("t_rut");
				num[2] += rsTotal.getLong("t_km");
				num[3] += rsTotal.getLong("vin");
			}

			Map<String, Object> map = new HashMap<>();
			map.put("total_nap", num[0]);
			map.put("total_rut", num[1]);
			map.put("total_km", num[2]);
			map.put("total_vin", num[3]);
			data.add(map);

			ResultSet rsCount = stmtCount.executeQuery();
			while (rsCount.next()) {
				map = new HashMap<>();
				map.put("total", rsCount.getInt("total"));
				data.add(map);
			}

			rs.close();
			rsCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;

		} catch (Exception e) {
			logger.error("analyticsDepositWithdrawOfAllUserOfAgent: " + e.getMessage());
			return data;
		}
	}

	private long getLog_money_user_nap_vin(Integer user_id, String nick_name) {
		final long[] totalExchange = { 0L };
		// truy van bang log_money_user => tien nap
		if (user_id != null && nick_name != null) {
			MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
			MongoCollection<?> col = db.getCollection("log_money_user_nap_vin");

			HashMap<String, Object> conditions = new HashMap<String, Object>();
			conditions.put("user_id", user_id);
			conditions.put("nick_name", nick_name);

			col.find((Bson) new Document(conditions)).forEach((Block) new Block<Document>() {
				public void apply(Document document) {
					Long money_exchange = document.getLong("money_exchange");
					if (money_exchange != null) {
						totalExchange[0] += money_exchange;
					}
				}
			});
		}

		return totalExchange[0];
	}

	private long getLog_money_user_tieu_vin(Integer user_id, String nick_name) {
		final long[] totalExchange = { 0L };

		// truy van bang log_money_user => tien rut
		MongoDatabase db1 = MongoDBConnectionFactory.getDB();
		MongoCollection<?> col1 = db1.getCollection("log_money_user_tieu_vin");

		HashMap<String, Object> conditions = new HashMap<String, Object>();
		conditions.put("user_id", user_id);
		conditions.put("nick_name", nick_name);

		col1.find((Bson) new Document(conditions)).forEach((Block) new Block<Document>() {
			public void apply(Document document) {
				Long money_exchange = document.getLong("money_exchange");
				if (money_exchange != null) {
					totalExchange[0] += money_exchange;
				}
			}
		});

		return totalExchange[0];
	}

	@Override
	public Map<String, Object> listAgent1(String nick_name, String refcode, String fromTime, String endTime, int page,
			int maxItem) throws SQLException {
		Map<String, Object> map = new HashMap<>();
		String condition = getConditionListAgent1(nick_name, refcode, fromTime, endTime);
		map.put("listData", getListListAgent1(condition, page, maxItem));
		map.put("totalData", countDataListAgent1(condition));
		return map;
	}

	private String getConditionListAgent1(String nick_name, String refcode, String fromTime, String endTime) {
		return " FROM vinplay.users u,vinplay.agency_code ag WHERE u.id = ag.user_id "
				+ (nick_name == null || nick_name.isEmpty() ? "" : (" and nick_name = '" + nick_name + "' "))
				+ (refcode == null || refcode.isEmpty() ? "" : (" and refcode = '" + refcode + "' "))
				+ (fromTime == null || fromTime.isEmpty() ? ""
						: (" and create_time >= '" + fromTime + " 00:00:00" + "'"))
				+ (endTime == null || endTime.isEmpty() ? "" : (" and create_time <= '" + endTime + " 23:59:59" + "'"));
	}

	private List<VinPlayAgentModel> getListListAgent1(String condition, int page, int maxItem) throws SQLException {
		List<VinPlayAgentModel> results = new ArrayList<VinPlayAgentModel>();
		page = (page - 1) < 0 ? 0 : (page - 1);
		page *= maxItem;

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			String getListSql = "SELECT u.id, u.nick_name, ag.referral_code, u.create_time " + condition
					+ " order by id desc limit ?,?";
			PreparedStatement stmt = conn.prepareStatement(getListSql);
			stmt.setInt(1, page);
			stmt.setInt(2, maxItem);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				VinPlayAgentModel agent = new VinPlayAgentModel();
				agent.setAgent_id(rs.getInt("id"));
				agent.setNick_name(rs.getString("nick_name"));
				agent.setReferral_code(rs.getString("referral_code"));
				agent.setCreate_time(rs.getDate("create_time"));
				results.add(agent);
			}
			rs.close();
			stmt.close();
			return results;
		}
	}

	private long countDataListAgent1(String condition) throws SQLException {
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			String countSQL = "SELECT count(*) as cnt " + condition;
			PreparedStatement stmt1 = conn.prepareStatement(countSQL);
			ResultSet rs1 = stmt1.executeQuery();

			long count = 0;
			if (rs1.next()) {
				count = rs1.getInt("cnt");
			}
			rs1.close();
			stmt1.close();

			if (conn != null) {
				conn.close();
			}

			return count;
		}
	}

	@Override
	public List<VinPlayAgentModel> listCountUserInListAgent1(String nick_name, String refcode, String fromTime,
			String endTime, int page, int maxItem) throws SQLException {
		List<VinPlayAgentModel> results = new ArrayList<VinPlayAgentModel>();

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			String getListSql = "SELECT  * FROM "
					+ " ( SELECT u.id, u.nick_name, u.user_name, u.create_time, ag.referral_code as refcode FROM vinplay.agency_code ag LEFT JOIN vinplay.users u ON u.id = ag.user_id ) ag "
					+ " LEFT JOIN ( SELECT u.referral_code, count(*) AS cnt FROM vinplay.users u WHERE referral_code IS NOT NULL GROUP BY u.referral_code ) c "
					+ " ON ag.refcode = c.referral_code ORDER BY cnt desc";
			PreparedStatement stmt = conn.prepareStatement(getListSql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				VinPlayAgentModel agent = new VinPlayAgentModel();
				agent.setAgent_id(rs.getInt("id"));
				agent.setNick_name(rs.getString("nick_name"));
				agent.setReferral_code(rs.getString("user_name"));
				agent.setReferral_code(rs.getString("refcode"));
				agent.setCreate_time(rs.getDate("create_time"));
				agent.setCountUser(rs.getLong("cnt"));
				results.add(agent);
			}
			rs.close();
			stmt.close();
			return results;
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public Long countUserOfAgent(String referral_code, String nick_name, String fromTime, String endTime, Long doanhThu)
			throws SQLException {
		List<UserOfAgentModel> users = new ArrayList<>();
		long count = 0;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "select count(*) as cnt FROM vinplay.users as a where referral_code = ? "
					+ ((nick_name == null || nick_name.isEmpty()) ? "" : (" and nick_name = '" + nick_name + "' "))
					+ ((fromTime == null || endTime == null || fromTime.isEmpty() || endTime.isEmpty()) ? ""
							: (" and create_time >= '" + fromTime + " 00:00:00" + "' and create_time <= '" + endTime
									+ " 23:59:59" + "'"));
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, referral_code);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt("cnt");
			}
			return count;
		}
	}

	@Override
	public List<UserDetailAgentModel> getUserDetailAgent(String nick_name, String fromTime, String endTime)
			throws SQLException {
		List<UserDetailAgentModel> users = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT * FROM vinplay.log_report_user WHERE 1=1"
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and log_report_user.nick_name = ?"))
					+ ((fromTime == null || fromTime.trim().isEmpty()) ? "" : (" and log_report_user.time_report >= ?"))
					+ ((endTime == null || endTime.trim().isEmpty()) ? "" : (" and log_report_user.time_report <= ?"));
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				index++;
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime);
				index++;
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime);
				index++;
			}
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				UserDetailAgentModel user = new UserDetailAgentModel();
				user.setNick_name(rs.getString("nick_name"));
				user.setTime(rs.getDate("time_report"));
				user.setWm(rs.getLong("wm"));
				user.setWm_win(rs.getLong("wm_win"));
				user.setIbc(rs.getLong("ibc"));
				user.setIbc_win(rs.getLong("ibc_win"));
				user.setAg(rs.getLong("ag"));
				user.setAg_win(rs.getLong("ag_win"));
				user.setTlmn(rs.getLong("tlmn"));
				user.setTlmn_win(rs.getLong("tlmn_win"));
				user.setBacay(rs.getLong("bacay"));
				user.setBacay_win(rs.getLong("bacay_win"));
				user.setXocdia(rs.getLong("xocdia"));
				user.setXocdia_win(rs.getLong("xocdia_win"));
				user.setMinipoker(rs.getLong("minipoker"));
				user.setMinipoker_win(rs.getLong("minipoker_win"));
				user.setSlot_pokemon(rs.getLong("slot_pokemon"));
				user.setSlot_pokemon_win(rs.getLong("slot_pokemon_win"));
				user.setBaucua(rs.getLong("baucua"));
				user.setBaucua_win(rs.getLong("baucua_win"));
				user.setTaixiu(rs.getLong("taixiu"));
				user.setTaixiu_win(rs.getLong("taixiu_win"));
				user.setCaothap(rs.getLong("caothap"));
				user.setCaothap_win(rs.getLong("caothap_win"));
				user.setSlot_bitcoin(rs.getLong("slot_bitcoin"));
				user.setSlot_bitcoin_win(rs.getLong("slot_bitcoin_win"));
				user.setSlot_taydu(rs.getLong("slot_taydu"));
				user.setSlot_taydu_win(rs.getLong("slot_taydu_win"));
				user.setSlot_angrybird(rs.getLong("slot_angrybird"));
				user.setSlot_angrybird_win(rs.getLong("slot_angrybird_win"));
				user.setSlot_thantai(rs.getLong("slot_thantai"));
				user.setSlot_thantai_win(rs.getLong("slot_thantai_win"));
				user.setSlot_thethao(rs.getLong("slot_thethao"));
				user.setSlot_thethao_win(rs.getLong("slot_thethao_win"));
				user.setSlot_chiemtinh(rs.getLong("slot_chiemtinh"));
				user.setSlot_chiemtinh_win(rs.getLong("slot_chiemtinh_win"));
				user.setTaixiu_st(rs.getLong("taixiu_st"));
				user.setTaixiu_st_win(rs.getLong("taixiu_st_win"));
				user.setFish(rs.getLong("fish"));
				user.setFish_win(rs.getLong("fish_win"));
				user.setDeposit(rs.getLong("deposit"));
				user.setWithdraw(rs.getLong("withdraw"));
				user.setSlot_bikini(rs.getLong("slot_bikini"));
				user.setSlot_bikini_win(rs.getLong("slot_bikini_win"));
				user.setSlot_galaxy(rs.getLong("slot_galaxy"));
				user.setSlot_galaxy_win(rs.getLong("slot_galaxy_win"));
				users.add(user);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return users;
		}
	}

	private List<String> AddFieldDefault(List<String> fields) {
		List<String> newFields = new ArrayList<>();
		List<String> fieldDefaults = Arrays.asList("id", "time_report", "nick_name", "code");
		newFields.addAll(fieldDefaults);
		newFields.addAll(fields);
		return newFields;
	}
	
	/*
	 * Lay danh sach user nap moi ma tu truoc den gio chua bao gio nap (chi danh cho dai ly)
	 */
	@Override
	public List<Map<String, Object>> getUsersDepositFirstInDay(String currentTime, String nickname, String referCode,
			int page, int maxItem) throws SQLException {
		page = page < 1 ? 0 : page - 1;
		List<Map<String, Object>> data = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String codeCondition = (referCode == null || referCode.trim().isEmpty()) ? ""
					: (" and ((select refercode) = ?)");
			String nicknameCondition = (nickname == null || nickname.trim().isEmpty()) ? ""
					: (" and (select nickname = ?)");
			String sqlCount = "select nick_name nickname, deposit depositCount,time_report timeReport,"
					+ " (select referral_code from users where nick_name = (select nickname)) refercode,"
					+ " (SELECT COUNT(nick_name) FROM log_count_user_play where time_report < ? and deposit > 0 and nick_name = (select nickname)) rq"
					+ " from log_count_user_play" + " having rq < 1 and deposit > 0 and time_report = ? "
					+ codeCondition + nicknameCondition;
			String sqlCount1 = " SELECT FOUND_ROWS() as total";
			String sql = "select nick_name nickname, deposit depositCount,time_report timeReport,"
					+ " (select referral_code from users where nick_name = (select nickname)) refercode,"
					+ " (SELECT COUNT(nick_name) FROM log_count_user_play where time_report < ? and deposit > 0 and nick_name = (select nickname)) rq"
					+ " from log_count_user_play" + " having rq < 1 and deposit > 0 and time_report = ? "
					+ codeCondition + nicknameCondition + " order by time_report, nick_name, depositcount limit ?,?";
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmtCount1 = conn.prepareStatement(sqlCount1);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(currentTime == null || currentTime.trim().isEmpty())) {
				stmt.setString(index, currentTime);
				stmtCount.setString(index, currentTime);
				index++;
			}

			if (!(currentTime == null || currentTime.trim().isEmpty())) {
				stmt.setString(index, currentTime);
				stmtCount.setString(index, currentTime);
				index++;
			}

			if (!(referCode == null || referCode.trim().isEmpty())) {
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
			}

			if (!(nickname == null || nickname.trim().isEmpty())) {
				stmt.setString(index, nickname);
				stmtCount.setString(index, nickname);
				index++;
			}

			stmt.setInt(index++, page * maxItem);
			stmt.setInt(index++, maxItem);
			ResultSet rs = stmt.executeQuery();
			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rsCount1 = stmtCount1.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("timeReport", rs.getObject("timeReport"));
				objectMap.put("nickName", rs.getObject("nickName"));
				objectMap.put("code", rs.getObject("refercode"));
				objectMap.put("depositCount", rs.getObject("depositCount"));
				data.add(objectMap);
			}

			while (rsCount1.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount1.getObject("total") == null ? 0 : rsCount1.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		}
	}
	
	public List<Map<String, Object>> getUsersDepositFirstInDay(String fromTime, String endTime, String nickname, String referCode,
			int page, int maxItem) throws SQLException {
		page = page < 1 ? 0 : page - 1;
		List<Map<String, Object>> data = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String codeCondition = (referCode == null || referCode.trim().isEmpty()) ? ""
					: (" and ((select refercode) = ?)");
			String nicknameCondition = (nickname == null || nickname.trim().isEmpty()) ? ""
					: (" and (select nickname = ?)");
			String sqlCount = "select nick_name nickname, deposit depositCount,time_report timeReport,"
					+ " (select referral_code from users where nick_name = (select nickname)) refercode,"
					+ " (SELECT COUNT(nick_name) FROM log_count_user_play where time_report < ? and deposit > 0 and nick_name = (select nickname)) rq"
					+ " from log_count_user_play" + " having rq < 1 and deposit > 0 and time_report >= ? and time_report <= ? "
					+ codeCondition + nicknameCondition;
			String sqlCount1 = " SELECT FOUND_ROWS() as total";
			String sql = "select nick_name nickname, deposit depositCount,time_report timeReport,"
					+ " (select referral_code from users where nick_name = (select nickname)) refercode,"
					+ " (SELECT COUNT(nick_name) FROM log_count_user_play where time_report < ? and deposit > 0 and nick_name = (select nickname)) rq"
					+ " from log_count_user_play" + " having rq < 1 and deposit > 0 and time_report >= ? and time_report <= ? "
					+ codeCondition + nicknameCondition + " order by time_report, nick_name, depositcount limit ?,?";
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmtCount1 = conn.prepareStatement(sqlCount1);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime);
				stmtCount.setString(index, fromTime);
				index++;
			}

			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime + " 00:00:00");
				stmtCount.setString(index, fromTime + " 00:00:00");
				index++;
			}
			
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime + " 23:59:59");
				stmtCount.setString(index, endTime + " 23:59:59");
				index++;
			}

			if (!(referCode == null || referCode.trim().isEmpty())) {
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
			}

			if (!(nickname == null || nickname.trim().isEmpty())) {
				stmt.setString(index, nickname);
				stmtCount.setString(index, nickname);
				index++;
			}

			stmt.setInt(index++, page * maxItem);
			stmt.setInt(index++, maxItem);
			ResultSet rs = stmt.executeQuery();
			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rsCount1 = stmtCount1.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("timeReport", rs.getObject("timeReport"));
				objectMap.put("nickName", rs.getObject("nickName"));
				objectMap.put("code", rs.getObject("refercode"));
				objectMap.put("depositCount", rs.getObject("depositCount"));
				data.add(objectMap);
			}

			while (rsCount1.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount1.getObject("total") == null ? 0 : rsCount1.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		}
	}
	
	private String toDay() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = formatter.format(date);
		return dateString;
	}

	@Override
	public List<UserDetailAgentModel> getUserDetailAgentCurrentDay(String nick_name, String fromTime, String endTime)
			throws SQLException {
		List<UserDetailAgentModel> users = new ArrayList<>();

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT * FROM vinplay.log_report_user WHERE 1=1"
					+ (nick_name == null || nick_name.trim().isEmpty() ? "" : " and log_report_user.nick_name = ?")
					+ (fromTime == null || fromTime.trim().isEmpty() ? "" : " and time_report >= ?")
					+ (endTime == null || endTime.trim().isEmpty() ? "" : " and time_report <= ?");

			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index++, nick_name);
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index++, fromTime);
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime);
			}

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				UserDetailAgentModel user = new UserDetailAgentModel();
				user.setNick_name(rs.getString("nick_name"));
				user.setTime(rs.getDate("time_report"));
				user.setWm(rs.getLong("wm"));
				user.setWm_win(rs.getLong("wm_win"));
				user.setIbc(rs.getLong("ibc"));
				user.setIbc_win(rs.getLong("ibc_win"));
				user.setAg(rs.getLong("ag"));
				user.setAg_win(rs.getLong("ag_win"));
				user.setTlmn(rs.getLong("tlmn"));
				user.setTlmn_win(rs.getLong("tlmn_win"));
				user.setBacay(rs.getLong("bacay"));
				user.setBacay_win(rs.getLong("bacay_win"));
				user.setXocdia(rs.getLong("xocdia"));
				user.setXocdia_win(rs.getLong("xocdia_win"));
				user.setMinipoker(rs.getLong("minipoker"));
				user.setMinipoker_win(rs.getLong("minipoker_win"));
				user.setSlot_pokemon(rs.getLong("slot_pokemon"));
				user.setSlot_pokemon_win(rs.getLong("slot_pokemon_win"));
				user.setBaucua(rs.getLong("baucua"));
				user.setBaucua_win(rs.getLong("baucua_win"));
				user.setTaixiu(rs.getLong("taixiu"));
				user.setTaixiu_win(rs.getLong("taixiu_win"));
				user.setCaothap(rs.getLong("caothap"));
				user.setCaothap_win(rs.getLong("caothap_win"));
				user.setSlot_bitcoin(rs.getLong("slot_bitcoin"));
				user.setSlot_bitcoin_win(rs.getLong("slot_bitcoin_win"));
				user.setSlot_taydu(rs.getLong("slot_taydu"));
				user.setSlot_taydu_win(rs.getLong("slot_taydu_win"));
				user.setSlot_angrybird(rs.getLong("slot_angrybird"));
				user.setSlot_angrybird_win(rs.getLong("slot_angrybird_win"));
				user.setSlot_thantai(rs.getLong("slot_thantai"));
				user.setSlot_thantai_win(rs.getLong("slot_thantai_win"));
				user.setSlot_thethao(rs.getLong("slot_thethao"));
				user.setSlot_thethao_win(rs.getLong("slot_thethao_win"));
				user.setSlot_chiemtinh(rs.getLong("slot_chiemtinh"));
				user.setSlot_chiemtinh_win(rs.getLong("slot_chiemtinh_win"));
				user.setTaixiu_st(rs.getLong("taixiu_st"));
				user.setTaixiu_st_win(rs.getLong("taixiu_st_win"));
				user.setFish(rs.getLong("fish"));
				user.setFish_win(rs.getLong("fish_win"));
				user.setDeposit(rs.getLong("deposit"));
				user.setWithdraw(rs.getLong("withdraw"));
				user.setSlot_bikini(rs.getLong("slot_bikini"));
				user.setSlot_bikini_win(rs.getLong("slot_bikini_win"));
				user.setSlot_galaxy(rs.getLong("slot_galaxy"));
				user.setSlot_galaxy_win(rs.getLong("slot_galaxy_win"));
				user.setEbet(rs.getLong("ebet"));
				user.setEbet_win(rs.getLong("ebet_win"));
				users.add(user);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return users;
		}
	}

	@Override
	public List<Object> getLogUserDetail(String nick_name, String fromTime, String endTime, int page, int maxItem)
			throws Exception {
		page = page < 1 ? 0 : page - 1;

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT * FROM vinplay.log_report_user WHERE 1=1"
					+ (nick_name == null || nick_name.trim().isEmpty() ? "" : " and log_report_user.nick_name = ? ")
					+ (fromTime == null || fromTime.trim().isEmpty() ? "" : " and time_report >= ? ")
					+ (endTime == null || endTime.trim().isEmpty() ? "" : " and time_report <= ? ")
					+ " order by time_report, nick_name limit ?,?";

			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index++, nick_name);
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index++, fromTime);
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index++, endTime);
			}
			stmt.setInt(index++, page * maxItem);
			stmt.setInt(index++, maxItem);

			ResultSet rs = stmt.executeQuery();

			return DataUtils.convertToObjectArray(rs);
		}
	}

	@Override
	public long totalLogUserDetail(String nick_name, String fromTime, String endTime) throws Exception {
		List<UserDetailAgentModel> users = new ArrayList<>();
		long count = 0L;

		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT count(*) as cnt FROM vinplay.log_report_user WHERE 1=1"
					+ (nick_name == null || nick_name.trim().isEmpty() ? "" : " and log_report_user.nick_name = ? ")
					+ (fromTime == null || fromTime.trim().isEmpty() ? "" : " and time_report >= ? ")
					+ (endTime == null || endTime.trim().isEmpty() ? "" : " and time_report <= ? ");

			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index++, nick_name);
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index++, fromTime);
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index++, endTime);
			}

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt("cnt");
			}
			return count;
		}
	}

	@Override
	public List<AgentResponse> listAgentByClient(String client) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT nameagent,nickname,phone,address,parentid,`show`,`active`,`order`,`facebook`,`site`,commission_rate FROM useragent WHERE status='D' and parentid=-1 and `show`=1 and active=1 and site = '"
							+ client + "' order by `order` asc");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				String site = rs.getString("site");
				if (site != null && !site.equals(""))
					agent.fullName = "[" + site + "] " + rs.getString("nameagent");
				else
					agent.fullName = rs.getString("nameagent");
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.parentid = rs.getInt("parentid");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.orderNo = rs.getInt("order");
				agent.facebook = rs.getString("facebook");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(agent);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return results;
		}
	}

	@Override
	public AgentResponse listAgentByKey(String key) throws SQLException {
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT nameagent,nickname,phone,address,parentid,`show`,`active`,`order`,`facebook`,`site`,commission_rate FROM useragent WHERE `key` = '"
							+ key + "' order by `order` asc");
			ResultSet rs = stmt.executeQuery();
			AgentResponse agent = null;
			while (rs.next()) {
				agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.parentid = rs.getInt("parentid");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.orderNo = rs.getInt("order");
				agent.facebook = rs.getString("facebook");
				agent.commissionRate = rs.getDouble("commission_rate");
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return agent;
		}
	}

	@Override
	public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String nickNameSend, String nickNameRecieve,
			String status, String timeStart, String timeEnd, String top_ds, int page) {
		final ArrayList<LogAgentTranferMoneyResponse> results = new ArrayList<LogAgentTranferMoneyResponse>();
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		FindIterable iterable = null;
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		int numStart = (page - 1) * 50;
		int numEnd = 50;
		Object v2 = -1;
		objsort.put("_id", v2);
//        objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")
				|| nickNameRecieve != null && !nickNameRecieve.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickNameSend);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickNameRecieve);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (top_ds != null && !top_ds.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(top_ds)));
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", obj);
		}
		iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson) new Document(conditions)).sort((Bson) objsort)
				.skip(numStart).limit(50);
		iterable.forEach((Block) new Block<Document>() {

			public void apply(Document document) {
				LogAgentTranferMoneyResponse trans = new LogAgentTranferMoneyResponse();
				trans.nick_name_send = document.getString((Object) "nick_name_send");
				trans.nick_name_receive = document.getString((Object) "nick_name_receive");
				trans.status = document.getInteger((Object) "status");
				trans.trans_time = document.getString((Object) "trans_time");
				trans.fee = document.getLong((Object) "fee");
				trans.money_send = document.getLong((Object) "money_send");
				trans.money_receive = document.getLong((Object) "money_receive");
				trans.top_ds = document.getInteger((Object) "top_ds");
				results.add(trans);
			}
		});
		return results;
	}

	@Override
	public List<AgentResponse> listUserAgent(String nickName) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		String sql = "";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			PreparedStatement stmt;
			if (!nickName.isEmpty()) {
				sql = "SELECT nameagent,nickname,phone,address,id,parentid,`show`,active,percent_bonus_vincard,commission_rate FROM useragent WHERE status='D' and `active` = 1 and  nickname=?";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, nickName);
			} else {
				sql = "SELECT nameagent,nickname,phone,address,id,parentid,`show`,active,percent_bonus_vincard,commission_rate FROM useragent WHERE status='D' and `active` = 1";
				stmt = conn.prepareStatement(sql);
			}
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.id = rs.getInt("id");
				agent.parentid = rs.getInt("parentid");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.percent = rs.getInt("percent_bonus_vincard");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(0, agent);
			}
			rs.close();
			stmt.close();
		}
		return results;
	}

	@Override
	public List<AgentResponse> listUserAgentByParentID(int ParentID) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		String sql = "";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			sql = "SELECT nameagent,nickname,phone,address,id,parentid FROM useragent WHERE status='D' and parentid=?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setInt(1, ParentID);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.id = rs.getInt("id");
				agent.parentid = rs.getInt("parentid");
				results.add(0, agent);
			}
			rs.close();
			stmt.close();
		}
		return results;
	}

	@Override
	public TranferAgentResponse searchAgentTranfer(String nickName, String status, String timeStart, String timeEnd)
			throws SQLException {
		long totalBuy1 = 0L;
		long totalSale1 = 0L;
		long totalFeeBuy1 = 0L;
		long totalFeeSale1 = 0L;
		int countBuy1 = 0;
		int countSale1 = 0;
		long totalBuy2 = 0L;
		long totalSale2 = 0L;
		long totalFeeBuy2 = 0L;
		long totalFeeSale2 = 0L;
		int countBuy2 = 0;
		int countSale2 = 0;
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		if (collection != null) {
			HashMap<String, Object> statusmua1 = new HashMap<String, Object>();
			HashMap<String, Object> statusmua2 = new HashMap<String, Object>();
			HashMap<String, Object> statusban1 = new HashMap<String, Object>();
			HashMap<String, Object> statusban2 = new HashMap<String, Object>();
			HashMap<String, Object> statusfeeban1 = new HashMap<String, Object>();
			HashMap<String, Object> statusfeeban2 = new HashMap<String, Object>();
			HashMap<String, Object> statusfeemua1 = new HashMap<String, Object>();
			HashMap<String, Object> statusfeemua2 = new HashMap<String, Object>();
			BasicDBObject obj = new BasicDBObject();
			BasicDBObject stt1 = new BasicDBObject("status", (Object) 1);
			BasicDBObject stt2 = new BasicDBObject("status", (Object) 2);
			BasicDBObject stt3 = new BasicDBObject("status", (Object) 3);
			BasicDBObject stt4 = new BasicDBObject("status", (Object) 4);
			BasicDBObject stt5 = new BasicDBObject("status", (Object) 5);
			BasicDBObject stt6 = new BasicDBObject("status", (Object) 6);
			BasicDBObject stt7 = new BasicDBObject("status", (Object) 7);
			BasicDBObject stt8 = new BasicDBObject("status", (Object) 8);
			statusmua1.put("top_ds", 1);
			statusmua2.put("top_ds", 1);
			statusban1.put("top_ds", 1);
			statusban2.put("top_ds", 1);
			if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
				obj.put("$gte", (Object) timeStart);
				obj.put("$lte", (Object) timeEnd);
				statusmua1.put("trans_time", (Object) obj);
				statusban1.put("trans_time", (Object) obj);
				statusfeemua1.put("trans_time", (Object) obj);
				statusfeeban1.put("trans_time", (Object) obj);
				statusmua2.put("trans_time", (Object) obj);
				statusban2.put("trans_time", (Object) obj);
				statusfeemua2.put("trans_time", (Object) obj);
				statusfeeban2.put("trans_time", (Object) obj);
			}
			if (!status.isEmpty()) {
				statusmua1.put("status", status);
				statusban1.put("status", status);
				statusfeemua1.put("status", status);
				statusfeeban1.put("status", status);
				statusmua2.put("status", status);
				statusban2.put("status", status);
				statusfeemua2.put("status", status);
				statusfeeban2.put("status", status);
			} else {
				ArrayList<BasicDBObject> sttdsmua1 = new ArrayList<BasicDBObject>();
				sttdsmua1.add(stt1);
				statusmua1.put("$or", sttdsmua1);
				statusmua1.put("nick_name_receive", nickName);
				ArrayList<BasicDBObject> sttdsmua2 = new ArrayList<BasicDBObject>();
				sttdsmua2.add(stt2);
				statusmua2.put("$or", sttdsmua2);
				statusmua2.put("nick_name_receive", nickName);
				ArrayList<BasicDBObject> sttdsban1 = new ArrayList<BasicDBObject>();
				sttdsban1.add(stt3);
				statusban1.put("$or", sttdsban1);
				statusban1.put("nick_name_send", nickName);
				ArrayList<BasicDBObject> sttdsban2 = new ArrayList<BasicDBObject>();
				sttdsban2.add(stt6);
				statusban2.put("$or", sttdsban2);
				statusban2.put("nick_name_send", nickName);
				ArrayList<BasicDBObject> sttfeemua1 = new ArrayList<BasicDBObject>();
				sttfeemua1.add(stt1);
				sttfeemua1.add(stt7);
				statusfeemua1.put("$or", sttfeemua1);
				statusfeemua1.put("nick_name_receive", nickName);
				ArrayList<BasicDBObject> sttfeemua2 = new ArrayList<BasicDBObject>();
				sttfeemua2.add(stt2);
				statusfeemua2.put("$or", sttfeemua2);
				statusfeemua2.put("nick_name_receive", nickName);
				ArrayList<BasicDBObject> sttfeeban1 = new ArrayList<BasicDBObject>();
				sttfeeban1.add(stt3);
				sttfeeban1.add(stt4);
				sttfeeban1.add(stt5);
				statusfeeban1.put("$or", sttfeeban1);
				statusfeeban1.put("nick_name_send", nickName);
				ArrayList<BasicDBObject> sttfeeban2 = new ArrayList<BasicDBObject>();
				sttfeeban2.add(stt6);
				sttfeeban2.add(stt8);
				statusfeeban2.put("$or", sttfeeban2);
				statusfeeban2.put("nick_name_send", nickName);
			}
			Document dsmua1 = (Document) collection
					.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(statusmua1)),
							new Document("$group", (Object) new Document("_id", (Object) "$nick_name_receive")
									.append("money", (Object) new Document("$sum", (Object) "$money_send"))) }))
					.first();
			Document dsmua2 = (Document) collection
					.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(statusmua2)),
							new Document("$group", (Object) new Document("_id", (Object) "$nick_name_receive")
									.append("money", (Object) new Document("$sum", (Object) "$money_send"))) }))
					.first();
			Document dsban1 = (Document) collection
					.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(statusban1)),
							new Document("$group", (Object) new Document("_id", (Object) "$nick_name_send")
									.append("money", (Object) new Document("$sum", (Object) "$money_send"))) }))
					.first();
			Document dsban2 = (Document) collection
					.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(statusban2)),
							new Document("$group", (Object) new Document("_id", (Object) "$nick_name_send")
									.append("money", (Object) new Document("$sum", (Object) "$money_send"))) }))
					.first();
			Document feemua1 = (Document) collection
					.aggregate(
							Arrays.asList(
									new Document[] {
											new Document("$match", (Object) new Document(
													statusfeemua1)),
											new Document("$group",
													(Object) new Document("_id", (Object) "$nick_name_receive")
															.append("money",
																	(Object) new Document("$sum", (Object) "$fee"))
															.append("count",
																	(Object) new Document("$sum", (Object) 1))) }))
					.first();
			Document feemua2 = (Document) collection
					.aggregate(
							Arrays.asList(
									new Document[] {
											new Document("$match", (Object) new Document(
													statusfeemua2)),
											new Document("$group",
													(Object) new Document("_id", (Object) "$nick_name_receive")
															.append("money",
																	(Object) new Document("$sum", (Object) "$fee"))
															.append("count",
																	(Object) new Document("$sum", (Object) 1))) }))
					.first();
			Document feeban1 = (Document) collection
					.aggregate(
							Arrays.asList(
									new Document[] {
											new Document("$match", (Object) new Document(
													statusfeeban1)),
											new Document("$group",
													(Object) new Document("_id", (Object) "$nick_name_send")
															.append("money",
																	(Object) new Document("$sum", (Object) "$fee"))
															.append("count",
																	(Object) new Document("$sum", (Object) 1))) }))
					.first();
			Document feeban2 = (Document) collection
					.aggregate(
							Arrays.asList(
									new Document[] {
											new Document("$match", (Object) new Document(
													statusfeeban2)),
											new Document("$group",
													(Object) new Document("_id", (Object) "$nick_name_send")
															.append("money",
																	(Object) new Document("$sum", (Object) "$fee"))
															.append("count",
																	(Object) new Document("$sum", (Object) 1))) }))
					.first();
			if (dsmua1 != null) {
				totalBuy1 = dsmua1.getLong((Object) "money");
			}
			if (dsban1 != null) {
				totalSale1 = dsban1.getLong((Object) "money");
			}
			if (feemua1 != null) {
				totalFeeBuy1 = feemua1.getLong((Object) "money");
				countBuy1 = feemua1.getInteger((Object) "count");
			}
			if (feeban1 != null) {
				totalFeeSale1 = feeban1.getLong((Object) "money");
				countSale1 = feeban1.getInteger((Object) "count");
			}
			if (dsmua2 != null) {
				totalBuy2 = dsmua2.getLong((Object) "money");
			}
			if (dsban2 != null) {
				totalSale2 = dsban2.getLong((Object) "money");
			}
			if (feemua2 != null) {
				totalFeeBuy2 = feemua2.getLong((Object) "money");
				countBuy2 = feemua2.getInteger((Object) "count");
			}
			if (feeban2 != null) {
				totalFeeSale2 = feeban2.getLong((Object) "money");
				countSale2 = feeban2.getInteger((Object) "count");
			}
		}
		TranferAgentResponse trans = new TranferAgentResponse(nickName, totalBuy1, totalSale1, totalFeeBuy1,
				totalFeeSale1, countBuy1, countSale1, totalBuy2, totalSale2, totalFeeBuy2, totalFeeSale2, countBuy2,
				countSale2);
		return trans;
	}

	@Override
	public AgentDSModel getDS(String nickName, String timeStart, String timeEnd, boolean bAgent1) throws SQLException {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> statusmua = new HashMap<String, Object>();
		HashMap<String, Object> statusban = new HashMap<String, Object>();
		if (!timeStart.isEmpty() && !timeEnd.isEmpty()) {
			BasicDBObject obj = new BasicDBObject();
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			statusmua.put("trans_time", (Object) obj);
			statusban.put("trans_time", (Object) obj);
		}
		statusmua.put("top_ds", 1);
		statusmua.put("nick_name_receive", nickName);
		statusban.put("top_ds", 1);
		statusban.put("nick_name_send", nickName);
		if (bAgent1) {
			statusmua.put("status", 1);
			statusban.put("status", 3);
		} else {
			statusmua.put("status", 2);
			statusban.put("status", 6);
		}
		long dsMua = 0L;
		long dsBan = 0L;
		int gdMua = 0;
		int gdBan = 0;
		Document dsmua = (Document) collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(statusmua)),
						new Document("$group",
								(Object) new Document("_id", (Object) "$nick_name_receive")
										.append("money", (Object) new Document("$sum", (Object) "$money_send"))
										.append("count", (Object) new Document("$sum", (Object) 1))) }))
				.first();
		Document dsban = (Document) collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(statusban)),
						new Document("$group",
								(Object) new Document("_id", (Object) "$nick_name_send")
										.append("money", (Object) new Document("$sum", (Object) "$money_send"))
										.append("count", (Object) new Document("$sum", (Object) 1))) }))
				.first();
		if (dsmua != null) {
			dsMua = dsmua.getLong((Object) "money");
			gdMua = dsmua.getInteger((Object) "count");
		}
		if (dsban != null) {
			dsBan = dsban.getLong((Object) "money");
			gdBan = dsban.getInteger((Object) "count");
		}
		long ds = dsMua + dsBan;
		int gd = gdMua + gdBan;
		AgentDSModel res = new AgentDSModel(dsMua, dsBan, ds, gdMua, gdBan, gd);
		return res;
	}

	@Override
	public Map<String, ArrayList<String>> getAllAgent() throws SQLException {
		HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			String sql = " SELECT id, nickname, percent_bonus_vincard  FROM useragent  WHERE status = 'D'    AND parentid = -1    AND active = 1 ";
			PreparedStatement stmt = conn.prepareStatement(
					" SELECT id, nickname, percent_bonus_vincard  FROM useragent  WHERE status = 'D'    AND parentid = -1    AND active = 1 ");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				ArrayList<String> temp = new ArrayList<String>();
				int id = rs.getInt("id");
				String agent1 = rs.getString("nickname");
				int percent = rs.getInt("percent_bonus_vincard");
				StringBuilder agent2 = new StringBuilder("");
				String sql2 = " SELECT nickname FROM useragent WHERE status = 'D' AND parentid = ? ";
				PreparedStatement stmt2 = conn
						.prepareStatement(" SELECT nickname FROM useragent WHERE status = 'D' AND parentid = ? ");
				stmt2.setInt(1, id);
				ResultSet rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					agent2.append(rs2.getString("nickname")).append(",");
				}
				if (agent2.length() >= 1) {
					agent2.delete(agent2.length() - 1, agent2.length());
				}
				rs2.close();
				stmt2.close();
				temp.add(0, agent2.toString());
				temp.add(1, String.valueOf(percent));
				map.put(agent1, temp);
			}
			rs.close();
			stmt.close();
		}
		return map;
	}

	@Override
	public Map<String, String> getAllNameAgent() throws SQLException {
		HashMap<String, String> map = new HashMap<String, String>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			String sql = "SELECT id,nickname FROM useragent WHERE status='D' and parentid = -1 and active = 1";
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT id,nickname FROM useragent WHERE status='D' and parentid = -1 and active = 1");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				int id = rs.getInt("id");
				String agent1 = rs.getString("nickname");
				StringBuilder agent2 = new StringBuilder("");
				String sql2 = "SELECT nickname FROM useragent WHERE status='D' and parentid = ?";
				PreparedStatement stmt2 = conn
						.prepareStatement("SELECT nickname FROM useragent WHERE status='D' and parentid = ?");
				stmt2.setInt(1, id);
				ResultSet rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					agent2.append(rs2.getString("nickname")).append(",");
				}
				if (agent2.length() >= 1) {
					agent2.delete(agent2.length() - 1, agent2.length());
				}
				rs2.close();
				stmt2.close();
				map.put(agent1, agent2.toString());
			}
			rs.close();
			stmt.close();
		}
		return map;
	}

	@Override
	public boolean checkRefundFeeAgent(String nickname, String month) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		conditions.put("nick_name", nickname);
		conditions.put("month", month);
		conditions.put("code", Integer.parseInt("0"));
		FindIterable iterable = db.getCollection("log_refund_fee_agent").find((Bson) new Document(conditions)).limit(1);
		Document document = iterable != null ? (Document) iterable.first() : null;
		return document != null;
	}

	@Override
	public List<RefundFeeAgentMessage> getLogRefundFeeAgent(String nickname, String month) {
		final ArrayList<RefundFeeAgentMessage> results = new ArrayList<RefundFeeAgentMessage>();
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
		if (!nickname.isEmpty()) {
			conditions.put("nick_name", nickname);
		}
		if (!month.isEmpty()) {
			conditions.put("month", month);
		}
		FindIterable iterable = db.getCollection("log_refund_fee_agent").find((Bson) new Document(conditions));
		iterable.forEach((Block) new Block<Document>() {

			public void apply(Document document) {
				RefundFeeAgentMessage trans = new RefundFeeAgentMessage(document.getString((Object) "nick_name"),
						document.getLong((Object) "fee_1").longValue(),
						document.getDouble((Object) "ratio_1").doubleValue(),
						document.getLong((Object) "fee_2").longValue(),
						document.getDouble((Object) "ratio_2").doubleValue(),
						document.getLong((Object) "fee_2_more") == null ? 0L : document.getLong((Object) "fee_2_more"),
						document.getDouble((Object) "ratio_2_more") == null ? 0.0
								: document.getDouble((Object) "ratio_2_more"),
						document.getLong((Object) "fee").longValue(), document.getString((Object) "month"),
						document.getInteger((Object) "code").intValue(), document.getString((Object) "description"),
						document.getLong((Object) "fee_vinplay_card") == null ? 0L
								: document.getLong((Object) "fee_vinplay_card"),
						document.getLong((Object) "fee_vin_cash") == null ? 0L
								: document.getLong((Object) "fee_vin_cash"),
						document.getInteger((Object) "percent") == null ? 0 : document.getInteger((Object) "percent"));
				String createTime = document.getString((Object) "time_log");
				trans.setCreateTime(createTime);
				results.add(trans);
			}
		});
		return results;
	}

	@Override
	public long countsearchAgentTranferMoney(String nickNameSend, String nickNameRecieve, String status,
			String timeStart, String timeEnd, String top_ds) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")) {
			conditions.put("nick_name_send", nickNameSend);
		}
		if (nickNameRecieve != null && !nickNameRecieve.equals("")) {
			conditions.put("nick_name_receive", nickNameRecieve);
		}
		if (top_ds != null && !top_ds.equals("")) {
			conditions.put("top_ds", Integer.parseInt(top_ds));
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.parseInt(status));
		}
		if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		long totalRecord = db.getCollection("log_chuyen_tien_dai_ly").count((Bson) new Document(conditions));
		return totalRecord;
	}

	@Override
	public long totalMoneyVinReceiveFromAgent(String nickNameSend, String nickNameRecieve, String status,
			String timeStart, String timeEnd, String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")
				|| nickNameRecieve != null && !nickNameRecieve.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickNameSend);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickNameRecieve);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(topDS)));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_receive")
								.append("money", (Object) new Document("$sum", (Object) "$money_receive"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public long totalMoneyVinSendFromAgent(String nickNameSend, String nickNameRecieve, String status, String timeStart,
			String timeEnd, String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")
				|| nickNameRecieve != null && !nickNameRecieve.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickNameSend);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickNameRecieve);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(topDS)));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_send").append("money",
								(Object) new Document("$sum", (Object) "$money_send"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public long totalMoneyVinFeeFromAgent(String nickNameSend, String nickNameRecieve, String status, String timeStart,
			String timeEnd, String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")
				|| nickNameRecieve != null && !nickNameRecieve.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickNameSend);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickNameRecieve);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(topDS)));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "").append("money",
								(Object) new Document("$sum", (Object) "$fee"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public List<LogAgentTranferMoneyResponse> searchAgentTranferMoneyVinSale(String nickName, String timeStart,
			String timeEnd, String type, int page, int totalRecord) {
		BasicDBObject nnSend;
		BasicDBObject nnReceive;
		ArrayList<BasicDBObject> myList;
		BasicDBObject query2;
		BasicDBObject query1;
		ArrayList<BasicDBObject> lstNN;
		final ArrayList<LogAgentTranferMoneyResponse> results = new ArrayList<LogAgentTranferMoneyResponse>();
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		BasicDBObject objNN = new BasicDBObject();
		BasicDBObject objSTT = new BasicDBObject();
		BasicDBList lstAll = new BasicDBList();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		FindIterable iterable = null;
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		int numStart = (page - 1) * totalRecord;
		objsort.put("_id", -1);
		if (type.equals("1")) {
			if (nickName != null && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList<BasicDBObject>();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 1);
			query2 = new BasicDBObject("status", (Object) 2);
			myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (type.equals("2")) {
			if (nickName != null && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 3);
			query2 = new BasicDBObject("status", (Object) 6);
			myList = new ArrayList();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson) new Document(conditions)).sort((Bson) objsort)
				.skip(numStart).limit(totalRecord);
		iterable.forEach((Block) new Block<Document>() {

			public void apply(Document document) {
				LogAgentTranferMoneyResponse trans = new LogAgentTranferMoneyResponse();
				trans.nick_name_send = document.getString((Object) "nick_name_send");
				trans.nick_name_receive = document.getString((Object) "nick_name_receive");
				trans.status = document.getInteger((Object) "status");
				trans.trans_time = document.getString((Object) "trans_time");
				trans.fee = document.getLong((Object) "fee");
				trans.money_send = document.getLong((Object) "money_send");
				trans.money_receive = document.getLong((Object) "money_receive");
				results.add(trans);
			}
		});
		return results;
	}

	@Override
	public long countSearchAgentTranferMoneyVinSale(String nickName, String timeStart, String timeEnd, String type) {
		BasicDBObject query2;
		BasicDBObject query1;
		ArrayList<BasicDBObject> lstNN;
		ArrayList<BasicDBObject> myList;
		BasicDBObject nnSend;
		BasicDBObject nnReceive;
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		BasicDBObject objNN = new BasicDBObject();
		BasicDBObject objSTT = new BasicDBObject();
		BasicDBList lstAll = new BasicDBList();
		objsort.put("_id", -1);
		if (type.equals("1")) {
			if (!nickName.equals(null) && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList<BasicDBObject>();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 1);
			query2 = new BasicDBObject("status", (Object) 2);
			myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (type.equals("2")) {
			if (!nickName.equals(null) && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 3);
			query2 = new BasicDBObject("status", (Object) 6);
			myList = new ArrayList();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (!(timeStart.equals(null) || timeStart.equals("") || timeEnd.equals(null) || timeEnd.equals(""))) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		long totalRecord = db.getCollection("log_chuyen_tien_dai_ly").count((Bson) new Document(conditions));
		return totalRecord;
	}

	@Override
	public long totalMoneyVinReceiveFromAgentByStatus(String nickName, String type, String timeStart, String timeEnd) {
		BasicDBObject nnReceive;
		BasicDBObject nnSend;
		BasicDBObject query2;
		BasicDBObject query1;
		ArrayList<BasicDBObject> lstNN;
		ArrayList<BasicDBObject> myList;
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		BasicDBObject objNN = new BasicDBObject();
		BasicDBObject objSTT = new BasicDBObject();
		BasicDBList lstAll = new BasicDBList();
		objsort.put("_id", -1);
		if (type.equals("1")) {
			if (!nickName.equals(null) && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList<BasicDBObject>();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 1);
			query2 = new BasicDBObject("status", (Object) 2);
			myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (type.equals("2")) {
			if (!nickName.equals(null) && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 3);
			query2 = new BasicDBObject("status", (Object) 6);
			myList = new ArrayList();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (!(timeStart.equals(null) || timeStart.equals("") || timeEnd.equals(null) || timeEnd.equals(""))) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_receive")
								.append("money", (Object) new Document("$sum", (Object) "$money_receive"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public long totalMoneyVinSendFromAgentByStatus(String nickName, String type, String timeStart, String timeEnd) {
		BasicDBObject nnReceive;
		BasicDBObject nnSend;
		BasicDBObject query2;
		BasicDBObject query1;
		ArrayList<BasicDBObject> lstNN;
		ArrayList<BasicDBObject> myList;
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		BasicDBObject objNN = new BasicDBObject();
		BasicDBObject objSTT = new BasicDBObject();
		BasicDBList lstAll = new BasicDBList();
		objsort.put("_id", -1);
		if (type.equals("1")) {
			if (!nickName.equals(null) && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList<BasicDBObject>();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 1);
			query2 = new BasicDBObject("status", (Object) 2);
			myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (type.equals("2")) {
			if (!nickName.equals(null) && !nickName.equals("")) {
				nnSend = new BasicDBObject("nick_name_send", (Object) nickName);
				nnReceive = new BasicDBObject("nick_name_receive", (Object) nickName);
				lstNN = new ArrayList();
				lstNN.add(nnSend);
				lstNN.add(nnReceive);
				objNN.put("$or", lstNN);
			}
			query1 = new BasicDBObject("status", (Object) 3);
			query2 = new BasicDBObject("status", (Object) 6);
			myList = new ArrayList();
			myList.add(query1);
			myList.add(query2);
			objSTT.put("$or", myList);
			lstAll.add((Object) objNN);
			lstAll.add((Object) objSTT);
			conditions.put("$and", (Object) lstAll);
		}
		if (!(timeStart.equals(null) || timeStart.equals("") || timeEnd.equals(null) || timeEnd.equals(""))) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_send").append("money",
								(Object) new Document("$sum", (Object) "$money_send"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public boolean updateTopDsFromAgent(String nickNameSend, String nickNameReceive, String timeLog, String topds) {
		MongoDatabase db = MongoDBConnectionFactory.getDB();
		MongoCollection col = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
		conditions.put("trans_time", timeLog);
		conditions.put("nick_name_send", nickNameSend);
		conditions.put("nick_name_receive", nickNameReceive);
		col.updateOne((Bson) new Document(conditions),
				(Bson) new Document("$set", (Object) new Document("top_ds", (Object) Integer.parseInt(topds))));
		return true;
	}

	@Override
	public boolean updateTopDsFromAgentMySQL(String nickNameSend, String nickNameReceive, String timeLog, String topds)
			throws SQLException {
		String sql = " UPDATE vinplay.log_tranfer_agent  SET top_ds = ?,      update_time = ?  WHERE trans_time = ?        AND nick_name_send = ?        AND nick_name_receive = ? ";
		PreparedStatement stmt = null;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			stmt = conn.prepareStatement(
					" UPDATE vinplay.log_tranfer_agent  SET top_ds = ?,      update_time = ?  WHERE trans_time = ?        AND nick_name_send = ?        AND nick_name_receive = ? ");
			stmt.setInt(1, Integer.parseInt(topds));
			stmt.setString(2, DateTimeUtils.getCurrentTime((String) "yyyy-MM-dd HH:mm:ss"));
			stmt.setString(3, timeLog);
			stmt.setString(4, nickNameSend);
			stmt.setString(5, nickNameReceive);
			stmt.executeUpdate();
			stmt.close();
		}
		return true;
	}

	@Override
	public boolean logBonusTopDS(BonusTopDSModel model) {
		MongoDatabase db = MongoDBConnectionFactory.getDB();
		MongoCollection col = db.getCollection("log_bonus_top_ds_agent");
		Document doc = new Document();
		doc.append("nick_name", (Object) model.getNickname());
		doc.append("ds", (Object) model.getDs());
		doc.append("top", (Object) model.getTop());
		doc.append("bonus_fix", (Object) model.getBonusFix());
		doc.append("bonus_more", (Object) model.getBonusMore());
		doc.append("ds_2", (Object) model.getDs2());
		doc.append("top_2", (Object) model.getTop2());
		doc.append("bonus_fix_2", (Object) model.getBonusFix2());
		doc.append("bonus_more_2", (Object) model.getBonusMore2());
		doc.append("bonus_total", (Object) model.getBonusTotal());
		doc.append("month", (Object) model.getMonth());
		doc.append("code", (Object) model.getCode());
		doc.append("description", (Object) model.getDescription());
		doc.append("time_log", (Object) model.getTimeLog());
		doc.append("create_time", (Object) new Date());
		doc.append("bonus_vinplay_card", (Object) model.getBonusVinplayCard());
		doc.append("bonus_vin_cash", (Object) model.getBonusVinCash());
		doc.append("percent", (Object) model.getPercent());
		col.insertOne((Object) doc);
		return true;
	}

	@Override
	public boolean checkBonusTopDS(String nickname, String month) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		conditions.put("nick_name", nickname);
		conditions.put("month", month);
		conditions.put("code", Integer.parseInt("0"));
		FindIterable iterable = db.getCollection("log_bonus_top_ds_agent").find((Bson) new Document(conditions))
				.limit(1);
		Document document = iterable != null ? (Document) iterable.first() : null;
		return document != null;
	}

	@Override
	public List<BonusTopDSModel> getLogBonusTopDS(String nickname, String month) {
		final ArrayList<BonusTopDSModel> results = new ArrayList<BonusTopDSModel>();
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
		if (nickname != null && !nickname.equals("")) {
			conditions.put("nick_name", nickname);
		}
		if (month != null && !month.equals("")) {
			conditions.put("month", month);
		}
		FindIterable iterable = db.getCollection("log_bonus_top_ds_agent").find((Bson) new Document(conditions));
		iterable.forEach((Block) new Block<Document>() {

			public void apply(Document document) {
				BonusTopDSModel model = new BonusTopDSModel(document.getString((Object) "nick_name"),
						document.getLong((Object) "ds"), document.getInteger((Object) "top"),
						document.getLong((Object) "bonus_fix"), document.getLong((Object) "bonus_more"),
						document.getLong((Object) "ds_2") == null ? 0L : document.getLong((Object) "ds_2"),
						document.getInteger((Object) "top_2") == null ? 0 : document.getInteger((Object) "top_2"),
						document.getLong((Object) "bonus_fix_2") == null ? 0L
								: document.getLong((Object) "bonus_fix_2"),
						document.getLong((Object) "bonus_more_2") == null ? 0L
								: document.getLong((Object) "bonus_more_2"),
						document.getLong((Object) "bonus_total"), document.getString((Object) "month"),
						document.getInteger((Object) "code"), document.getString((Object) "description"),
						document.getString((Object) "time_log"),
						document.getLong((Object) "bonus_vinplay_card") == null ? 0L
								: document.getLong((Object) "bonus_vinplay_card"),
						document.getLong((Object) "bonus_vin_cash") == null ? 0L
								: document.getLong((Object) "bonus_vin_cash"),
						document.getInteger((Object) "percent") == null ? 0 : document.getInteger((Object) "percent"));
				results.add(model);
			}
		});
		return results;
	}

	@Override
	public TranferMoneyAgent getTransferMoneyAgent(String nickNameSend, String nickNameReceive, String timeLog) {
		TranferMoneyAgent results = null;
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection col = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, String> conditions = new HashMap<String, String>();
		conditions.put("trans_time", timeLog);
		conditions.put("nick_name_send", nickNameSend);
		conditions.put("nick_name_receive", nickNameReceive);
		Document dc = (Document) col.find((Bson) new Document(conditions)).first();
		if (dc != null) {
			int status = dc.getInteger((Object) "status");
			long moneySend = dc.getLong((Object) "money_send");
			long moneyReceive = dc.getLong((Object) "money_receive");
			results = new TranferMoneyAgent(nickNameSend, nickNameReceive, moneySend, moneyReceive, status);
		}
		return results;
	}

	@Override
	public List<AgentResponse> listUserAgentAdmin(String nickName) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		String sql = "";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stmt;
			if (nickName != null && !nickName.equals("")) {
				sql = "SELECT nameagent,nickname,phone,address,id,parentid,`show`,active,percent_bonus_vincard,commission_rate FROM useragent WHERE status='D'  and  nickname=?";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, nickName);
			} else {
				sql = "SELECT nameagent,nickname,phone,address,id,parentid,`show`,active,percent_bonus_vincard,commission_rate FROM useragent WHERE status='D'";
				stmt = conn.prepareStatement(sql);
			}
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.id = rs.getInt("id");
				agent.parentid = rs.getInt("parentid");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.percent = rs.getInt("percent_bonus_vincard");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(0, agent);
			}
			rs.close();
			stmt.close();
		}
		return results;
	}

	@Override
	public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String nickName, String status, String timeStart,
			String timeEnd, String top_ds, int page) {
		final ArrayList<LogAgentTranferMoneyResponse> results = new ArrayList<LogAgentTranferMoneyResponse>();
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		FindIterable iterable = null;
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		int numStart = (page - 1) * 50;
		int numEnd = 50;
		objsort.put("_id", -1);
		if (nickName != null && !nickName.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickName);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickName);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (top_ds != null && !top_ds.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(top_ds)));
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", obj);
		}
		iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson) new Document(conditions)).sort((Bson) objsort)
				.skip(numStart).limit(50);
		iterable.forEach((Block) new Block<Document>() {

			public void apply(Document document) {
				LogAgentTranferMoneyResponse trans = new LogAgentTranferMoneyResponse();
				trans.nick_name_send = document.getString((Object) "nick_name_send");
				trans.nick_name_receive = document.getString((Object) "nick_name_receive");
				trans.status = document.getInteger((Object) "status");
				trans.trans_time = document.getString((Object) "trans_time");
				trans.fee = document.getLong((Object) "fee");
				trans.money_send = document.getLong((Object) "money_send");
				trans.money_receive = document.getLong((Object) "money_receive");
				trans.top_ds = document.getInteger((Object) "top_ds");
				trans.process = document.getInteger((Object) "process");
				trans.des_send = document.getString((Object) "des_send") != null
						&& !document.getString((Object) "des_send").equals("") ? document.getString((Object) "des_send")
								: "";
				trans.des_receive = document.getString((Object) "des_receive") != null
						&& !document.getString((Object) "des_receive").equals("")
								? document.getString((Object) "des_receive")
								: "";
				results.add(trans);
			}
		});
		return results;
	}

	@Override
	public LogAgentTranferMoneyResponse searchAgentTranferMoney(String tid) {
		final LogAgentTranferMoneyResponse result = new LogAgentTranferMoneyResponse();
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		FindIterable iterable = null;
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		conditions.put("transaction_no", tid);
		iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson) new Document(conditions)).sort((Bson) objsort)
				.skip(0).limit(1);
		iterable.forEach((Block) new Block<Document>() {

			public void apply(Document document) {
				result.nick_name_send = document.getString((Object) "nick_name_send");
				result.nick_name_receive = document.getString((Object) "nick_name_receive");
				result.status = document.getInteger((Object) "status");
				result.trans_time = document.getString((Object) "trans_time");
				result.fee = document.getLong((Object) "fee");
				result.money_send = document.getLong((Object) "money_send");
				result.money_receive = document.getLong((Object) "money_receive");
				result.top_ds = document.getInteger((Object) "top_ds");
				result.process = document.getInteger((Object) "process");
				result.des_send = document.getString((Object) "des_send") != null
						&& !document.getString((Object) "des_send").equals("") ? document.getString((Object) "des_send")
								: "";
				result.des_receive = document.getString((Object) "des_receive") != null
						&& !document.getString((Object) "des_receive").equals("")
								? document.getString((Object) "des_receive")
								: "";
			}
		});
		return result;
	}

	@Override
	public long countsearchAgentTranferMoney(String nickName, String status, String timeStart, String timeEnd,
			String top_ds) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickName != null && !nickName.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickName);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickName);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (top_ds != null && !top_ds.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(top_ds)));
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Serializable) obj);
		}
		long totalRecord = db.getCollection("log_chuyen_tien_dai_ly").count((Bson) new Document(conditions));
		return totalRecord;
	}

	@Override
	public long totalMoneyVinReceiveFromAgent(String nickName, String status, String timeStart, String timeEnd,
			String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickName != null && !nickName.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickName);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickName);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(topDS)));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Serializable) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_receive")
								.append("money", (Object) new Document("$sum", (Object) "$money_receive"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public long totalMoneyVinSendFromAgent(String nickName, String status, String timeStart, String timeEnd,
			String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickName != null && !nickName.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickName);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickName);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(topDS)));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Serializable) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_send").append("money",
								(Object) new Document("$sum", (Object) "$money_send"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public long totalMoneyVinFeeFromAgent(String nickName, String status, String timeStart, String timeEnd,
			String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
//        HashMap<String, Serializable> conditions = new HashMap<String, Serializable>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickName != null && !nickName.equals("")) {
			BasicDBObject query1 = new BasicDBObject("nick_name_send", (Object) nickName);
			BasicDBObject query2 = new BasicDBObject("nick_name_receive", (Object) nickName);
			ArrayList<BasicDBObject> myList = new ArrayList<BasicDBObject>();
			myList.add(query1);
			myList.add(query2);
			conditions.put("$or", myList);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.valueOf(Integer.parseInt(status)));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.valueOf(Integer.parseInt(topDS)));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Serializable) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "").append("money",
								(Object) new Document("$sum", (Object) "$fee"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public List<AgentResponse> listUserAgentLevel2ByParentID(int ParentID) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		String sql = "";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			sql = "SELECT nameagent,nickname,phone,address,id,parentid,commission_rate FROM useragent WHERE status='D' and  active=1 and parentid=?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setInt(1, ParentID);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.id = rs.getInt("id");
				agent.parentid = rs.getInt("parentid");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(0, agent);
			}
			rs.close();
			stmt.close();
		}
		return results;
	}

	@Override
	public List<AgentResponse> listUserAgentLevel2ByID(int ID) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		String sql = "";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			sql = "SELECT nameagent,nickname,phone,address,id,parentid,commission_rate FROM useragent WHERE status='D' and `show`=1 and id=?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setInt(1, ID);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.id = rs.getInt("id");
				agent.parentid = rs.getInt("parentid");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(0, agent);
			}
			rs.close();
			stmt.close();
		}
		return results;
	}

	@Override
	public List<AgentResponse> listUserAgentActive(String nickName) throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		String sql = "";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stmt;
			if (nickName != null && !nickName.equals("")) {
				sql = "SELECT nameagent,nickname,phone,address,id,parentid,`show`,active,commission_rate FROM useragent WHERE status='D' and `show` = 1 and active=1 and parentid=-1 and  nickname=?";
				stmt = conn.prepareStatement(sql);
				stmt.setString(1, nickName);
			} else {
				sql = "SELECT nameagent,nickname,phone,address,id,parentid,`show`,active,commission_rate FROM useragent WHERE status='D' and active=1 and parentid=-1 and `show` = 1";
				stmt = conn.prepareStatement(sql);
			}
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.id = rs.getInt("id");
				agent.parentid = rs.getInt("parentid");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(0, agent);
			}
			rs.close();
			stmt.close();
		}
		return results;
	}

	@Override
	public List<AgentResponse> listUserAgentLevel1Active() throws SQLException {
		ArrayList<AgentResponse> results = new ArrayList<AgentResponse>();
		String sql = "";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			sql = "SELECT nameagent,nickname,phone,address,id,parentid,`show`,active,commission_rate FROM useragent WHERE status='D' and active=1 and parentid=-1 and `show` = 1";
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				AgentResponse agent = new AgentResponse();
				agent.fullName = rs.getString("nameagent");
				agent.nickName = rs.getString("nickname");
				agent.mobile = rs.getString("phone");
				agent.address = rs.getString("address");
				agent.id = rs.getInt("id");
				agent.parentid = rs.getInt("parentid");
				agent.show = rs.getInt("show");
				agent.active = rs.getInt("active");
				agent.commissionRate = rs.getDouble("commission_rate");
				results.add(0, agent);
			}
			rs.close();
			stmt.close();
		}
		return results;
	}

	@Override
	public List<LogAgentTranferMoneyResponse> searchAgentTongTranferMoney(String nickNameSend, String nickNameRecieve,
			String status, String timeStart, String timeEnd, String top_ds, int page) {
		final ArrayList<LogAgentTranferMoneyResponse> results = new ArrayList<LogAgentTranferMoneyResponse>();
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		FindIterable iterable = null;
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		int numStart = (page - 1) * 50;
		int numEnd = 50;
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")) {
			conditions.put("nick_name_send", nickNameSend);
		}
		if (nickNameRecieve != null && !nickNameRecieve.equals("")) {
			conditions.put("nick_name_receive", nickNameRecieve);
		}
		if (top_ds != null && !top_ds.equals("")) {
			conditions.put("top_ds", Integer.parseInt(top_ds));
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.parseInt(status));
		}
		if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		iterable = db.getCollection("log_chuyen_tien_dai_ly").find((Bson) new Document(conditions)).sort((Bson) objsort)
				.skip(numStart).limit(50);
		iterable.forEach((Block) new Block<Document>() {

			public void apply(Document document) {
				LogAgentTranferMoneyResponse trans = new LogAgentTranferMoneyResponse();
				trans.nick_name_send = document.getString((Object) "nick_name_send");
				trans.nick_name_receive = document.getString((Object) "nick_name_receive");
				trans.status = document.getInteger((Object) "status");
				trans.trans_time = document.getString((Object) "trans_time");
				trans.fee = document.getLong((Object) "fee");
				trans.money_send = document.getLong((Object) "money_send");
				trans.money_receive = document.getLong((Object) "money_receive");
				trans.top_ds = document.getInteger((Object) "top_ds");
				trans.process = document.getInteger((Object) "process");
				trans.des_send = document.getString((Object) "des_send") != null
						&& !document.getString((Object) "des_send").equals("") ? document.getString((Object) "des_send")
								: "";
				trans.des_receive = document.getString((Object) "des_receive") != null
						&& !document.getString((Object) "des_receive").equals("")
								? document.getString((Object) "des_receive")
								: "";
				results.add(trans);
			}
		});
		return results;
	}

	@Override
	public long countsearchAgentTongTranferMoney(String nickNameSend, String nickNameRecieve, String status,
			String timeStart, String timeEnd, String top_ds) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")) {
			conditions.put("nick_name_send", nickNameSend);
		}
		if (nickNameRecieve != null && !nickNameRecieve.equals("")) {
			conditions.put("nick_name_receive", nickNameRecieve);
		}
		if (top_ds != null && !top_ds.equals("")) {
			conditions.put("top_ds", Integer.parseInt(top_ds));
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.parseInt(status));
		}
		if (timeStart != null && !timeStart.equals("") && timeEnd != null && !timeEnd.equals("")) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		long totalRecord = db.getCollection("log_chuyen_tien_dai_ly").count((Bson) new Document(conditions));
		return totalRecord;
	}

	@Override
	public long totalMoneyVinReceiveFromAgentTong(String nickNameSend, String nickNameRecieve, String status,
			String timeStart, String timeEnd, String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")) {
			conditions.put("nick_name_send", nickNameSend);
		}
		if (nickNameRecieve != null && !nickNameRecieve.equals("")) {
			conditions.put("nick_name_receive", nickNameRecieve);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.parseInt(status));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.parseInt(topDS));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_receive")
								.append("money", (Object) new Document("$sum", (Object) "$money_receive"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public long totalMoneyVinSendFromAgentTong(String nickNameSend, String nickNameRecieve, String status,
			String timeStart, String timeEnd, String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")) {
			conditions.put("nick_name_send", nickNameSend);
		}
		if (nickNameRecieve != null && !nickNameRecieve.equals("")) {
			conditions.put("nick_name_receive", nickNameRecieve);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.parseInt(status));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.parseInt(topDS));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "$nick_name_send").append("money",
								(Object) new Document("$sum", (Object) "$money_send"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public long totalMoneyVinFeeFromAgentTong(String nickNameSend, String nickNameRecieve, String status,
			String timeStart, String timeEnd, String topDS) {
		MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
		MongoCollection collection = db.getCollection("log_chuyen_tien_dai_ly");
		HashMap<String, Object> conditions = new HashMap<String, Object>();
		BasicDBObject obj = new BasicDBObject();
		BasicDBObject objsort = new BasicDBObject();
		objsort.put("_id", -1);
		if (nickNameSend != null && !nickNameSend.equals("")) {
			conditions.put("nick_name_send", nickNameSend);
		}
		if (nickNameRecieve != null && !nickNameRecieve.equals("")) {
			conditions.put("nick_name_receive", nickNameRecieve);
		}
		if (status != null && !status.equals("")) {
			conditions.put("status", Integer.parseInt(status));
		}
		if (topDS != null && !topDS.equals("")) {
			conditions.put("top_ds", Integer.parseInt(topDS));
		}
		if (timeStart != null && !timeStart.isEmpty() && timeEnd != null && !timeEnd.isEmpty()) {
			obj.put("$gte", (Object) timeStart);
			obj.put("$lte", (Object) timeEnd);
			conditions.put("trans_time", (Object) obj);
		}
		AggregateIterable<Document> iterable = collection
				.aggregate(Arrays.asList(new Document[] { new Document("$match", (Object) new Document(conditions)),
						new Document("$group", (Object) new Document("_id", (Object) "").append("money",
								(Object) new Document("$sum", (Object) "$fee"))) }));
		long totalMoney = 0L;
		for (Document row : iterable) {
			totalMoney += row.getLong((Object) "money").longValue();
		}
		return totalMoney;
	}

	@Override
	public int registerPercentBonusVincard(String nickName, int percent) throws SQLException {
		String sql = " UPDATE vinplay_admin.useragent  SET percent_bonus_vincard = ?  WHERE nickname = ? ";
		int recordNumber = 0;
		PreparedStatement stmt = null;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			stmt = conn.prepareStatement(
					" UPDATE vinplay_admin.useragent  SET percent_bonus_vincard = ?  WHERE nickname = ? ");
			stmt.setInt(1, percent);
			stmt.setString(2, nickName);
			recordNumber = stmt.executeUpdate();
			stmt.close();
		}
		return recordNumber;
	}

	@Override
	public List<AgentModel> getListPercentBonusVincard(String nickName) throws SQLException {
		List<AgentModel> result = new ArrayList<AgentModel>();
		if (nickName != null && !nickName.isEmpty()) {
			result = "all".equals(nickName) ? this.getAllPercentBonusVincard()
					: this.getPercentBonusVincardByNickName(nickName);
		}
		return result;
	}

	private List<AgentModel> getAllPercentBonusVincard() throws SQLException {
		ArrayList<AgentModel> result = new ArrayList<AgentModel>();
		String sql = " SELECT nickname, percent_bonus_vincard  FROM vinplay_admin.useragent  WHERE parentid = -1    AND active = 1    AND status = 'D'    AND `show` = 1  ORDER BY `order` ";
		Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(
					" SELECT nickname, percent_bonus_vincard  FROM vinplay_admin.useragent  WHERE parentid = -1    AND active = 1    AND status = 'D'    AND `show` = 1  ORDER BY `order` ");
			rs = stmt.executeQuery();
			while (rs.next()) {
				AgentModel agentModel = new AgentModel(rs.getString("nickname"), rs.getInt("percent_bonus_vincard"));
				result.add(agentModel);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
		return result;
	}

	private List<AgentModel> getPercentBonusVincardByNickName(String nickName) throws SQLException {
		ArrayList<AgentModel> result = new ArrayList<AgentModel>();
		String sql = " SELECT nickname, percent_bonus_vincard  FROM vinplay_admin.useragent  WHERE parentid = -1    AND nickname = ? ";
		Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(
					" SELECT nickname, percent_bonus_vincard  FROM vinplay_admin.useragent  WHERE parentid = -1    AND nickname = ? ");
			stmt.setString(1, nickName);
			rs = stmt.executeQuery();
			while (rs.next()) {
				AgentModel agentModel = new AgentModel(rs.getString("nickname"), rs.getInt("percent_bonus_vincard"));
				result.add(agentModel);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
		return result;
	}

	/*
	 * Get list all agency
	 */
	@Override
	public List<Map<String, Object>> getAgencies(String agencyCode, int page, int maxItem) throws SQLException {
		List<Map<String, Object>> data = new ArrayList<>();
		String conditionReferCode = ((agencyCode == null || agencyCode.trim().isEmpty()) ? "" : (" and (code = ?)"));
		String conditionPaginate = ((page == -1 || maxItem == -1) ? "" : ("  limit ?,?"));
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sqlCount = "select count(code) total from useragent where code <> '' and code is not null"
					+ conditionReferCode;
			String sql = "select username, nickname, nameagent, nameaccount, `code`, createtime from useragent where code <> '' and code is not null"
					+ conditionReferCode + " ORDER BY createtime desc , nickname asc, nameagent asc"
					+ conditionPaginate;
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (agencyCode != null && !agencyCode.trim().isEmpty()) {
				stmt.setString(index, agencyCode);
				stmtCount.setString(index, agencyCode);
				index++;
			}

			if (page != -1 && maxItem != -1) {
				page = page < 1 ? 0 : page - 1;
				stmt.setInt(index++, page * maxItem);
				stmt.setInt(index++, maxItem);
			}

			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("username", rs.getObject("username"));
				objectMap.put("nickname", rs.getObject("nickname"));
				objectMap.put("nameagent", rs.getObject("nameagent"));
				objectMap.put("nameaccount", rs.getObject("nameaccount"));
				objectMap.put("code", rs.getObject("code"));
				objectMap.put("createtime", rs.getObject("createtime"));
				data.add(objectMap);
			}

			while (rsCount.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getAgency: " + e.getMessage());
			return new ArrayList<>();
		}
	}
	
	/*
	 * Get list all agency level 1
	 */
	@Override
	public List<Map<String, Object>> getAgenciesLevel1(String agencyCode, int page, int maxItem) throws SQLException {
		List<Map<String, Object>> data = new ArrayList<>();
		String conditionReferCode = ((agencyCode == null || agencyCode.trim().isEmpty()) ? "" : (" and (code = ?)"));
		String conditionPaginate = ((page == -1 || maxItem == -1) ? "" : ("  limit ?,?"));
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sqlCount = "select count(code) total from useragent where code <> '' and code is not null"
					+ conditionReferCode;
			String sql = "select username, nickname, nameagent, nameaccount, `code`, createtime from useragent where code <> '' and code is not null and level = 1 and (parentid = -1 or parentid is null)"
					+ conditionReferCode + " ORDER BY createtime desc , nickname asc, nameagent asc"
					+ conditionPaginate;
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (agencyCode != null && !agencyCode.trim().isEmpty()) {
				stmt.setString(index, agencyCode);
				stmtCount.setString(index, agencyCode);
				index++;
			}

			if (page != -1 && maxItem != -1) {
				page = page < 1 ? 0 : page - 1;
				stmt.setInt(index++, page * maxItem);
				stmt.setInt(index++, maxItem);
			}

			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("username", rs.getObject("username"));
				objectMap.put("nickname", rs.getObject("nickname"));
				objectMap.put("nameagent", rs.getObject("nameagent"));
				objectMap.put("nameaccount", rs.getObject("nameaccount"));
				objectMap.put("code", rs.getObject("code"));
				objectMap.put("createtime", rs.getObject("createtime"));
				data.add(objectMap);
			}

			while (rsCount.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getAgency: " + e.getMessage());
			return new ArrayList<>();
		}
	}

	@Override
	public List<Map<String, Object>> getProfitComponents4Agency(String agencyCode, String currentMonth)
			throws SQLException, ParseException {
		List<Map<String, Object>> data = new ArrayList<>();
		String firstDayOfMonthStr = currentMonth + "-01 00:00:00";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateFormat.parse(firstDayOfMonthStr));
		int lastMonth = cal.get(cal.MONTH);
		cal.add(Calendar.DATE, 30);
		if (cal.get(cal.MONTH) > lastMonth) {
			cal.add(Calendar.DATE, -30);
			cal.add(Calendar.DATE, 29);
		}

		String lastDayOfMonthStr = dateFormat.format(cal.getTime()) + " 23:59:59";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT sum(IFNULL(wm,0) + IFNULL(ibc,0) + IFNULL(ag, 0) + IFNULL(tlmn,0) + IFNULL(bacay,0) + IFNULL(xocdia,0) + IFNULL(minipoker,0) + IFNULL(slot_pokemon,0) + IFNULL(baucua,0) + IFNULL(taixiu,0) + "
					+ " IFNULL(caothap,0) + IFNULL(slot_bitcoin,0) + IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(taixiu_st,0) + IFNULL(fish,0)) totalbet,"
					+ " sum(IFNULL(wm_win,0) + IFNULL(ibc_win,0) + IFNULL(ag_win,0) + IFNULL(tlmn_win,0) + IFNULL(bacay_win,0) + IFNULL(xocdia_win,0) + IFNULL(minipoker_win,0) + IFNULL(slot_pokemon_win,0) + "
					+ " IFNULL(baucua_win,0) + IFNULL(taixiu_win,0) + IFNULL(caothap_win,0) + IFNULL(slot_bitcoin_win,0) + IFNULL(slot_taydu_win,0) + IFNULL(slot_angrybird_win,0) + IFNULL(slot_thantai_win,0) +"
					+ " IFNULL(slot_chiemtinh_win,0) + IFNULL(taixiu_st_win,0) + IFNULL(fish_win,0)) totalwin, SUM(IFNULL(t_bonus,0)) totalbonus, SUM(IFNULL(t_sport_bonus,0)) totalsportbonus, SUM(IFNULL(t_casino_bonus,0)) totalcasinobonus,"
					+ " SUM(IFNULL(t_egame_bonus,0)) totalegamebonus, SUM(IFNULL(t_refund,0)) totalfund, SUM(IFNULL(ag,0) - IFNULL(ag_win,0)) totalag, SUM(IFNULL(cmd,0) - IFNULL(cmd_win,0)) totalcmd,"
					+ " SUM(IFNULL(wm,0) - IFNULL(wm_win,0)) totalwm, SUM(IFNULL(ibc,0) + IFNULL(cmd,0)) totalbetsport,"
					+ " (SUM(IFNULL(tlmn,0) + IFNULL(bacay,0) + IFNULL(xocdia,0) + IFNULL(minipoker,0) + IFNULL(slot_pokemon,0) + IFNULL(baucua,0) + IFNULL(taixiu,0) + IFNULL(caothap,0) + IFNULL(slot_bitcoin,0) +"
					+ " IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(taixiu_st,0) + IFNULL(fish,0)) - SUM(IFNULL(tlmn_win,0) + IFNULL(bacay_win,0) + IFNULL(xocdia_win,0) +"
					+ " IFNULL(minipoker_win,0) + IFNULL(slot_pokemon_win,0) + IFNULL(baucua_win,0) + IFNULL(taixiu_win,0) + IFNULL(caothap_win,0) + IFNULL(slot_bitcoin_win,0) + IFNULL(slot_taydu_win,0) + IFNULL(slot_angrybird_win,0) +"
					+ " IFNULL(slot_thantai_win,0) + IFNULL(slot_thethao_win,0) + IFNULL(slot_chiemtinh_win,0) + IFNULL(taixiu_st_win,0) + IFNULL(fish_win,0))) totalegame, SUM(IFNULL(ibc,0) - IFNULL(ibc_win,0)) totalibc,"
					+ " sum(IFNULL(deposit,0)) totaldeposit, sum(IFNULL(withdraw,0)) totalwithdraw"
					+ " FROM log_report_user"
					+ " where time_report >= ? AND time_report <= ?" + " AND (code = ? or"
					+ " (nick_name in (select nick_name FROM users where referral_code = ?)) or"
					+ " (nick_name in (SELECT nick_name FROM users where"
					+ " id in (select user_id from agency_code where referral_code = ?))))";
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			stmt.setString(index++, firstDayOfMonthStr);
			stmt.setString(index++, lastDayOfMonthStr);
			stmt.setString(index++, agencyCode);
			stmt.setString(index++, agencyCode);
			stmt.setString(index++, agencyCode);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("totalbet", rs.getObject("totalbet"));
				objectMap.put("totalwin", rs.getObject("totalwin"));
				objectMap.put("totalbonus", rs.getObject("totalbonus"));
				objectMap.put("totalbonus", rs.getObject("totalsportbonus"));
				objectMap.put("totalbonus", rs.getObject("totalcasinobonus"));
				objectMap.put("totalbonus", rs.getObject("totalegamebonus"));
				objectMap.put("totalfund", rs.getObject("totalfund"));
				objectMap.put("totalag", rs.getObject("totalag"));
				objectMap.put("totalcmd", rs.getObject("totalcmd"));
				objectMap.put("totalwm", rs.getObject("totalwm"));
				objectMap.put("totalegame", rs.getObject("totalegame"));
				objectMap.put("totalibc", rs.getObject("totalibc"));
				objectMap.put("totaldeposit", rs.getObject("totaldeposit"));
				objectMap.put("totalwithdraw", rs.getObject("totalwithdraw"));
				data.add(objectMap);
			}

			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getProfitComponents4Agency: " + e.getMessage());
			return null;
		}
	}
	
	public List<Map<String, Object>> getProfitComponentsNotAgency(String currentMonth)
			throws SQLException, ParseException {
		List<Map<String, Object>> data = new ArrayList<>();
		String firstDayOfMonthStr = currentMonth + "-01 00:00:00";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateFormat.parse(firstDayOfMonthStr));
		int lastMonth = cal.get(cal.MONTH);
		cal.add(Calendar.DATE, 30);
		if (cal.get(cal.MONTH) > lastMonth) {
			cal.add(Calendar.DATE, -30);
			cal.add(Calendar.DATE, 29);
		}

		String lastDayOfMonthStr = dateFormat.format(cal.getTime()) + " 23:59:59";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT sum(IFNULL(wm,0) + IFNULL(ibc,0) + IFNULL(ag, 0) + IFNULL(tlmn,0) + IFNULL(bacay,0) + IFNULL(xocdia,0) + IFNULL(minipoker,0) + IFNULL(slot_pokemon,0) + IFNULL(baucua,0) + IFNULL(taixiu,0) + "
					+ " IFNULL(caothap,0) + IFNULL(slot_bitcoin,0) + IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(taixiu_st,0) + IFNULL(fish,0)) totalbet,"
					+ " sum(IFNULL(wm_win,0) + IFNULL(ibc_win,0) + IFNULL(ag_win,0) + IFNULL(tlmn_win,0) + IFNULL(bacay_win,0) + IFNULL(xocdia_win,0) + IFNULL(minipoker_win,0) + IFNULL(slot_pokemon_win,0) + "
					+ " IFNULL(baucua_win,0) + IFNULL(taixiu_win,0) + IFNULL(caothap_win,0) + IFNULL(slot_bitcoin_win,0) + IFNULL(slot_taydu_win,0) + IFNULL(slot_angrybird_win,0) + IFNULL(slot_thantai_win,0) +"
					+ " IFNULL(slot_chiemtinh_win,0) + IFNULL(taixiu_st_win,0) + IFNULL(fish_win,0)) totalwin, SUM(IFNULL(t_bonus,0)) totalbonus, SUM(IFNULL(t_sport_bonus,0)) totalsportbonus, SUM(IFNULL(t_casino_bonus,0)) totalcasinobonus,"
					+ " SUM(IFNULL(t_egame_bonus,0)) totalegamebonus, SUM(IFNULL(t_refund,0)) totalfund, SUM(IFNULL(ag,0) - IFNULL(ag_win,0)) totalag, SUM(IFNULL(cmd,0) - IFNULL(cmd_win,0)) totalcmd,"
					+ " SUM(IFNULL(wm,0) - IFNULL(wm_win,0)) totalwm, SUM(IFNULL(ibc,0) + IFNULL(cmd,0)) totalbetsport,"
					+ " (SUM(IFNULL(tlmn,0) + IFNULL(bacay,0) + IFNULL(xocdia,0) + IFNULL(minipoker,0) + IFNULL(slot_pokemon,0) + IFNULL(baucua,0) + IFNULL(taixiu,0) + IFNULL(caothap,0) + IFNULL(slot_bitcoin,0) +"
					+ " IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) + IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(taixiu_st,0) + IFNULL(fish,0)) - SUM(IFNULL(tlmn_win,0) + IFNULL(bacay_win,0) + IFNULL(xocdia_win,0) +"
					+ " IFNULL(minipoker_win,0) + IFNULL(slot_pokemon_win,0) + IFNULL(baucua_win,0) + IFNULL(taixiu_win,0) + IFNULL(caothap_win,0) + IFNULL(slot_bitcoin_win,0) + IFNULL(slot_taydu_win,0) + IFNULL(slot_angrybird_win,0) +"
					+ " IFNULL(slot_thantai_win,0) + IFNULL(slot_thethao_win,0) + IFNULL(slot_chiemtinh_win,0) + IFNULL(taixiu_st_win,0) + IFNULL(fish_win,0))) totalegame, SUM(IFNULL(ibc,0) - IFNULL(ibc_win,0)) totalibc,"
					+ " sum(IFNULL(deposit,0)) totaldeposit, sum(IFNULL(withdraw,0)) totalwithdraw"
					+ " FROM log_report_user"
					+ " where time_report >= ? AND time_report <= ?" + " AND (code is null or code = '')";
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			stmt.setString(index++, firstDayOfMonthStr);
			stmt.setString(index++, lastDayOfMonthStr);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("totalbet", rs.getObject("totalbet"));
				objectMap.put("totalwin", rs.getObject("totalwin"));
				objectMap.put("totalbonus", rs.getObject("totalbonus"));
				objectMap.put("totalbonus", rs.getObject("totalsportbonus"));
				objectMap.put("totalbonus", rs.getObject("totalcasinobonus"));
				objectMap.put("totalbonus", rs.getObject("totalegamebonus"));
				objectMap.put("totalfund", rs.getObject("totalfund"));
				objectMap.put("totalag", rs.getObject("totalag"));
				objectMap.put("totalcmd", rs.getObject("totalcmd"));
				objectMap.put("totalwm", rs.getObject("totalwm"));
				objectMap.put("totalegame", rs.getObject("totalegame"));
				objectMap.put("totalibc", rs.getObject("totalibc"));
				objectMap.put("totaldeposit", rs.getObject("totaldeposit"));
				objectMap.put("totalwithdraw", rs.getObject("totalwithdraw"));
				data.add(objectMap);
			}

			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getProfitComponents4Agency: " + e.getMessage());
			return null;
		}
	}

	@Override
	public List<Map<String, Object>> getMemberPassed4Agency(String agencyCode, String currentMonth)
			throws SQLException, ParseException {
		List<Map<String, Object>> data = new ArrayList<>();
		String firstDayOfMonthStr = currentMonth + "-01 00:00:00";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateFormat.parse(firstDayOfMonthStr));
		int lastMonth = cal.get(cal.MONTH);
		cal.add(Calendar.DATE, 30);
		if (cal.get(cal.MONTH) > lastMonth) {
			cal.add(Calendar.DATE, -30);
			cal.add(Calendar.DATE, 29);
		}

		String lastDayOfMonthStr = dateFormat.format(cal.getTime()) + " 23:59:59";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT DISTINCT(nick_name) nickname, "
					+ " (SELECT create_time FROM v_log_user_play where nick_name = nickname limit 1) createtime, "
					+ " fn_GetSumDepositByNickname((SELECT nickname), (SELECT createtime), 15) rq_deposit, (SELECT referral_code FROM v_log_user_play where nick_name = nickname limit 1) referral_code, "
					+ " (SELECT count(time_report) FROM v_log_user_play where nick_name = nickname AND time_report >= ? AND time_report <= ?) rq_active,"
					+ " (SELECT sum(IFNULL(wm,0) + IFNULL(ibc,0) + IFNULL(ag,0) + IFNULL(tlmn,0) + IFNULL(bacay,0) + IFNULL(xocdia,0) + IFNULL(minipoker,0) + IFNULL(slot_pokemon,0) +"
					+ " IFNULL(baucua,0) + IFNULL(taixiu,0) + IFNULL(caothap,0) + IFNULL(slot_bitcoin,0) + IFNULL(slot_taydu,0) + IFNULL(slot_angrybird,0) + IFNULL(slot_thantai,0) +"
					+ " IFNULL(slot_thethao,0) + IFNULL(slot_chiemtinh,0) + IFNULL(taixiu_st,0) + IFNULL(fish,0))"
					+ " FROM v_log_user_play where nick_name = (SELECT nickname) AND time_report >= (SELECT createtime) AND time_report <= DATE_ADD((SELECT createtime), INTERVAL 30 DAY)) rq_totalbet"
					+ " FROM v_log_user_play HAVING rq_active > 4 AND rq_deposit > 0 AND rq_totalbet > 0"
					+ " AND (FIND_IN_SET(referral_code,?) > 0 or"
					+ " (nick_name in (select nick_name FROM users where FIND_IN_SET(referral_code,?) > 0)) or"
					+ " (nick_name in (SELECT nick_name FROM users where"
					+ " id in (select user_id from agency_code where FIND_IN_SET(referral_code,?) > 0))))";
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			stmt.setString(index++, firstDayOfMonthStr);
			stmt.setString(index++, lastDayOfMonthStr);
			stmt.setString(index++, agencyCode);
			stmt.setString(index++, agencyCode);
			stmt.setString(index++, agencyCode);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("nickName", rs.getObject("nickName"));
				objectMap.put("createTime", rs.getObject("createTime"));
				objectMap.put("rqDeposit", rs.getObject("rq_deposit"));
				objectMap.put("code", rs.getObject("referral_code"));
				objectMap.put("rqActive", rs.getObject("rq_active"));
				objectMap.put("rqBetTotal", rs.getObject("rq_totalbet"));
				data.add(objectMap);
			}

			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	@Override
	public int getCountMemberNotAgency(String currentMonth) throws SQLException, ParseException {
		int count = 0;
		String firstDayOfMonthStr = currentMonth + "-01 00:00:00";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateFormat.parse(firstDayOfMonthStr));
		int lastMonth = cal.get(cal.MONTH);
		cal.add(Calendar.DATE, 30);
		if (cal.get(cal.MONTH) > lastMonth) {
			cal.add(Calendar.DATE, -30);
			cal.add(Calendar.DATE, 29);
		}

		String lastDayOfMonthStr = dateFormat.format(cal.getTime()) + " 23:59:59";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String sql = "SELECT ifnull(count(DISTINCT(nick_name)),0) total FROM vinplay.log_report_user where (`code` = ''  or `code` is null) and time_report >= ? AND time_report <= ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			stmt.setString(index++, firstDayOfMonthStr);
			stmt.setString(index++, lastDayOfMonthStr);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				count = rs.getInt("total");
			}
			
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}

			return count;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public List<UserDetailAgentModel> getUserDetailAgent(List<String> fields, String nick_name, String fromTime,
			String endTime, String referCode) throws SQLException {
		List<UserDetailAgentModel> users = new ArrayList<>();
		AddFieldDefault(fields);
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String selectField = "";
			for (String item : fields) {
				selectField += selectField.trim().isEmpty() ? item : "," + item;
			}

			String conditionReferCode = ((referCode == null || referCode.trim().isEmpty()) ? ""
					: (" AND (code = ? or" + " (nick_name in (select nick_name FROM users where referral_code = ?)) or"
							+ " (nick_name in (SELECT nick_name FROM users where"
							+ " id in (select user_id from agency_code where referral_code = ?))))"));

			String sql = "SELECT " + selectField + " FROM vinplay.log_report_user WHERE 1=1"
					+ ((nick_name == null || nick_name.trim().isEmpty()) ? "" : (" and log_report_user.nick_name = ?"))
					+ ((fromTime == null || fromTime.trim().isEmpty()) ? "" : (" and log_report_user.time_report >= ?"))
					+ ((endTime == null || endTime.trim().isEmpty()) ? "" : (" and log_report_user.time_report <= ?"))
					+ conditionReferCode;
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				index++;
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime + " 00:00:00");
				index++;
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime + " 23:59:59");
				index++;
			}

			if (!(referCode == null || referCode.trim().isEmpty())) {
				stmt.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				index++;
			}
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				UserDetailAgentModel user = new UserDetailAgentModel();
				user.setNick_name(rs.getString("nick_name"));
				user.setTime(rs.getDate("time_report"));
				user.setWm(rs.getLong("wm"));
				user.setWm_win(rs.getLong("wm_win"));
				user.setIbc(rs.getLong("ibc"));
				user.setIbc_win(rs.getLong("ibc_win"));
				user.setAg(rs.getLong("ag"));
				user.setAg_win(rs.getLong("ag_win"));
				user.setTlmn(rs.getLong("tlmn"));
				user.setTlmn_win(rs.getLong("tlmn_win"));
				user.setBacay(rs.getLong("bacay"));
				user.setBacay_win(rs.getLong("bacay_win"));
				user.setXocdia(rs.getLong("xocdia"));
				user.setXocdia_win(rs.getLong("xocdia_win"));
				user.setMinipoker(rs.getLong("minipoker"));
				user.setMinipoker_win(rs.getLong("minipoker_win"));
				user.setSlot_pokemon(rs.getLong("slot_pokemon"));
				user.setSlot_pokemon_win(rs.getLong("slot_pokemon_win"));
				user.setBaucua(rs.getLong("baucua"));
				user.setBaucua_win(rs.getLong("baucua_win"));
				user.setTaixiu(rs.getLong("taixiu"));
				user.setTaixiu_win(rs.getLong("taixiu_win"));
				user.setCaothap(rs.getLong("caothap"));
				user.setCaothap_win(rs.getLong("caothap_win"));
				user.setSlot_bitcoin(rs.getLong("slot_bitcoin"));
				user.setSlot_bitcoin_win(rs.getLong("slot_bitcoin_win"));
				user.setSlot_taydu(rs.getLong("slot_taydu"));
				user.setSlot_taydu_win(rs.getLong("slot_taydu_win"));
				user.setSlot_angrybird(rs.getLong("slot_angrybird"));
				user.setSlot_angrybird_win(rs.getLong("slot_angrybird_win"));
				user.setSlot_thantai(rs.getLong("slot_thantai"));
				user.setSlot_thantai_win(rs.getLong("slot_thantai_win"));
				user.setSlot_thethao(rs.getLong("slot_thethao"));
				user.setSlot_thethao_win(rs.getLong("slot_thethao_win"));
				user.setSlot_chiemtinh(rs.getLong("slot_chiemtinh"));
				user.setSlot_chiemtinh_win(rs.getLong("slot_chiemtinh_win"));
				user.setTaixiu_st(rs.getLong("taixiu_st"));
				user.setTaixiu_st_win(rs.getLong("taixiu_st_win"));
				user.setFish(rs.getLong("fish"));
				user.setFish_win(rs.getLong("fish_win"));
				user.setDeposit(rs.getLong("deposit"));
				user.setWithdraw(rs.getLong("withdraw"));
				user.setSlot_bikini(rs.getLong("slot_bikini"));
				user.setSlot_bikini_win(rs.getLong("slot_bikini_win"));
				user.setSlot_galaxy(rs.getLong("slot_galaxy"));
				user.setSlot_galaxy_win(rs.getLong("slot_galaxy_win"));
				users.add(user);
			}
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			return users;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(
					"getUserDetailAgent(List<String> fields, String nick_name, String fromTime, String endTime, String referCode): "
							+ e.getMessage());
			return null;
		}
	}

	@Override
	public List<Map<String, Object>> getUserDetailAgent(List<String> fields, String nick_name, String currentTime,
			String referCode, int page, int maxItem) throws SQLException {
		page = page < 1 ? 0 : page - 1;
		List<Map<String, Object>> data = new ArrayList<>();
		List<String> newFields = new ArrayList<>();
		newFields = AddFieldDefault(fields);
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String selectField = "";
			for (String item : newFields) {
				selectField += selectField.trim().isEmpty() ? item : "," + item;
			}

			String fromTime = currentTime == null || currentTime.trim().isEmpty()
					? new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(Calendar.getInstance().getTime())
					: currentTime + " 00:00:00";
			String endTime = currentTime == null || currentTime.trim().isEmpty()
					? new SimpleDateFormat("yyyy-MM-dd 23:59:59").format(Calendar.getInstance().getTime())
					: currentTime + " 23:59:59";
			String conditionNickname = ((nick_name == null || nick_name.trim().isEmpty()) ? ""
					: (" and log_report_user.nick_name = ?"));
			String conditionFromTimeReport = ((fromTime == null || fromTime.trim().isEmpty()) ? ""
					: (" and log_report_user.time_report >= ?"));
			String conditionEndTimeReport = ((endTime == null || endTime.trim().isEmpty()) ? ""
					: (" and log_report_user.time_report <= ?"));
			String conditionReferCode = ((referCode == null || referCode.trim().isEmpty()) ? ""
					: (" AND (code = ? or" + " (nick_name in (select nick_name FROM users where referral_code = ?)) or"
							+ " (nick_name in (SELECT nick_name FROM users where"
							+ " id in (select user_id from agency_code where referral_code = ?))))"));
			String sqlCount = "SELECT count(id) as total FROM vinplay.log_report_user WHERE 1=1" + conditionNickname
					+ conditionFromTimeReport + conditionEndTimeReport + conditionReferCode;
			String sql = "SELECT " + selectField + " FROM vinplay.log_report_user WHERE 1=1" + conditionNickname
					+ conditionFromTimeReport + conditionEndTimeReport + conditionReferCode
					+ " order by time_report, nick_name limit ?,?";
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				stmtCount.setString(index, nick_name);
				index++;
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime + " 00:00:00");
				stmtCount.setString(index, fromTime + " 00:00:00");
				index++;
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime + " 23:59:59");
				stmtCount.setString(index, endTime + " 23:59:59");
				index++;
			}
			if (!(referCode == null || referCode.trim().isEmpty())) {
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
			}

			stmt.setInt(index++, page * maxItem);
			stmt.setInt(index++, maxItem);
			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				for (String item : newFields) {
					String[] arr = item.split(" ");
					if (arr.length > 1)
						objectMap.put(arr[1], rs.getObject(arr[1]));
					else
						objectMap.put(item, rs.getObject(item));
				}

				data.add(objectMap);
			}

			while (rsCount.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(
					"getUserDetailAgent(List<String> fields, String nick_name, String currentTime, String referCode, int page, int maxItem): "
							+ e.getMessage());
			return null;
		}
	}
	
	@Override
	public List<Map<String, Object>> getUserDetailAgent(List<String> fields, String nick_name, String fromTime,
			String endTime, String referCode, int page, int maxItem) throws SQLException {
		page = page < 1 ? 0 : page - 1;
		List<Map<String, Object>> data = new ArrayList<>();
		List<String> newFields = new ArrayList<>();
		newFields = AddFieldDefault(fields);
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String selectField = "";
			for (String item : newFields) {
				selectField += selectField.trim().isEmpty() ? item : "," + item;
			}

			String conditionNickname = ((nick_name == null || nick_name.trim().isEmpty()) ? ""
					: (" and log_report_user.nick_name = ?"));
			String conditionFromTimeReport = ((fromTime == null || fromTime.trim().isEmpty()) ? ""
					: (" and log_report_user.time_report >= ?"));
			String conditionEndTimeReport = ((endTime == null || endTime.trim().isEmpty()) ? ""
					: (" and log_report_user.time_report <= ?"));
			String conditionReferCode = ((referCode == null || referCode.trim().isEmpty()) ? ""
					: (" AND (code = ? or" + " (nick_name in (select nick_name FROM users where referral_code = ?)) or"
							+ " (nick_name in (SELECT nick_name FROM users where"
							+ " id in (select user_id from agency_code where referral_code = ?))))"));
			String sqlCount = "SELECT count(id) as total FROM vinplay.log_report_user WHERE 1=1" + conditionNickname
					+ conditionFromTimeReport + conditionEndTimeReport + conditionReferCode;

			String sql = "SELECT " + selectField + " FROM vinplay.log_report_user WHERE 1=1" + conditionNickname
					+ conditionFromTimeReport + conditionEndTimeReport + conditionReferCode
					+ " order by time_report, nick_name limit ?,?";
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				stmtCount.setString(index, nick_name);
				index++;
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime + " 00:00:00");
				stmtCount.setString(index, fromTime + " 00:00:00");
				index++;
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime + " 23:59:59");
				stmtCount.setString(index, endTime + " 23:59:59");
				index++;
			}
			if (!(referCode == null || referCode.trim().isEmpty())) {
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
			}

			stmt.setInt(index++, page * maxItem);
			stmt.setInt(index++, maxItem);
			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				for (String item : newFields) {
					String[] arr = item.split(" ");
					if (arr.length > 1)
						objectMap.put(arr[1], rs.getObject(arr[1]));
					else
						objectMap.put(item, rs.getObject(item));
				}

				data.add(objectMap);
			}

			while (rsCount.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(
					"getUserDetailAgent(List<String> fields, String nick_name, String fromTime, String endTime, String referCode, int page, int maxItem): "
							+ e.getMessage());
			return null;
		}
	}
	
	@Override
	public List<Map<String, Object>> ExecuteByFields(List<String> fields, String nick_name, String fromTime,
			String endTime, String referCode, int page, int maxItem) throws SQLException {
		page = page < 1 ? 0 : page - 1;
		List<Map<String, Object>> data = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String selectField = "";
			for (String item : fields) {
				selectField += selectField.trim().isEmpty() ? item : "," + item;
			}

			String conditionNickname = ((nick_name == null || nick_name.trim().isEmpty()) ? ""
					: (" and log_report_user.nick_name = ?"));
			String conditionFromTimeReport = ((fromTime == null || fromTime.trim().isEmpty()) ? ""
					: (" and log_report_user.time_report >= ?"));
			String conditionEndTimeReport = ((endTime == null || endTime.trim().isEmpty()) ? ""
					: (" and log_report_user.time_report <= ?"));
			String conditionReferCode = ((referCode == null || referCode.trim().isEmpty()) ? ""
					: (" AND (code = ? or" + " (nick_name in (select nick_name FROM users where referral_code = ?)) or"
							+ " (nick_name in (SELECT nick_name FROM users where"
							+ " id in (select user_id from agency_code where referral_code = ?))))"));
			String sqlCount = "SELECT count(id) as total FROM vinplay.log_report_user WHERE 1=1" + conditionNickname
					+ conditionFromTimeReport + conditionEndTimeReport + conditionReferCode;

			String sql = "SELECT " + selectField + " FROM vinplay.log_report_user WHERE 1=1" + conditionNickname
					+ conditionFromTimeReport + conditionEndTimeReport + conditionReferCode
					+ " order by time_report, nick_name limit ?,?";
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				stmtCount.setString(index, nick_name);
				index++;
			}
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime + " 00:00:00");
				stmtCount.setString(index, fromTime + " 00:00:00");
				index++;
			}
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime + " 23:59:59");
				stmtCount.setString(index, endTime + " 23:59:59");
				index++;
			}
			if (!(referCode == null || referCode.trim().isEmpty())) {
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
				stmt.setString(index, referCode);
				stmtCount.setString(index, referCode);
				index++;
			}

			stmt.setInt(index++, page * maxItem);
			stmt.setInt(index++, maxItem);
			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				for (String item : fields) {
					String[] arr = item.split(" ");
					if (arr.length > 1)
						objectMap.put(arr[1], rs.getObject(arr[1]));
					else
						objectMap.put(item, rs.getObject(item));
				}

				data.add(objectMap);
			}

			while (rsCount.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(
					"getUserDetailAgent(List<String> fields, String nick_name, String fromTime, String endTime, String referCode, int page, int maxItem): "
							+ e.getMessage());
			return null;
		}
	}

	@Override
	public List<Map<String, Object>> getUsers4Agent(String agencyCode, int page, int maxItem) throws SQLException {
		page = page < 1 ? 0 : page - 1;
		List<Map<String, Object>> data = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String conditionCode = (agencyCode == null || agencyCode.trim().isEmpty()) ? ""
					: (" and (referral_code = ?)");
			String paginate = (page == -1 || maxItem == -1) ? "" : (" limit ?,?");
			String sqlCount = "select count(nick_name) total from users where is_bot = 0 and dai_ly = 0 and nick_name is not null and nick_name <> ''"
					+ conditionCode;
			String sql = "select nick_name nickname, IFNULL(mobile,'') mobile, IFNULL(email,'') email, create_time, referral_code,"
					+ " IFNULL((select IFNULL(deposit,0) from log_count_user_play where nick_name = (select nickname) order by deposit desc limit 1), 0) deposit,"
					+ " IFNULL((select time_report from log_count_user_play where nick_name = (select nickname) order by deposit desc limit 1),'1900-01-01') time_report,"
					+ " CASE" + " WHEN (select deposit) > 0 THEN (select time_report)" + " ELSE '1900-01-01'"
					+ " END AS lasttimedeposit" + " from users"
					+ " where is_bot = 0 and dai_ly = 0 and nick_name is not null and nick_name <> ''" + conditionCode
					+ " ORDER BY lasttimedeposit desc, create_time desc" + paginate;
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (!(agencyCode == null || agencyCode.trim().isEmpty())) {
				stmt.setString(index, agencyCode);
				stmtCount.setString(index, agencyCode);
				index++;
			}

			if (-1 != page && -1 != maxItem) {
				stmt.setInt(index++, page * maxItem);
				stmt.setInt(index++, maxItem);
			}

			ResultSet rsCount = stmtCount.executeQuery();
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("nickname", rs.getObject("nickname"));
				objectMap.put("mobile", rs.getObject("mobile"));
				objectMap.put("email", rs.getObject("email"));
				objectMap.put("createTime", rs.getObject("create_time"));
				objectMap.put("agencyCode", rs.getObject("referral_code"));
				objectMap.put("lastTimeDeposit", rs.getObject("lasttimedeposit"));
				data.add(objectMap);
			}

			while (rsCount.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				objectMapCount.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getObject("total"));
				data.add(objectMapCount);
			}

			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getUsers4Agent: " + e.getMessage());
			return null;
		}
	}

	@Override
	public Boolean checkExistAgencyByCode(String agencyCode) throws SQLException {
		Boolean result = false;
		String sql = "select count(id) total from useragent where code = ?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				if (rs.getObject("total") == null)
					result = false;

				if (Integer.parseInt(rs.getObject("total").toString()) == 1)
					result = true;
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("checkExistAgencyByCode: " + e.getMessage());
			return false;
		}
	}
	
	@Override
	public Boolean checkExistAgency(UserAgentModel userAgentModel) throws SQLException {
		Boolean result = false;
		String sql = "select count(id) total from useragent where username=? or nickname=? or nameagent=?";
		if(!StringUtils.isBlank(userAgentModel.getPhone()))
			sql = sql + " or phone=?";
		
		if(!StringUtils.isBlank(userAgentModel.getEmail()))
			sql = sql + " or email=?";
		
		if(!StringUtils.isBlank(userAgentModel.getFacebook()))
			sql = sql + " or facebook=?";
		
		if(!StringUtils.isBlank(userAgentModel.getNumberaccount()))
			sql = sql + " or numberaccount=?";
		
		if(userAgentModel.getId() != 0)
			sql = sql + " and id <> ?";
		
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, userAgentModel.getUsername());
			stm.setString(index++, userAgentModel.getNickname());
			stm.setString(index++, userAgentModel.getNameagent());
			if(!StringUtils.isBlank(userAgentModel.getPhone()))
				stm.setString(index++, userAgentModel.getPhone());
			
			if(!StringUtils.isBlank(userAgentModel.getEmail()))
				stm.setString(index++, userAgentModel.getEmail());
			
			if(!StringUtils.isBlank(userAgentModel.getFacebook()))
				stm.setString(index++, userAgentModel.getFacebook());
			
			if(!StringUtils.isBlank(userAgentModel.getNumberaccount()))
				stm.setString(index++, userAgentModel.getNumberaccount());
			
			if(userAgentModel.getId() != 0)
				stm.setInt(index++, userAgentModel.getId());
			
			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				Map<String, Object> objectMapCount = new HashMap<>();
				if (rs.getObject("total") == null)
					result = false;

				if (Integer.parseInt(rs.getObject("total").toString()) == 1)
					result = true;
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("checkExistAgencyByCode: " + e.getMessage());
			return false;
		}
	}

	@Override
	public String updateAgencyCode4Userplay(String nickname, String agencyCode) throws SQLException {
		if (!checkExistAgencyByCode(agencyCode))
			return "Agency not exist";

		String sql = "Update users set referral_code = ? where nick_name = ? and is_bot = 0";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			stm.setString(index++, nickname);
			stm.executeUpdate();
			stm.close();
			if (conn != null) {
				conn.close();
			}
			
			HazelcastInstance instance = HazelcastClientFactory.getInstance();
			IMap userMap = instance.getMap("users");
			if (userMap.containsKey(nickname)) {
				userMap.lock(nickname);
				UserCacheModel userCache = (UserCacheModel) userMap.get(nickname);
				userCache.setReferralCode(agencyCode);
				userMap.put(nickname, userCache);
				try {
					userMap.unlock(nickname);
				}catch (Exception e) { }
			}
		} catch (SQLException e) {
			logger.error("updateAgencyCode4Userplay: " + e.getMessage());
			return "Update code of agency for user play not success";
		}

		return "success";
	}

	@Override
	public int getTotalUserDeposit4Agency(String agencyCode, String fromTime, String endTime) throws SQLException {
		int result = 0;
		String conditionTime = "";
		if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
			conditionTime = " and (time_report >= ? and time_report <= ?)";

		String sql = "select IFNULL(count(DISTINCT(nick_name)),0) total from log_report_user where deposit > 0 and code = ?"
				+ conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty())) {
				stm.setString(index++, fromTime + " 00:00:00");
				stm.setString(index++, endTime + " 23:59:59");
			}

			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Integer.parseInt(rs.getObject("total").toString());
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getTotalUserDeposit4Agency: " + e.getMessage());
			return 0;
		}
	}

	@Override
	public int getTotalUserWithdraw4Agency(String agencyCode, String fromTime, String endTime) throws SQLException {
		int result = 0;
		String conditionTime = "";
		if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
			conditionTime = " and (time_report >= ? and time_report <= ?)";

		String sql = "select IFNULL(count(DISTINCT(nick_name)),0) total from log_report_user where withdraw > 0 and code = ?"
				+ conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty())) {
				stm.setString(index++, fromTime + " 00:00:00");
				stm.setString(index++, endTime + " 23:59:59");
			}

			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Integer.parseInt(rs.getObject("total").toString());
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getTotalUserWithdraw4Agency: " + e.getMessage());
			return 0;
		}
	}

	public int getSumDeposit4Agency(String agencyCode, String fromTime, String endTime) throws SQLException {
		int result = 0;
		String conditionTime = "";
		if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
			conditionTime = " AND (dt.created_at >= ? AND dt.created_at <= ?)";

		String sql = "SELECT IFNULL(SUM(dt.amount),0) total FROM deposit_transactions dt"
				+ " JOIN users u ON u.id = dt.user_id"
				+ " WHERE u.referral_code = ? AND dt.status = 'APPROVED'" + conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty())) {
				stm.setString(index++, fromTime + " 00:00:00");
				stm.setString(index++, endTime + " 23:59:59");
			}

			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Integer.parseInt(rs.getObject("total").toString());
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getSumDeposit4Agency: " + e.getMessage());
			return 0;
		}
	}

	@Override
	public int getSumWithdraw4Agency(String agencyCode, String fromTime, String endTime) throws SQLException {
		int result = 0;
		String conditionTime = "";
		if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
			conditionTime = " AND (bw.created_at >= ? AND bw.created_at <= ?)";

		String sql = "SELECT IFNULL(SUM(bw.amount_krw),0) total FROM bank_withdrawals bw"
				+ " JOIN users u ON u.id = bw.user_id"
				+ " WHERE u.referral_code = ? AND bw.status = 'APPROVED'" + conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty())) {
				stm.setString(index++, fromTime + " 00:00:00");
				stm.setString(index++, endTime + " 23:59:59");
			}

			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Integer.parseInt(rs.getObject("total").toString());
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getSumWithdraw4Agency: " + e.getMessage());
			return 0;
		}
	}
	
	@Override
	public List<Long> getSumWDandDO4Agency(String agencyCode, String fromTime, String endTime) throws SQLException {
		List<Long> result = new ArrayList<Long>();
		String conditionTime = "";
		if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
			conditionTime = " and (time_report >= ? and time_report <= ?)";
		
		String conditionReferCode = "";
		if (agencyCode != null && !agencyCode.trim().isEmpty())
			conditionReferCode = " and (code= ?)";

		String sql = "select IFNULL(sum(withdraw),0) totalWithdraw, IFNULL(sum(deposit),0) totalDeposit from log_report_user where (1=1)" + conditionReferCode + conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			if (agencyCode != null && !agencyCode.trim().isEmpty()) {
				stm.setString(index++, agencyCode);
			}
			
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty())) {
				stm.setString(index++, fromTime + " 00:00:00");
				stm.setString(index++, endTime + " 23:59:59");
			}

			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result.add(Long.parseLong(rs.getObject("totalWithdraw").toString()));
				result.add(Long.parseLong(rs.getObject("totalDeposit").toString()));
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getSumWDandDO4Agency: " + e.getMessage());
			return Arrays.asList(0l, 0l);
		}
	}
	
	@Override
	public List<Long> getSumWDandDO4Agency(String agencyCode, String currentMonth) throws SQLException {
		List<Long> result = new ArrayList<Long>();
		String firstDayOfMonthStr = currentMonth + "-01 00:00:00";
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		try{
			cal.setTime(dateFormat.parse(firstDayOfMonthStr));
		}catch (Exception e) {
			logger.error("getSumWDandDO4Agency by current month: " + e.getMessage());
			return Arrays.asList(0l, 0l);
		}
		
		int lastMonth = cal.get(cal.MONTH);
		cal.add(Calendar.DATE, 30);
		if (cal.get(cal.MONTH) > lastMonth) {
			cal.add(Calendar.DATE, -30);
			cal.add(Calendar.DATE, 29);
		}

		String lastDayOfMonthStr = dateFormat.format(cal.getTime()) + " 23:59:59";
		
		String conditionTime = "";
		conditionTime = " and (time_report >= ? and time_report <= ?)";
		String conditionReferCode = "";
		if (agencyCode != null && !agencyCode.trim().isEmpty())
			conditionReferCode = " and (code= ?)";

		String sql = "select IFNULL(sum(withdraw),0) totalWithdraw, IFNULL(sum(deposit),0) totalDeposit from log_report_user where (1=1)" + conditionReferCode + conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			if (agencyCode != null && !agencyCode.trim().isEmpty()) {
				stm.setString(index++, agencyCode);
			}
			
			stm.setString(index++, firstDayOfMonthStr);
			stm.setString(index++, lastDayOfMonthStr);
			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result.add(Long.parseLong(rs.getObject("totalWithdraw").toString()));
				result.add(Long.parseLong(rs.getObject("totalDeposit").toString()));
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getSumWDandDO4Agency by current month: " + e.getMessage());
			return Arrays.asList(0l, 0l);
		}
	}
	
	@Override
	public int getTotalUserBet4Agency(String agencyCode, String fromTime, String endTime) throws SQLException {
		int result = 0;
		String conditionTime = "";
		if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
			conditionTime = " and (time_report >= ? and time_report <= ?)";

		String sql = "select IFNULL(count(DISTINCT(nick_name)),0) total from log_report_user where code = ?"
				+ " and (wm > 0 or ibc > 0 or ag > 0 or tlmn > 0 or bacay > 0 or xocdia > 0"
				+ " or minipoker > 0 or slot_pokemon > 0 or baucua > 0 or taixiu > 0"
				+ " or caothap > 0 or slot_bitcoin > 0 or slot_taydu > 0 or slot_angrybird > 0"
				+ " or slot_thantai > 0 or slot_thethao > 0 or cmd > 0 or slot_chiemtinh > 0" + " or taixiu_st > 0)"
				+ conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty())) {
				stm.setString(index++, fromTime + " 00:00:00");
				stm.setString(index++, endTime + " 23:59:59");
			}

			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Integer.parseInt(rs.getObject("total").toString());
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getTotalUserBet4Agency: " + e.getMessage());
			return 0;
		}
	}

	@Override
	public int getTotalUserRegister4Agency(String agencyCode, String fromTime, String endTime) throws SQLException {
		int result = 0;
		String conditionTime = "";
		if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
			conditionTime = " and (create_time >= ? and create_time <= ?)";

		String sql = "select IFNULL(count(nick_name),0) total from users where"
				+ " is_bot = 0 and dai_ly = 0 and referral_code = ?" + conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty())) {
				stm.setString(index++, fromTime + " 00:00:00");
				stm.setString(index++, endTime + " 23:59:59");
			}

			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Integer.parseInt(rs.getObject("total").toString());
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getTotalUserRegister4Agency: " + e.getMessage());
			return 0;
		}
	}

	@Override
	public int getTotalUser4Agency(String agencyCode) throws SQLException {
		int result = 0;
		String conditionTime = "";
		String sql = "select IFNULL(count(nick_name),0) total from users where"
				+ " is_bot = 0 and dai_ly = 0 and referral_code = ?" + conditionTime;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, agencyCode);
			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Integer.parseInt(rs.getObject("total").toString());
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getTotalUser4Agency: " + e.getMessage());
			return 0;
		}
	}
	
	@Override
	public Map<String, Object> getSumPlayInGame(String nick_name, String fromTime,
			String endTime, int page, int maxItem) throws SQLException {
		page = page < 1 ? 0 : page - 1;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			String conditions = "";
			if ((fromTime != null && !fromTime.trim().isEmpty()) && (endTime != null && !endTime.trim().isEmpty()))
				conditions = " and (time_report >= ? and time_report <= ?)";
			
			if (!(nick_name == null || nick_name.trim().isEmpty()))
				conditions += " and (nick_name=?)";
			
			String paging = "";
			if (page != -1 && maxItem != -1)
				paging = " limit ?,?";

			String sql = "select nick_name, sum(ifnull(wm,0)) wm,sum(ifnull(ibc,0)) ibc,sum(ifnull(ag,0)) ag, sum(ifnull(cmd,0)) cmd,"
					+ "sum(ifnull(tlmn, 0)) tlmn,sum(ifnull(bacay,0)) bacay,sum(ifnull(xocdia,0)) xocdia,sum(ifnull(minipoker,0)) minipoker,"
					+ "sum(ifnull(slot_pokemon,0)) slot_pokemon,sum(ifnull(baucua,0)) baucua,sum(ifnull(taixiu,0)) taixiu,"
					+ "sum(ifnull(caothap,0)) caothap,sum(ifnull(slot_bitcoin,0)) slot_bitcoin,sum(ifnull(slot_taydu,0)) slot_taydu,"
					+ "sum(ifnull(slot_angrybird,0)) slot_angrybird,sum(ifnull(slot_thantai,0)) slot_thantai,sum(ifnull(slot_thethao, 0)) slot_thethao,"
					+ "sum(ifnull(slot_chiemtinh,0)) slot_chiemtinh,sum(ifnull(taixiu_st,0)) taixiu_st,sum(ifnull(fish,0)) fish,"
					+ "sum(ifnull(slot_thanbai,0)) slot_thanbai,sum(ifnull(ebet,0)) ebet,sum(ifnull(sbo,0)) sbo,"
					+ "sum(ifnull(slot_bikini,0)) slot_bikini,sum(ifnull(slot_galaxy, 0)) slot_galaxy "
					+ "from log_count_user_play where (1=1) " + conditions + " group by nick_name";
			String sqlCount = "select count(*) total from (" + sql + ") AS total";
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			PreparedStatement stmt = conn.prepareStatement(sql + paging);
			int index = 1;
			if (!(fromTime == null || fromTime.trim().isEmpty())) {
				stmt.setString(index, fromTime + " 00:00:00");
				stmtCount.setString(index, fromTime + " 00:00:00");
				index++;
			}
			
			if (!(endTime == null || endTime.trim().isEmpty())) {
				stmt.setString(index, endTime + " 23:59:59");
				stmtCount.setString(index, endTime + " 23:59:59");
				index++;
			}
			
			if (!(nick_name == null || nick_name.trim().isEmpty())) {
				stmt.setString(index, nick_name);
				stmtCount.setString(index, nick_name);
				index++;
			}
			
			if (page != -1 && maxItem != -1) {
				stmt.setInt(index++, page * maxItem);
				stmt.setInt(index++, maxItem);
			}
			
			ResultSet rs = stmt.executeQuery();
			List<Map<String, Object>> list = new ArrayList<>();
			while (rs.next()) {
				Map<String, Object> objectMap = new HashMap<>();
				objectMap.put("nick_name", rs.getObject("nick_name"));
				objectMap.put("wm", rs.getObject("wm"));
				objectMap.put("ibc", rs.getObject("ibc"));
				objectMap.put("ag", rs.getObject("ag"));
				objectMap.put("cmd", rs.getObject("cmd"));
				objectMap.put("tlmn", rs.getObject("tlmn"));
				objectMap.put("bacay", rs.getObject("bacay"));
				objectMap.put("xocdia", rs.getObject("xocdia"));
				objectMap.put("minipoker", rs.getObject("minipoker"));
				objectMap.put("slot_pokemon", rs.getObject("slot_pokemon"));
				objectMap.put("baucua", rs.getObject("baucua"));
				objectMap.put("taixiu", rs.getObject("taixiu"));
				objectMap.put("caothap", rs.getObject("caothap"));
				objectMap.put("slot_bitcoin", rs.getObject("slot_bitcoin"));
				objectMap.put("slot_taydu", rs.getObject("slot_taydu"));
				objectMap.put("slot_angrybird", rs.getObject("slot_angrybird"));
				objectMap.put("slot_thantai", rs.getObject("slot_thantai"));
				objectMap.put("slot_thethao", rs.getObject("slot_thethao"));
				objectMap.put("slot_chiemtinh", rs.getObject("slot_chiemtinh"));
				objectMap.put("taixiu_st", rs.getObject("taixiu_st"));
				objectMap.put("fish", rs.getObject("fish"));
				objectMap.put("slot_thanbai", rs.getObject("slot_thanbai"));
				objectMap.put("ebet", rs.getObject("ebet"));
				objectMap.put("sbo", rs.getObject("sbo"));
				objectMap.put("slot_bikini", rs.getObject("slot_bikini"));
				objectMap.put("slot_galaxy", rs.getObject("slot_galaxy"));
				list.add(objectMap);
			}
			
			Map<String, Object> data = new HashMap<>();
			data.put("list", list);
			long totalRecords = 0;
			ResultSet rsCount = stmtCount.executeQuery();
			while (rsCount.next()) {
				totalRecords = rsCount.getObject("total") == null ? 0 : rsCount.getLong("total");
			}
			
			data.put("totalRecords", totalRecords);
			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return data;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(
					"getUserDetailAgent(List<String> fields, String nick_name, String currentTime, String referCode, int page, int maxItem): "
							+ e.getMessage());
			return null;
		}
	}
	
	// -------------Syndata from log game to log_report_user-------------//
	private List<String> GetUserPlays(String currentDate, MongoCollection<?> collection, String fieldFilterName,
			String distinctField, boolean isTypeDate) {
		try {
			HashMap<String, Object> conditions = new HashMap<String, Object>();
			if (currentDate != null && !currentDate.isEmpty()) {
				BasicDBObject obj = new BasicDBObject();
				if (isTypeDate) {
					obj.put("$gte", new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss").parse(currentDate + " 00:00:00"));
					obj.put("$lte", new SimpleDateFormat("yyyyy-MM-dd hh:mm:ss").parse(currentDate + " 23:59:59"));
				} else {
					obj.put("$gte", currentDate + " 00:00:00");
					obj.put("$lte", currentDate + " 23:59:59");
				}

				conditions.put(fieldFilterName, obj);
			}

			DistinctIterable<String> rs = collection.distinct(distinctField, new Document(conditions), String.class);
			List<String> nickNames = new ArrayList<>();
			for (String item : rs) {
				if (item != null && !item.trim().isEmpty())
					nickNames.add(item);
			}

			return nickNames;
		} catch (Exception e) {
			logger.error("GetUserPlays: " + e.getMessage());
			return new ArrayList<>();
		}
	}

	private LogReportModel getLogReportModelSQL(String username, String currentDate) {
		String sql = "SELECT * FROM vinplay.log_report_user WHERE nick_name=? AND time_report=?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int param = 1;
			stm.setString(param++, username);
			stm.setString(param++, currentDate);
			ResultSet rs = stm.executeQuery();
			LogReportModel logReportModel = new LogReportModel();
			if (rs.next()) {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				logReportModel.id = rs.getLong("id");
				logReportModel.time = df.format(rs.getDate("time_report"));
				logReportModel.nick_name = rs.getString("nick_name");
				logReportModel.wm = rs.getLong("wm");
				logReportModel.wm_win = rs.getLong("wm_win");
				logReportModel.ibc = rs.getLong("ibc");
				logReportModel.ibc_win = rs.getLong("ibc_win");
				logReportModel.ag = rs.getLong("ag");
				logReportModel.ag_win = rs.getLong("ag_win");
				logReportModel.cmd = rs.getLong("cmd");
				logReportModel.cmd_win = rs.getLong("cmd_win");
				logReportModel.tlmn = rs.getLong("tlmn");
				logReportModel.tlmn_win = rs.getLong("tlmn_win");
				logReportModel.bacay = rs.getLong("bacay");
				logReportModel.bacay_win = rs.getLong("bacay_win");
				logReportModel.xocdia = rs.getLong("xocdia");
				logReportModel.xocdia_win = rs.getLong("xocdia_win");
				logReportModel.minipoker = rs.getLong("minipoker");
				logReportModel.minipoker_win = rs.getLong("minipoker_win");
				logReportModel.slot_pokemon = rs.getLong("slot_pokemon");
				logReportModel.slot_pokemon_win = rs.getLong("slot_pokemon_win");
				logReportModel.baucua = rs.getLong("baucua");
				logReportModel.baucua_win = rs.getLong("baucua_win");
				logReportModel.taixiu = rs.getLong("taixiu");
				logReportModel.taixiu_win = rs.getLong("taixiu_win");
				logReportModel.caothap = rs.getLong("caothap");
				logReportModel.caothap_win = rs.getLong("caothap_win");
				logReportModel.slot_bitcoin = rs.getLong("slot_bitcoin");
				logReportModel.slot_bitcoin_win = rs.getLong("slot_bitcoin_win");
				logReportModel.slot_taydu = rs.getLong("slot_taydu");
				logReportModel.slot_taydu_win = rs.getLong("slot_taydu_win");
				logReportModel.slot_angrybird = rs.getLong("slot_angrybird");
				logReportModel.slot_angrybird_win = rs.getLong("slot_angrybird_win");
				logReportModel.slot_thantai = rs.getLong("slot_thantai");
				logReportModel.slot_thantai_win = rs.getLong("slot_thantai_win");
				logReportModel.slot_thethao = rs.getLong("slot_thethao");
				logReportModel.slot_thethao_win = rs.getLong("slot_thethao_win");
				logReportModel.slot_chiemtinh = rs.getLong("slot_chiemtinh");
				logReportModel.slot_chiemtinh_win = rs.getLong("slot_chiemtinh_win");
				logReportModel.taixiu_st = rs.getLong("taixiu_st");
				logReportModel.taixiu_st_win = rs.getLong("taixiu_st_win");
				logReportModel.fish = rs.getLong("fish");
				logReportModel.fish_win = rs.getLong("fish_win");
				logReportModel.deposit = rs.getLong("deposit");
				logReportModel.withdraw = rs.getLong("withdraw");
				logReportModel.totalRefund = rs.getLong("t_refund");
				logReportModel.totalBonus = rs.getLong("t_bonus");
				logReportModel.code = rs.getString("code");
				logReportModel.slot_bikini = rs.getLong("slot_bikini");
				logReportModel.slot_bikini_win = rs.getLong("slot_bikini_win");
				logReportModel.slot_galaxy = rs.getLong("slot_galaxy");
				logReportModel.slot_galaxy_win = rs.getLong("slot_galaxy_win");
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return logReportModel;
		} catch (Exception e) {
			logger.error("getLogReportModelSQL: " + e.getMessage());
			return null;
		}
	}

	private boolean updateLogReportUser(LogReportModel logReportModel) {
		String sql = "UPDATE vinplay.log_report_user SET wm=?, wm_win=?, ibc=?, ibc_win=?, ag=?, ag_win=?, tlmn=?,tlmn_win=?, bacay=?, bacay_win=?,"
				+ " xocdia=?, xocdia_win=?, minipoker=?, minipoker_win=?, slot_pokemon=?, slot_pokemon_win=?, baucua=?, baucua_win=?,"
				+ " taixiu=?, taixiu_win=?, caothap=?, caothap_win=?, slot_bitcoin=?, slot_bitcoin_win=?, slot_taydu=?, slot_taydu_win=?,"
				+ " slot_angrybird=?, slot_angrybird_win=?, slot_thantai=?, slot_thantai_win=?, slot_thethao=?, slot_thethao_win=?,"
				+ " deposit=?, withdraw=?, t_bonus=?, cmd=?, cmd_win=?, t_refund=? , code=? ,slot_chiemtinh=? , slot_chiemtinh_win=?,"
				+ " taixiu_st=?, taixiu_st_win=?, fish=?, fish_win=?, slot_bikini=?, slot_bikini_win=?, slot_galaxy=?, slot_galaxy_win=? "
				+ " WHERE nick_name=? AND time_report=?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
				PreparedStatement stm = conn.prepareStatement(sql);) {
			int param = 1;
			stm.setLong(param++, logReportModel.wm);
			stm.setLong(param++, logReportModel.wm_win);
			stm.setLong(param++, logReportModel.ibc);
			stm.setLong(param++, logReportModel.ibc_win);
			stm.setLong(param++, logReportModel.ag);
			stm.setLong(param++, logReportModel.ag_win);
			stm.setLong(param++, logReportModel.tlmn);
			stm.setLong(param++, logReportModel.tlmn_win);
			stm.setLong(param++, logReportModel.bacay);
			stm.setLong(param++, logReportModel.bacay_win);
			stm.setLong(param++, logReportModel.xocdia);
			stm.setLong(param++, logReportModel.xocdia_win);
			stm.setLong(param++, logReportModel.minipoker);
			stm.setLong(param++, logReportModel.minipoker_win);
			stm.setLong(param++, logReportModel.slot_pokemon);
			stm.setLong(param++, logReportModel.slot_pokemon_win);
			stm.setLong(param++, logReportModel.baucua);
			stm.setLong(param++, logReportModel.baucua_win);
			stm.setLong(param++, logReportModel.taixiu);
			stm.setLong(param++, logReportModel.taixiu_win);
			stm.setLong(param++, logReportModel.caothap);
			stm.setLong(param++, logReportModel.caothap_win);
			stm.setLong(param++, logReportModel.slot_bitcoin);
			stm.setLong(param++, logReportModel.slot_bitcoin_win);
			stm.setLong(param++, logReportModel.slot_taydu);
			stm.setLong(param++, logReportModel.slot_taydu_win);
			stm.setLong(param++, logReportModel.slot_angrybird);
			stm.setLong(param++, logReportModel.slot_angrybird_win);
			stm.setLong(param++, logReportModel.slot_thantai);
			stm.setLong(param++, logReportModel.slot_thantai_win);
			stm.setLong(param++, logReportModel.slot_thethao);
			stm.setLong(param++, logReportModel.slot_thethao_win);
			stm.setLong(param++, logReportModel.deposit);
			stm.setLong(param++, logReportModel.withdraw);
			stm.setLong(param++, logReportModel.totalBonus);
			stm.setLong(param++, logReportModel.cmd);
			stm.setLong(param++, logReportModel.cmd_win);
			stm.setLong(param++, logReportModel.totalRefund);
			stm.setString(param++, logReportModel.code);
			// add more new game
			stm.setLong(param++, logReportModel.slot_chiemtinh);
			stm.setLong(param++, logReportModel.slot_chiemtinh_win);
			stm.setLong(param++, logReportModel.taixiu_st);
			stm.setLong(param++, logReportModel.taixiu_st_win);
			stm.setLong(param++, logReportModel.fish);
			stm.setLong(param++, logReportModel.fish_win);

			stm.setLong(param++, logReportModel.slot_bikini);
			stm.setLong(param++, logReportModel.slot_bikini_win);
			stm.setLong(param++, logReportModel.slot_galaxy);
			stm.setLong(param++, logReportModel.slot_galaxy_win);
			//
			stm.setString(param++, logReportModel.nick_name);
			stm.setDate(param++, java.sql.Date.valueOf(logReportModel.time));
			int value = stm.executeUpdate();
			return value >= 1;
		} catch (Exception e) {
			logger.info(e.getMessage());
		}

		return false;
	}
	
	private boolean updateLogReportUserByDate(String gameName, String currentDate) {
		String sql = "";
		switch (gameName) {
		case "ibc":
			sql = "UPDATE vinplay.log_report_user SET ibc=0, ibc_win=0 WHERE time_report=?";
			break;
		case "wm":
			sql = "UPDATE vinplay.log_report_user SET wm=0, wm_win=0 WHERE time_report=?";
			break;
		case "ag":
			sql = "UPDATE vinplay.log_report_user SET ag=0, ag_win=0 WHERE time_report=?";
			break;
		}
		
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
				PreparedStatement stm = conn.prepareStatement(sql);) {
			int param = 1;
			
			stm.setDate(param++, java.sql.Date.valueOf(currentDate));
			int value = stm.executeUpdate();
			return value >= 0;
		} catch (Exception e) {
			logger.info(e.getMessage());
		}

		return false;
	}
	
	private String getCodeByNickName (String nickname) {
		String sql = "SELECT referral_code FROM vinplay.users WHERE nick_name=?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int param = 1;
			stm.setString(param++, nickname);
			ResultSet rs = stm.executeQuery();
			LogReportModel logReportModel = new LogReportModel();
			String code = "";
			if (rs.next()) {
				code = rs.getString("referral_code");
			}
			
			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}
			
			return code;
		}catch (Exception e) {
			logger.error("getCodeByNickName: " + e.getMessage());
			return "";
		}
	}
	
	public boolean insertNewLogSQL(LogReportModel logReportModel) {
		Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
		String sql = "INSERT INTO vinplay.log_report_user (time_report,nick_name,wm,wm_win,ibc,ibc_win,ag,ag_win,tlmn,tlmn_win,"
				+ "bacay,bacay_win,"
				+ "xocdia,xocdia_win,minipoker,minipoker_win,slot_pokemon,slot_pokemon_win,baucua,baucua_win,taixiu,taixiu_win,"
				+ "caothap,caothap_win,slot_bitcoin,slot_bitcoin_win,slot_taydu,slot_taydu_win,slot_angrybird,slot_angrybird_win,"
				+ "slot_thantai,slot_thantai_win,slot_thethao,slot_thethao_win,deposit,withdraw,t_bonus,cmd,cmd_win,t_refund,code, "
				+ "slot_chiemtinh, slot_chiemtinh_win ,taixiu_st,taixiu_st_win,fish,fish_win, slot_bikini,slot_bikini_win, slot_galaxy,slot_galaxy_win " + // 45 games
				")  VALUES (?,?,?,?,?,?," + "?,?,?,?,?," + "?,?,?,?,?," + "?,?,?,?,?," + "?,?,?,?,?," + "?,?,?,?,?,"
				+ "?,?,?,?,?," + "?,?,?,?,?," + "?,?,?,?,?,?)" + ",?,?,?,?)";
		try {
			PreparedStatement stm = conn.prepareStatement(sql);
			int param = 1;
			stm.setDate(param++, java.sql.Date.valueOf(logReportModel.time));
			stm.setString(param++, logReportModel.nick_name);
			stm.setLong(param++, logReportModel.wm);
			stm.setLong(param++, logReportModel.wm_win);
			stm.setLong(param++, logReportModel.ibc);
			stm.setLong(param++, logReportModel.ibc_win);
			stm.setLong(param++, logReportModel.ag);
			stm.setLong(param++, logReportModel.ag_win);
			stm.setLong(param++, logReportModel.tlmn);
			stm.setLong(param++, logReportModel.tlmn_win);
			stm.setLong(param++, logReportModel.bacay);
			stm.setLong(param++, logReportModel.bacay_win);
			stm.setLong(param++, logReportModel.xocdia);
			stm.setLong(param++, logReportModel.xocdia_win);
			stm.setLong(param++, logReportModel.minipoker);
			stm.setLong(param++, logReportModel.minipoker_win);
			stm.setLong(param++, logReportModel.slot_pokemon);
			stm.setLong(param++, logReportModel.slot_pokemon_win);
			stm.setLong(param++, logReportModel.baucua);
			stm.setLong(param++, logReportModel.baucua_win);
			stm.setLong(param++, logReportModel.taixiu);
			stm.setLong(param++, logReportModel.taixiu_win);
			stm.setLong(param++, logReportModel.caothap);
			stm.setLong(param++, logReportModel.caothap_win);
			stm.setLong(param++, logReportModel.slot_bitcoin);
			stm.setLong(param++, logReportModel.slot_bitcoin_win);
			stm.setLong(param++, logReportModel.slot_taydu);
			stm.setLong(param++, logReportModel.slot_taydu_win);
			stm.setLong(param++, logReportModel.slot_angrybird);
			stm.setLong(param++, logReportModel.slot_angrybird_win);
			stm.setLong(param++, logReportModel.slot_thantai);
			stm.setLong(param++, logReportModel.slot_thantai_win);
			stm.setLong(param++, logReportModel.slot_thethao);
			stm.setLong(param++, logReportModel.slot_thethao_win);
			stm.setLong(param++, logReportModel.deposit);
			stm.setLong(param++, logReportModel.withdraw);
			stm.setLong(param++, logReportModel.totalBonus);
			stm.setLong(param++, logReportModel.cmd);
			stm.setLong(param++, logReportModel.cmd_win);
			stm.setLong(param++, logReportModel.totalRefund);
			stm.setString(param++, logReportModel.code);
			stm.setLong(param++, logReportModel.slot_chiemtinh);
			stm.setLong(param++, logReportModel.slot_chiemtinh_win);
			stm.setLong(param++, logReportModel.taixiu_st);
			stm.setLong(param++, logReportModel.taixiu_st_win);
			stm.setLong(param++, logReportModel.fish);
			stm.setLong(param++, logReportModel.fish_win);
			stm.setLong(param++, logReportModel.slot_bikini);
			stm.setLong(param++, logReportModel.slot_bikini_win);
			stm.setLong(param++, logReportModel.slot_galaxy);
			stm.setLong(param++, logReportModel.slot_galaxy_win);
			int value = stm.executeUpdate();
			return value == 1;
		} catch (Exception e) {
			logger.info(e.getMessage());
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {

				}
			}
		}
		return false;
	}

	@Override
	public List<Map<String, Object>> SynDataLogGameToReport(String currentDate, String gameName) {
		List<Map<String, Object>> result = new ArrayList<>();
		try {
			if (gameName == null || gameName.trim().isEmpty())
				return new ArrayList<>();
			
			String collectionName = "";
			Document match = new Document();
			Document group = new Document();
			switch (gameName) {
			case "ibc":
				collectionName = "ibc2gamerecord";
				match = new Document("$match", 
        					new Document("winlostdatetime",
    							new Document("$gte", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(currentDate + " 00:00:00"))
    							.append( "$lte", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(currentDate + " 23:59:59")))
        					.append("ticketstatus", new Document("$in", Arrays.asList("won", "lose", "draw", "half won", "half lose"))));
				group = new Document("$group",
    						new Document("_id","$nick_name")
    						.append("totalValidBet",new Document("$sum", "$actual_stake"))
    						.append("totalPayout",new Document("$sum", "$winloseamount")));
				break;
			case "wm":
				collectionName = "wmgamerecord";
				match = new Document("$match", new Document("bettime",
							new Document("$gte", currentDate + " 00:00:00").append( "$lte", currentDate + " 23:59:59")));
				group = new Document("$group",
							new Document("_id","$nick_name")
							.append("totalValidBet",new Document("$sum", "$validbet"))
							.append("totalPayout",new Document("$sum", "$winloss")));
				break;
			case "ag":
				collectionName = "aggamerecord";
				match = new Document("$match", 
						new Document("bettime",
							new Document("$gte", currentDate + " 00:00:00").append( "$lte", currentDate + " 23:59:59"))
						.append("flag", new Document("$in", Arrays.asList("1", "4"))));
				group = new Document("$group",
						new Document("_id","$nickName")
						.append("totalValidBet",new Document("$sum", "$validbet"))
						.append("totalPayout",new Document("$sum", "$payout")));
				break;
			case "ebet":
				collectionName = "ebetgamerecord";
				match = new Document("$match", new Document("createtime",
							new Document("$gte", currentDate + " 00:00:00").append( "$lte", currentDate + " 23:59:59")));
				group = new Document("$group",
							new Document("_id","$nick_name")
							.append("totalValidBet",new Document("$sum", "$bet"))
							.append("totalPayout",new Document("$sum", "$payout")));
				break;
			}

			MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
			MongoCollection<?> collection = db.getCollection(collectionName);
			AggregateIterable<Document> aggregates = (AggregateIterable<Document>) collection.aggregate(Arrays.asList(match, group));
			boolean status = false;
			updateLogReportUserByDate(gameName, currentDate);
			Map<String, Object> hasMap = new HashMap<>();
			
			for (Document document : aggregates) {
				String nickname = "";
				try {
					nickname = document.get("_id") == null ? "" : document.get("_id").toString();
					LogReportModel logReportModel = getLogReportModelSQL(nickname, currentDate);
					Long totalValidBet = 0l;
					Long totalPayout = 0l;
					try {
						totalValidBet = document.getDouble("totalValidBet").longValue();
					} catch (Exception e) {
						totalValidBet = Long.parseLong(document.get("totalValidBet").toString());
					}

					try {
						totalPayout = document.getDouble("totalPayout").longValue();
					} catch (Exception e) {
						totalPayout = Long.parseLong(document.get("totalPayout").toString());
					}

					switch (gameName) {
					case "ibc":
						logReportModel.ibc = Math.abs(totalValidBet);
						logReportModel.ibc_win = Math.abs(totalPayout);
						break;
					case "wm":
						logReportModel.wm = Math.abs(totalValidBet);
						logReportModel.wm_win = Math.abs(totalPayout);
						break;
					case "ag":
						logReportModel.ag = Math.abs(totalValidBet);
						logReportModel.ag_win = Math.abs(totalPayout);
						break;
					case "ebet":
						logReportModel.ebet = Math.abs(totalValidBet);
						logReportModel.ebet_win = Math.abs(totalPayout);
						break;
					}
					
					HazelcastInstance instance = HazelcastClientFactory.getInstance();
					IMap userMap = instance.getMap("users");
					String code = "";
					if (userMap.containsKey(nickname)) {
						UserCacheModel userCache = (UserCacheModel) userMap.get(nickname);
						if(userCache != null)
							code = userCache.getReferralCode();
						else {
							code = getCodeByNickName(nickname);
						}
					}
					
					if(code == null || code.trim().isEmpty()) {
						code = getCodeByNickName(nickname);
					}
					
					if (logReportModel.id > 0) {
						logReportModel.code = code;
						status = updateLogReportUser(logReportModel);
					} else {
						logReportModel.nick_name = nickname;
						logReportModel.time = currentDate;
						logReportModel.code = code;
						status = insertNewLogSQL(logReportModel);
					}
					
					hasMap = new HashMap<>();
					if (status)
						hasMap.put("success", nickname);
					else
						hasMap.put("error", nickname + " | Reason: Update failure");

					result.add(hasMap);
				} catch (Exception e) {
					logger.error("GetUserPlays state-1: " + e.getMessage());
					hasMap = new HashMap<>();
					hasMap.put("error", nickname + " | Reason: " + e.getMessage() + " log report user with nickname: "
							+ nickname + " and time report: " + currentDate);
					result.add(hasMap);
				}
			}
		} catch (Exception e) {
			logger.error("GetUserPlays state-2: " + e.getMessage());
			Map<String, Object> hasMap = new HashMap<>();
			hasMap.put("error", e.getMessage());
			result.add(hasMap);
		}

		return result;
	}
	// ------------------------------------------------------------------//
	
	// ----------------------For unlimit level agent---------------------//
	@Override
	public List<String> getListCode() throws SQLException {
		List<String> result = new ArrayList<>();
		String sql = "select GROUP_CONCAT(DISTINCT `code` SEPARATOR ',') codes from useragent WHERE `code` is not null and `code` <> ''";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = Arrays.asList(rs.getString("codes").split(","));
			}

			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getListCode: " + e.getMessage());
			return new ArrayList<>();
		}
	}
	
	public String getListCodeChildsById(int id) throws SQLException {
		String result = "";
		String sql = "select GROUP_CONCAT(DISTINCT `code` SEPARATOR ',') codes from useragent"
				+ " WHERE FIND_IN_SET('" + id + "',ancestors) and `code` is not null and `code` <> ''"
				+ " order by id DESC limit 1;";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = rs.getString("codes");
			}
			
			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getListCode: " + e.getMessage());
			return "";
		}
	}
	
	public String getListCodeChildsByCode(String code) throws SQLException {
		String result = "";
		String sql = "select GROUP_CONCAT(DISTINCT `code` SEPARATOR ',') codes from useragent"
				+ " WHERE FIND_IN_SET((select id from useragent where `code` = ?),ancestors) and `code` is not null and `code` <> ''"
				+ " order by id DESC limit 1;";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin");) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int index = 1;
			stm.setString(index++, code);
			ResultSet rs = stm.executeQuery();
			while (rs.next()) {
				result = rs.getString("codes");
			}
			
			rs.close();
			stm.close();
			if (conn != null) {
				conn.close();
			}

			return result;
		} catch (SQLException e) {
			logger.error("getListCode: " + e.getMessage());
			return "";
		}
	}
	
	@Override
	public Map<String, Object> getAllChilds(Integer id, int pageIndex, int limit) throws SQLException {
		Map<String, Object> map = new HashMap<>();
		List<UserAgentModel> userAgentModels = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			pageIndex = pageIndex < 1 ? 0 : pageIndex - 1;
			String paginateCondition = (pageIndex == -1 || limit == -1) ? "" : (" limit ?,?");
			String sql = "select * from useragent where find_in_set(" + id + ",ancestors) > 0 ORDER BY id" + paginateCondition;
			PreparedStatement stmt = conn.prepareStatement(sql);
			int index = 1;
			if (-1 != pageIndex && -1 != limit) {
				stmt.setInt(index++, pageIndex * limit);
				stmt.setInt(index++, limit);
			}
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				UserAgentModel userAgentModel = new UserAgentModel();
				userAgentModel.setId(rs.getInt("id"));
				userAgentModel.setUsername(rs.getString("username"));
				userAgentModel.setNickname(rs.getString("nickname"));
				userAgentModel.setNameagent(rs.getString("nameagent"));
				userAgentModel.setAddress(rs.getString("address"));
				userAgentModel.setPhone(rs.getString("phone"));
				userAgentModel.setEmail(rs.getString("email"));
				userAgentModel.setFacebook(rs.getString("facebook"));
				userAgentModel.setKey(rs.getString("key"));
				userAgentModel.setStatus(rs.getString("status"));
				userAgentModel.setParentid(rs.getInt("parentid"));
				userAgentModel.setNamebank(rs.getString("namebank"));
				userAgentModel.setNameaccount(rs.getString("nameaccount"));
				userAgentModel.setNumberaccount(rs.getString("numberaccount"));
				userAgentModel.setShow(rs.getInt("show"));
				userAgentModel.setActive(rs.getInt("active"));
				userAgentModel.setCreatetime(rs.getDate("createtime"));
				userAgentModel.setUpdatetime(rs.getDate("updatetime"));
				userAgentModel.setOrder(rs.getInt("order"));
				userAgentModel.setSms(rs.getInt("sms"));
				userAgentModel.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				userAgentModel.setCommission_rate(rs.getDouble("commission_rate"));
				userAgentModel.setSite(rs.getString("site"));
				userAgentModel.setLast_login_time(rs.getDate("last_login_time"));
				userAgentModel.setLogin_times(rs.getInt("login_times"));
				userAgentModel.setLevel(rs.getInt("level"));
				userAgentModel.setCode(rs.getString("code"));
				userAgentModel.setAncestors(rs.getString("ancestors"));
				userAgentModels.add(userAgentModel);
			}
			
			map.put("childs", userAgentModels);
			String sqlCount = "select count(id) total from useragent where find_in_set(" + id + ",ancestors) > 0";
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			ResultSet rsCount = stmtCount.executeQuery();
			while (rsCount.next()) {
				map.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getInt("total"));
			}
			
			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return map;
		} catch (SQLException e) {
			map.put("childs", new ArrayList<>());
			map.put("total", 0);
			return map;
		}
	}
	
	@Override
	public Map<String, Object> searchChilds(Integer id, String keyword, int level, int pageIndex, int limit) throws SQLException {
		Map<String, Object> map = new HashMap<>();
		List<UserAgentModel> userAgentModels = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			pageIndex = pageIndex < 1 ? 0 : pageIndex - 1;
			String paginateCondition = (pageIndex == -1 || limit == -1) ? "" : (" limit ?,?");
			String keywordCondition = "";
			if(!StringUtils.isBlank(keyword)) {
				keywordCondition = " and ("
						+ "(username like ?) or "
						+ "(nickname like ?) or "
						+ "(nameagent like ?) or "
						+ "(address like ?) or "
						+ "(phone like ?) or "
						+ "(email like ?) or "
						+ "(facebook like ?) or "
						+ "(namebank like ?) or "
						+ "(nameaccount like ?) or "
						+ "(numberaccount like ?) or "
						+ "(`code` like ?)"
						+ ")";
			}
			
			String levelCondition = "";
			if((-1 != level))
				levelCondition = " and (level = ?)";
			
			String parentIdCondition = "";
			if((-1 != id))
				parentIdCondition = " and (find_in_set(" + id + ",ancestors) > 0)";
				
			String sql = "select * from useragent where (1=1)"
					+ parentIdCondition
					+ keywordCondition
					+ levelCondition
					+ " ORDER BY level asc, id desc" + paginateCondition;
			String sqlCount = "select count(id) total from useragent where (1=1)"
					+ parentIdCondition
		  			+ keywordCondition
		  			+ levelCondition;
			PreparedStatement stmt = conn.prepareStatement(sql);
			PreparedStatement stmtCount = conn.prepareStatement(sqlCount);
			int index = 1;
			if(!StringUtils.isBlank(keyword)) {
				for(int i = index; i < 12; i++) {
					stmt.setString(index, "%" + keyword + "%");
					stmtCount.setString(index, "%" + keyword + "%");
					index ++;
				}
			}
			
			if((-1 != level)) {
				stmt.setInt(index, level);
				stmtCount.setInt(index, level);
				index++;
			}
			
			if (-1 != pageIndex && -1 != limit) {
				stmt.setInt(index++, pageIndex * limit);
				stmt.setInt(index++, limit);
			}
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				UserAgentModel userAgentModel = new UserAgentModel();
				userAgentModel.setId(rs.getInt("id"));
				userAgentModel.setUsername(rs.getString("username"));
				userAgentModel.setNickname(rs.getString("nickname"));
				userAgentModel.setNameagent(rs.getString("nameagent"));
				userAgentModel.setAddress(rs.getString("address"));
				userAgentModel.setPhone(rs.getString("phone"));
				userAgentModel.setEmail(rs.getString("email"));
				userAgentModel.setFacebook(rs.getString("facebook"));
				userAgentModel.setKey(rs.getString("key"));
				userAgentModel.setStatus(rs.getString("status"));
				userAgentModel.setParentid(rs.getInt("parentid"));
				userAgentModel.setNamebank(rs.getString("namebank"));
				userAgentModel.setNameaccount(rs.getString("nameaccount"));
				userAgentModel.setNumberaccount(rs.getString("numberaccount"));
				userAgentModel.setShow(rs.getInt("show"));
				userAgentModel.setActive(rs.getInt("active"));
				userAgentModel.setCreatetime(rs.getDate("createtime"));
				userAgentModel.setUpdatetime(rs.getDate("updatetime"));
				userAgentModel.setOrder(rs.getInt("order"));
				userAgentModel.setSms(rs.getInt("sms"));
				userAgentModel.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				userAgentModel.setCommission_rate(rs.getDouble("commission_rate"));
				userAgentModel.setSite(rs.getString("site"));
				userAgentModel.setLast_login_time(rs.getDate("last_login_time"));
				userAgentModel.setLogin_times(rs.getInt("login_times"));
				userAgentModel.setLevel(rs.getInt("level"));
				userAgentModel.setCode(rs.getString("code"));
				userAgentModel.setAncestors(rs.getString("ancestors"));
				userAgentModels.add(userAgentModel);
			}
			
			map.put("childs", userAgentModels);
			ResultSet rsCount = stmtCount.executeQuery();
			while (rsCount.next()) {
				map.put("total", rsCount.getObject("total") == null ? 0 : rsCount.getInt("total"));
			}
			
			rs.close();
			rsCount.close();
			stmt.close();
			stmtCount.close();
			if (conn != null) {
				conn.close();
			}

			return map;
		} catch (SQLException e) {
			map.put("childs", new ArrayList<>());
			map.put("total", 0);
			return map;
		}
	}
	
	@Override
	public List<UserAgentModel> getParents(Integer id) throws SQLException {
		List<UserAgentModel> userAgentModels = new ArrayList<>();
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpool_admin")) {
			String sql = "select * from useragent where find_in_set(id, (select ancestors from useragent where id = ?)) > 0;";
			PreparedStatement stmt = conn.prepareStatement(sql);
			if (!(id == null || id == 0)) {
				stmt.setInt(1, id);
			} else
				return new ArrayList<>();

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				UserAgentModel userAgentModel = new UserAgentModel();
				userAgentModel.setId(rs.getInt("id"));
				userAgentModel.setUsername(rs.getString("username"));
				userAgentModel.setNickname(rs.getString("nickname"));
				userAgentModel.setNameagent(rs.getString("nameagent"));
				userAgentModel.setAddress(rs.getString("address"));
				userAgentModel.setPhone(rs.getString("phone"));
				userAgentModel.setEmail(rs.getString("email"));
				userAgentModel.setFacebook(rs.getString("facebook"));
				userAgentModel.setKey(rs.getString("key"));
				userAgentModel.setStatus(rs.getString("status"));
				userAgentModel.setParentid(rs.getInt("parentid"));
				userAgentModel.setNamebank(rs.getString("namebank"));
				userAgentModel.setNameaccount(rs.getString("nameaccount"));
				userAgentModel.setNumberaccount(rs.getString("numberaccount"));
				userAgentModel.setShow(rs.getInt("show"));
				userAgentModel.setActive(rs.getInt("active"));
				userAgentModel.setCreatetime(rs.getDate("createtime"));
				userAgentModel.setUpdatetime(rs.getDate("updatetime"));
				userAgentModel.setOrder(rs.getInt("order"));
				userAgentModel.setSms(rs.getInt("sms"));
				userAgentModel.setPercent_bonus_vincard(rs.getInt("percent_bonus_vincard"));
				userAgentModel.setCommission_rate(rs.getDouble("commission_rate"));
				userAgentModel.setSite(rs.getString("site"));
				userAgentModel.setLast_login_time(rs.getDate("last_login_time"));
				userAgentModel.setLogin_times(rs.getInt("login_times"));
				userAgentModel.setLevel(rs.getInt("level"));
				userAgentModel.setCode(rs.getString("code"));
				userAgentModel.setAncestors(rs.getString("ancestors"));
				userAgentModels.add(userAgentModel);
			}
			
			rs.close();
			stmt.close();
			if (conn != null) {
				conn.close();
			}
			
			return new ArrayList<>();
		} catch (SQLException e) {
			return new ArrayList<>();
		}
	}
	// ------------------------------------------------------------------//
}
