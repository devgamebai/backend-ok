package com.vinplay.vbee.rmq.loguserinday;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.vinplay.vbee.common.models.LogReportModel;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class LogReportUtil {
    private static final Logger logger = Logger.getLogger("vbee");
    public static boolean updateLogSQL(LogReportModel logReportModel){
    	if(logReportModel.code == null || logReportModel.code.trim().isEmpty()) {
    		logReportModel.code = getCodeByNickName(logReportModel.nick_name);
    	}
    	
        String sql = "UPDATE vinplay.log_report_user SET wm=?, wm_win=?, ibc=?, ibc_win=?, ag=?, ag_win=?, tlmn=?,tlmn_win=?, bacay=?, bacay_win=?," +
                "xocdia=?, xocdia_win=?, minipoker=?, minipoker_win=?, slot_pokemon=?, slot_pokemon_win=?, baucua=?, baucua_win=?," +
                "taixiu=?, taixiu_win=?, caothap=?, caothap_win=?, slot_bitcoin=?, slot_bitcoin_win=?, slot_taydu=?, slot_taydu_win=?," +
                "slot_angrybird=?, slot_angrybird_win=?, slot_thantai=?, slot_thantai_win=?, slot_thethao=?, slot_thethao_win=?," +
                "deposit=?, withdraw=?, t_bonus=?, cmd=?, cmd_win=?, t_refund=? , code=? ,slot_chiemtinh=? , slot_chiemtinh_win=?," +
                "taixiu_st=?, taixiu_st_win=?,fish=?,fish_win=?," +
                "slot_thanbai=?, slot_thanbai_win=?,ebet=?,ebet_win=?,sbo=?,sbo_win=?, slot_bikini=?, slot_bikini_win=?, slot_galaxy=?, slot_galaxy_win=?" +
                ",attendance=?" +
                ",sam=?,sam_win=?,binh=?,binh_win=?,tala=?,tala_win=?,lieng=?,lieng_win=?" +
                ",xito=?,xito_win=?,baicao=?,baicao_win=?,poker=?,poker_win=?,xidzach=?,xidzach_win=?" +
                ",hamcamap=?,hamcamap_win=?,taixiu_sicbo=?,taixiu_sicbo_win=?,over_under=?,over_under_win=?" +
                ",samtruyen=?,samtruyen_win=?,range_rover=?,range_rover_win=?,sexygirl=?,sexygirl_win=?,lode=?,lode_win=?" +
                " WHERE nick_name=? AND time_report=?";
        int param = 1;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
				PreparedStatement stm = conn.prepareStatement(sql);) 
		{
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
            //
            stm.setLong(param++, logReportModel.slot_thanbai);
            stm.setLong(param++, logReportModel.slot_thanbai_win);
            
            stm.setLong(param++, logReportModel.ebet);
            stm.setLong(param++, logReportModel.ebet_win);
            
            stm.setLong(param++, logReportModel.sbo);
            stm.setLong(param++, logReportModel.sbo_win);
            //
            stm.setLong(param++, logReportModel.slot_bikini);
            stm.setLong(param++, logReportModel.slot_bikini_win);

            stm.setLong(param++, logReportModel.slot_galaxy);
            stm.setLong(param++, logReportModel.slot_galaxy_win);
            
            stm.setLong(param++, logReportModel.attendance);

            stm.setLong(param++, logReportModel.sam);
            stm.setLong(param++, logReportModel.sam_win);
            stm.setLong(param++, logReportModel.binh);
            stm.setLong(param++, logReportModel.binh_win);
            stm.setLong(param++, logReportModel.tala);
            stm.setLong(param++, logReportModel.tala_win);
            stm.setLong(param++, logReportModel.lieng);
            stm.setLong(param++, logReportModel.lieng_win);
            stm.setLong(param++, logReportModel.xito);
            stm.setLong(param++, logReportModel.xito_win);
            stm.setLong(param++, logReportModel.baicao);
            stm.setLong(param++, logReportModel.baicao_win);
            stm.setLong(param++, logReportModel.poker);
            stm.setLong(param++, logReportModel.poker_win);
            stm.setLong(param++, logReportModel.xidzach);
            stm.setLong(param++, logReportModel.xidzach_win);
            stm.setLong(param++, logReportModel.hamcamap);
            stm.setLong(param++, logReportModel.hamcamap_win);
            stm.setLong(param++, logReportModel.taixiu_sicbo);
            stm.setLong(param++, logReportModel.taixiu_sicbo_win);
            stm.setLong(param++, logReportModel.over_under);
            stm.setLong(param++, logReportModel.over_under_win);
            stm.setLong(param++, logReportModel.samtruyen);
            stm.setLong(param++, logReportModel.samtruyen_win);
            stm.setLong(param++, logReportModel.range_rover);
            stm.setLong(param++, logReportModel.range_rover_win);
            stm.setLong(param++, logReportModel.sexygirl);
            stm.setLong(param++, logReportModel.sexygirl_win);
            stm.setLong(param++, logReportModel.lode);
            stm.setLong(param++, logReportModel.lode_win);

            stm.setString(param++, logReportModel.nick_name);
            stm.setDate(param++,  java.sql.Date.valueOf(logReportModel.time));
            int value = stm.executeUpdate();
            return value == 1;
        }catch (Exception e){
            logger.info(e.getMessage());
        }

        return false;
    }

//    public static boolean updateLog(LogReportModel logReportModel, int userID)  {
//        MongoDatabase db = MongoDBConnectionFactory.getDB();
//        MongoCollection col = db.getCollection(LogReportConfig.MONGO_COLLECTION);
//        BasicDBObject updateFields = new BasicDBObject();
//
//        updateFields.append("user_id", userID);
//
//        updateFields.append("wm", logReportModel.wm);
//        updateFields.append("wm_win", logReportModel.wm_win);
//
//        updateFields.append("ibc", logReportModel.ibc);
//        updateFields.append("ibc_win", logReportModel.ibc_win);
//
//        updateFields.append("ag", logReportModel.ag);
//        updateFields.append("ag_win", logReportModel.ag_win);
//
//        updateFields.append("tlmn", logReportModel.tlmn);
//
//        updateFields.append("bacay", logReportModel.bacay);
//        updateFields.append("bacay_win", logReportModel.bacay_win);
//
//        updateFields.append("xocdia", logReportModel.xocdia);
//        updateFields.append("xocdia_win", logReportModel.xocdia_win);
//
//        updateFields.append("minipoker", logReportModel.minipoker);
//        updateFields.append("minipoker_win", logReportModel.minipoker_win);
//
//        updateFields.append("slot_pokemon", logReportModel.slot_pokemon);
//        updateFields.append("slot_pokemon_win", logReportModel.slot_pokemon_win);
//
//        updateFields.append("baucua", logReportModel.baucua);
//        updateFields.append("baucua_win", logReportModel.baucua_win);
//
//        updateFields.append("taixiu", logReportModel.taixiu);
//        updateFields.append("taixiu_win", logReportModel.taixiu_win);
//
//        updateFields.append("caothap", logReportModel.caothap);
//        updateFields.append("caothap_win", logReportModel.caothap_win);
//
//        updateFields.append("slot_bitcoin", logReportModel.slot_bitcoin);
//        updateFields.append("slot_bitcoin_win", logReportModel.slot_bitcoin_win);
//
//        updateFields.append("slot_taydu", logReportModel.slot_taydu);
//        updateFields.append("slot_taydu_win", logReportModel.slot_taydu_win);
//
//        updateFields.append("slot_angrybird", logReportModel.slot_angrybird);
//        updateFields.append("slot_angrybird_win", logReportModel.slot_angrybird_win);
//
//        updateFields.append("slot_thantai", logReportModel.slot_thantai);
//        updateFields.append("slot_thantai_win", logReportModel.slot_thantai_win);
//
//        updateFields.append("slot_thethao", logReportModel.slot_thethao);
//        updateFields.append("slot_thethao_win", logReportModel.slot_thethao_win);
//        
//        updateFields.append("slot_chiemtinh", logReportModel.slot_chiemtinh);
//        updateFields.append("slot_chiemtinh_win", logReportModel.slot_chiemtinh_win);
//        
//        updateFields.append("taixiu_st", logReportModel.taixiu_st);
//        updateFields.append("taixiu_st_win", logReportModel.taixiu_st_win);
//        
//        updateFields.append("fish", logReportModel.fish);
//        updateFields.append("fish_win", logReportModel.fish_win);
//
//        updateFields.append("deposit", logReportModel.deposit);
//        updateFields.append("withdraw", logReportModel.withdraw);
//
//        BasicDBObject conditions = new BasicDBObject();
//        conditions.append("nick_name", logReportModel.nick_name);
//        conditions.append("time", logReportModel.time);
//
//        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
//        options.upsert(true);
//
//        col.findOneAndUpdate(conditions, new Document("$set", updateFields), options);
////            boolean insert = insertNewLog(logReportModel, userID);
////        }
//
//        return true;
//    }
    
    public static String getCodeByNickName (String nickname) {
		String sql = "SELECT referral_code FROM vinplay.users WHERE nick_name=?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
			PreparedStatement stm = conn.prepareStatement(sql);
			int param = 1;
			stm.setString(param++, nickname);
			ResultSet rs = stm.executeQuery();
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
    
    public static boolean insertNewLogSQL(LogReportModel logReportModel){
    	if(logReportModel.code == null || logReportModel.code.trim().isEmpty()) {
    		logReportModel.code = getCodeByNickName(logReportModel.nick_name);
    	}
    	
        String sql = "INSERT INTO vinplay.log_report_user (time_report,nick_name,wm,wm_win,ibc,"
        		+ "ibc_win,ag,ag_win,tlmn,tlmn_win," 
                + " bacay,bacay_win,xocdia,xocdia_win,minipoker," 
                + " minipoker_win,slot_pokemon,slot_pokemon_win,baucua,baucua_win,"
                + "taixiu,taixiu_win,caothap,caothap_win,slot_bitcoin," 
                + "slot_bitcoin_win,slot_taydu,slot_thantai,slot_thantai_win,slot_taydu_win,"
                + "slot_angrybird,slot_angrybird_win,slot_thethao,slot_thethao_win,deposit," 
                + "withdraw,t_bonus,cmd,cmd_win,t_refund,"
                + "code, slot_chiemtinh, slot_chiemtinh_win ,taixiu_st,taixiu_st_win,"
                + "fish,fish_win,slot_thanbai,slot_thanbai_win,ebet,"
                + "ebet_win,sbo,sbo_win, slot_bikini, slot_bikini_win,slot_galaxy, slot_galaxy_win, attendance,"
                + "sam,sam_win,binh,binh_win,tala,tala_win,lieng,lieng_win,"
                + "xito,xito_win,baicao,baicao_win,poker,poker_win,xidzach,xidzach_win,"
                + "hamcamap,hamcamap_win,taixiu_sicbo,taixiu_sicbo_win,over_under,over_under_win,"
                + "samtruyen,samtruyen_win,range_rover,range_rover_win,sexygirl,sexygirl_win,lode,lode_win"
                + ")  VALUES ("+
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?,"+
                "?,?,?,?,?,"+
                "?,?,?,?,?,"+
                "?,?,?,?,?,"+
                "?,?,?,"+
                "?,?,?,?,?,?,?,?," +
                "?,?,?,?,?,?,?,?," +
                "?,?,?,?,?,?," +
                "?,?,?,?,?,?,?,?)";
        
        int param = 1;
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
				PreparedStatement stm = conn.prepareStatement(sql);) {
			
            stm.setDate(param++,  java.sql.Date.valueOf(logReportModel.time));
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
            
            stm.setLong(param++, logReportModel.slot_thanbai);
            stm.setLong(param++, logReportModel.slot_thanbai_win);
            
            stm.setLong(param++, logReportModel.ebet);
            stm.setLong(param++, logReportModel.ebet_win);
            
            stm.setLong(param++, logReportModel.sbo);
            stm.setLong(param++, logReportModel.sbo_win);

            stm.setLong(param++, logReportModel.slot_bikini);
            stm.setLong(param++, logReportModel.slot_bikini_win);

            stm.setLong(param++, logReportModel.slot_galaxy);
            stm.setLong(param++, logReportModel.slot_galaxy_win);
            
            stm.setLong(param++, logReportModel.attendance);

            stm.setLong(param++, logReportModel.sam);
            stm.setLong(param++, logReportModel.sam_win);
            stm.setLong(param++, logReportModel.binh);
            stm.setLong(param++, logReportModel.binh_win);
            stm.setLong(param++, logReportModel.tala);
            stm.setLong(param++, logReportModel.tala_win);
            stm.setLong(param++, logReportModel.lieng);
            stm.setLong(param++, logReportModel.lieng_win);
            stm.setLong(param++, logReportModel.xito);
            stm.setLong(param++, logReportModel.xito_win);
            stm.setLong(param++, logReportModel.baicao);
            stm.setLong(param++, logReportModel.baicao_win);
            stm.setLong(param++, logReportModel.poker);
            stm.setLong(param++, logReportModel.poker_win);
            stm.setLong(param++, logReportModel.xidzach);
            stm.setLong(param++, logReportModel.xidzach_win);
            stm.setLong(param++, logReportModel.hamcamap);
            stm.setLong(param++, logReportModel.hamcamap_win);
            stm.setLong(param++, logReportModel.taixiu_sicbo);
            stm.setLong(param++, logReportModel.taixiu_sicbo_win);
            stm.setLong(param++, logReportModel.over_under);
            stm.setLong(param++, logReportModel.over_under_win);
            stm.setLong(param++, logReportModel.samtruyen);
            stm.setLong(param++, logReportModel.samtruyen_win);
            stm.setLong(param++, logReportModel.range_rover);
            stm.setLong(param++, logReportModel.range_rover_win);
            stm.setLong(param++, logReportModel.sexygirl);
            stm.setLong(param++, logReportModel.sexygirl_win);
            stm.setLong(param++, logReportModel.lode);
            stm.setLong(param++, logReportModel.lode_win);

            int value = stm.executeUpdate();
            return value == 1;
        }catch (Exception e){
            logger.info(e.getMessage());
        }
        return false;
    }

//    public static boolean insertNewLog(LogReportModel logReportModel, int userID){
//        MongoDatabase db = MongoDBConnectionFactory.getDB();
//        MongoCollection col = db.getCollection(LogReportConfig.MONGO_COLLECTION);
//        Document doc = new Document();
//        doc.append("time", logReportModel.time);
//
//        doc.append("nick_name", logReportModel.nick_name);
//        doc.append("user_id", userID);
//
//        doc.append("wm", logReportModel.wm);
//        doc.append("wm_win", logReportModel.wm_win);
//
//        doc.append("ibc", logReportModel.ibc);
//        doc.append("ibc_win", logReportModel.ibc_win);
//
//        doc.append("ag", logReportModel.ag);
//        doc.append("ag_win", logReportModel.ag_win);
//
//        doc.append("tlmn", logReportModel.tlmn);
//
//        doc.append("bacay", logReportModel.bacay);
//        doc.append("bacay_win", logReportModel.bacay_win);
//
//        doc.append("xocdia", logReportModel.xocdia);
//        doc.append("xocdia_win", logReportModel.xocdia_win);
//
//        doc.append("minipoker", logReportModel.minipoker);
//        doc.append("minipoker_win", logReportModel.minipoker_win);
//
//        doc.append("slot_pokemon", logReportModel.slot_pokemon);
//        doc.append("slot_pokemon_win", logReportModel.slot_pokemon_win);
//
//        doc.append("baucua", logReportModel.baucua);
//        doc.append("baucua_win", logReportModel.baucua_win);
//
//        doc.append("taixiu", logReportModel.taixiu);
//        doc.append("taixiu_win", logReportModel.taixiu_win);
//
//        doc.append("caothap", logReportModel.caothap);
//        doc.append("caothap_win", logReportModel.caothap_win);
//
//        doc.append("slot_bitcoin", logReportModel.slot_bitcoin);
//        doc.append("slot_bitcoin_win", logReportModel.slot_bitcoin_win);
//
//        doc.append("slot_taydu", logReportModel.slot_taydu);
//        doc.append("slot_taydu_win", logReportModel.slot_taydu_win);
//
//        doc.append("slot_angrybird", logReportModel.slot_angrybird);
//        doc.append("slot_angrybird_win", logReportModel.slot_angrybird_win);
//
//        doc.append("slot_thantai", logReportModel.slot_thantai);
//        doc.append("slot_thantai_win", logReportModel.slot_thantai_win);
//
//        doc.append("slot_thethao", logReportModel.slot_thethao);
//        doc.append("slot_thethao_win", logReportModel.slot_thethao_win);
//
//        doc.append("deposit", logReportModel.deposit);
//        doc.append("withdraw", logReportModel.withdraw);
//        
//        col.insertOne(doc);
//        return true;
//    }
    
//    private boolean isExistReport(String username, String currentTime) {
//    	String sql = "SELECT count(id) as cnt FROM vinplay.log_report_user WHERE nick_name=? AND time_report=?";
//    	int cnt = 0;
//    	try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
//    			PreparedStatement stm = conn.prepareStatement(sql);){
//    		stm.setString(1, username);
//            stm.setString(2, currentTime);
//            ResultSet rs =stm.executeQuery();
//            if(rs.next()) {
//            	cnt = rs.getInt("cnt");
//            }
//		}catch (Exception e) {
//			logger.error(e+"");
//		}
//    	return cnt >0;
//    }

    public static LogReportModel getLogReportModelSQL(String username, String currentTime) {
        LogReportModel logReportModel = new LogReportModel();
        
        String sql = "SELECT * FROM vinplay.log_report_user WHERE nick_name=? AND time_report=?";
		try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
				PreparedStatement stm = conn.prepareStatement(sql);) {

            int param = 1;
            stm.setString(param++, username);
            stm.setString(param++, currentTime);

            ResultSet rs =stm.executeQuery();
            if (rs.next()) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                logReportModel.id = rs.getInt("id");
                logReportModel.nick_name = rs.getString("nick_name");
                logReportModel.time = df.format(rs.getDate("time_report"));

                logReportModel.wm = rs.getLong("wm");
                logReportModel.wm_win = rs.getLong("wm_win");

                logReportModel.ibc = rs.getLong("ibc");
                logReportModel.ibc_win = rs.getLong("ibc_win");

                logReportModel.ag = rs.getLong("ag");
                logReportModel.ag_win = rs.getLong("ag_win");

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

                logReportModel.deposit = rs.getLong("deposit");
                logReportModel.withdraw = rs.getLong("withdraw");
                
                logReportModel.totalBonus = rs.getLong("t_bonus");
                logReportModel.cmd = rs.getLong("cmd");
                logReportModel.cmd_win = rs.getLong("cmd_win");
                logReportModel.totalRefund = rs.getLong("t_refund");
                logReportModel.code = rs.getString("code");
                
                logReportModel.slot_chiemtinh= rs.getLong("slot_chiemtinh");
                logReportModel.slot_chiemtinh_win= rs.getLong("slot_chiemtinh_win");
                
                logReportModel.taixiu_st= rs.getLong("taixiu_st");
                logReportModel.taixiu_st_win= rs.getLong("taixiu_st_win");
                
                logReportModel.fish= rs.getLong("fish");
                logReportModel.fish_win= rs.getLong("fish_win");
                
                logReportModel.slot_thanbai= rs.getLong("slot_thanbai");
                logReportModel.slot_thanbai_win= rs.getLong("slot_thanbai_win");
                
                logReportModel.ebet= rs.getLong("ebet");
                logReportModel.ebet_win= rs.getLong("ebet_win");
                
                logReportModel.sbo= rs.getLong("sbo");
                logReportModel.sbo_win= rs.getLong("sbo_win");

                logReportModel.slot_bikini = rs.getLong("slot_bikini");
                logReportModel.slot_bikini_win = rs.getLong("slot_bikini_win");

                logReportModel.slot_galaxy = rs.getLong("slot_galaxy");
                logReportModel.slot_galaxy_win = rs.getLong("slot_galaxy_win");
                
                logReportModel.attendance = rs.getLong("attendance");

                logReportModel.sam = rs.getLong("sam");
                logReportModel.sam_win = rs.getLong("sam_win");
                logReportModel.binh = rs.getLong("binh");
                logReportModel.binh_win = rs.getLong("binh_win");
                logReportModel.tala = rs.getLong("tala");
                logReportModel.tala_win = rs.getLong("tala_win");
                logReportModel.lieng = rs.getLong("lieng");
                logReportModel.lieng_win = rs.getLong("lieng_win");
                logReportModel.xito = rs.getLong("xito");
                logReportModel.xito_win = rs.getLong("xito_win");
                logReportModel.baicao = rs.getLong("baicao");
                logReportModel.baicao_win = rs.getLong("baicao_win");
                logReportModel.poker = rs.getLong("poker");
                logReportModel.poker_win = rs.getLong("poker_win");
                logReportModel.xidzach = rs.getLong("xidzach");
                logReportModel.xidzach_win = rs.getLong("xidzach_win");
                logReportModel.hamcamap = rs.getLong("hamcamap");
                logReportModel.hamcamap_win = rs.getLong("hamcamap_win");
                logReportModel.taixiu_sicbo = rs.getLong("taixiu_sicbo");
                logReportModel.taixiu_sicbo_win = rs.getLong("taixiu_sicbo_win");
                logReportModel.over_under = rs.getLong("over_under");
                logReportModel.over_under_win = rs.getLong("over_under_win");
                logReportModel.samtruyen = rs.getLong("samtruyen");
                logReportModel.samtruyen_win = rs.getLong("samtruyen_win");
                logReportModel.range_rover = rs.getLong("range_rover");
                logReportModel.range_rover_win = rs.getLong("range_rover_win");
                logReportModel.sexygirl = rs.getLong("sexygirl");
                logReportModel.sexygirl_win = rs.getLong("sexygirl_win");
                logReportModel.lode = rs.getLong("lode");
                logReportModel.lode_win = rs.getLong("lode_win");
            }
        }catch (Exception e){
            logger.info(e.getMessage());
        }
        return logReportModel;
    }

//
//    public static LogReportModel getLogReportModel(String username, String currentTime) {
//        LogReportModel logReportModel = new LogReportModel();
//        MongoDatabase db = MongoDBConnectionFactory.getDBSlave();
//        FindIterable iterable = null;
//        Document conditions = new Document();
//        conditions.put("user_name", username);
//
//        BasicDBObject obj = new BasicDBObject();
//
//        obj.put("$gte", currentTime);
//        conditions.put("time", obj);
//
//        BasicDBObject sortCondtions = new BasicDBObject();
//        sortCondtions.put("_id", -1);
//
//        iterable = db.getCollection(LogReportConfig.MONGO_COLLECTION).find(conditions).sort(sortCondtions).limit(1);
//        iterable.forEach(new Block<Document>(){
//
//            public void apply(Document document) {
//                logReportModel.id =  1;
//                logReportModel.time =  document.getString("time");
//                logReportModel.nick_name =  document.getString("nick_name");
//
//                logReportModel.wm =  document.getLong("wm");
//                logReportModel.wm_win =  document.getLong("wm_win");
//
//                logReportModel.ibc =  document.getLong("ibc");
//                logReportModel.ibc_win =  document.getLong("ibc_win");
//
//                logReportModel.ag =  document.getLong("ag");
//                logReportModel.ag_win =  document.getLong("ag_win");
//
//                logReportModel.tlmn =  document.getLong("tlmn");
//
//                logReportModel.bacay =  document.getLong("bacay");
//                logReportModel.bacay_win =  document.getLong("bacay_win");
//
//                logReportModel.xocdia =  document.getLong("xocdia");
//                logReportModel.xocdia_win =  document.getLong("xocdia_win");
//
//                logReportModel.minipoker =  document.getLong("minipoker");
//                logReportModel.minipoker_win =  document.getLong("minipoker_win");
//
//                logReportModel.slot_pokemon =  document.getLong("slot_pokemon");
//                logReportModel.slot_pokemon_win =  document.getLong("slot_pokemon_win");
//
//                logReportModel.baucua =  document.getLong("baucua");
//                logReportModel.baucua_win =  document.getLong("baucua_win");
//
//                logReportModel.taixiu =  document.getLong("taixiu");
//                logReportModel.taixiu_win =  document.getLong("taixiu_win");
//
//                logReportModel.caothap =  document.getLong("caothap");
//                logReportModel.caothap_win =  document.getLong("caothap_win");
//
//                logReportModel.slot_bitcoin =  document.getLong("slot_bitcoin");
//                logReportModel.slot_bitcoin_win =  document.getLong("slot_bitcoin_win");
//
//                logReportModel.slot_taydu =  document.getLong("slot_bitcoin");
//                logReportModel.slot_taydu_win =  document.getLong("slot_bitcoin_win");
//
//                logReportModel.slot_angrybird =  document.getLong("slot_angrybird");
//                logReportModel.slot_angrybird_win =  document.getLong("slot_angrybird_win");
//
//                logReportModel.slot_thantai =  document.getLong("slot_thantai");
//                logReportModel.slot_thantai_win =  document.getLong("slot_thantai_win");
//
//                logReportModel.slot_thethao =  document.getLong("slot_thethao");
//                logReportModel.slot_thethao_win =  document.getLong("slot_thethao_win");
//                
//                try{
//                	logReportModel.slot_chiemtinh =  document.getLong("slot_thethao");
//                }catch (Exception e) {
//                	logReportModel.slot_chiemtinh = 0;
//				}
//                
//                try{
//                	logReportModel.slot_chiemtinh_win =  document.getLong("slot_chiemtinh_win");
//                }catch (Exception e) {
//                	logReportModel.slot_chiemtinh_win = 0;
//				}
//                
//                try{
//                	logReportModel.taixiu_st =  document.getLong("taixiu_st");
//                }catch (Exception e) {
//                	logReportModel.taixiu_st = 0;
//				}
//                
//                try{
//                	logReportModel.taixiu_st_win =  document.getLong("taixiu_st_win");
//                }catch (Exception e) {
//                	logReportModel.taixiu_st_win = 0;
//				}
//                
//                try{
//                	logReportModel.fish =  document.getLong("fish");
//                }catch (Exception e) {
//                	logReportModel.fish = 0;
//				}
//                
//                try{
//                	logReportModel.fish_win =  document.getLong("fish_win");
//                }catch (Exception e) {
//                	logReportModel.fish_win = 0;
//				}
//                
//                logReportModel.deposit =  document.getLong("deposit");
//                logReportModel.withdraw =  document.getLong("withdraw");
//            }
//        });
//        return logReportModel;
//    }

//
//    public static String getTimeStampInDay() {
//        return VinPlayUtils.getCurrentDateMarketing();
////        Calendar time = Calendar.getInstance();
////        time.add(Calendar.MILLISECOND, time.getTimeZone().getOffset(time.getTimeInMillis()));
////        return time.getTimeInMillis() / (86400000);
//    }
}
