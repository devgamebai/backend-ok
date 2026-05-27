package com.vinplay.vbee.rmq.loguserinday;

import com.vinplay.dal.entities.report.LogCountUserPlay;
import com.vinplay.vbee.common.pools.ConnectionPool;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

public class LogCountReportUtil {

    private static final Logger logger = Logger.getLogger((String)"vbee");

    public static LogCountUserPlay getLogReportModelSQL(String username, String currentTime) {
        LogCountUserPlay logCountUserPlay = new LogCountUserPlay();

        String sql = "SELECT * FROM vinplay.log_count_user_play WHERE nick_name=? AND time_report=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setString(param++, username);
            stm.setString(param++, currentTime);

            ResultSet rs =stm.executeQuery();
            if (rs.next()) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                logCountUserPlay.id = rs.getInt("id");
                logCountUserPlay.nick_name = rs.getString("nick_name");
                logCountUserPlay.time = df.format(rs.getDate("time_report"));

                logCountUserPlay.wm = rs.getInt("wm");

                logCountUserPlay.ibc = rs.getInt("ibc");

                logCountUserPlay.ag = rs.getInt("ag");

                logCountUserPlay.tlmn = rs.getInt("tlmn");

                logCountUserPlay.bacay = rs.getInt("bacay");

                logCountUserPlay.xocdia = rs.getInt("xocdia");

                logCountUserPlay.minipoker = rs.getInt("minipoker");

                logCountUserPlay.slot_pokemon = rs.getInt("slot_pokemon");

                logCountUserPlay.baucua = rs.getInt("baucua");

                logCountUserPlay.taixiu = rs.getInt("taixiu");

                logCountUserPlay.caothap = rs.getInt("caothap");

                logCountUserPlay.slot_bitcoin = rs.getInt("slot_bitcoin");

                logCountUserPlay.slot_taydu = rs.getInt("slot_taydu");

                logCountUserPlay.slot_angrybird = rs.getInt("slot_angrybird");

                logCountUserPlay.slot_thantai = rs.getInt("slot_thantai");

                logCountUserPlay.slot_thethao = rs.getInt("slot_thethao");

                logCountUserPlay.deposit = rs.getInt("deposit");
                logCountUserPlay.withdraw = rs.getInt("withdraw");
                
                logCountUserPlay.cmd = rs.getInt("cmd");
                logCountUserPlay.slot_chiemtinh=rs.getInt("slot_chiemtinh");
                logCountUserPlay.taixiu_st=rs.getInt("taixiu_st");
                logCountUserPlay.fish=rs.getInt("fish");
                
                logCountUserPlay.slot_thanbai=rs.getInt("slot_thanbai");
                logCountUserPlay.ebet=rs.getInt("ebet");
                logCountUserPlay.sbo=rs.getInt("sbo");

                logCountUserPlay.slot_bikini = rs.getInt("slot_bikini");
                logCountUserPlay.slot_galaxy = rs.getInt("slot_galaxy");

                logCountUserPlay.sam = rs.getInt("sam");
                logCountUserPlay.binh = rs.getInt("binh");
                logCountUserPlay.tala = rs.getInt("tala");
                logCountUserPlay.lieng = rs.getInt("lieng");
                logCountUserPlay.xito = rs.getInt("xito");
                logCountUserPlay.baicao = rs.getInt("baicao");
                logCountUserPlay.poker = rs.getInt("poker");
                logCountUserPlay.xidzach = rs.getInt("xidzach");
                logCountUserPlay.hamcamap = rs.getInt("hamcamap");
                logCountUserPlay.taixiu_sicbo = rs.getInt("taixiu_sicbo");
                logCountUserPlay.over_under = rs.getInt("over_under");
                logCountUserPlay.samtruyen = rs.getInt("samtruyen");
                logCountUserPlay.range_rover = rs.getInt("range_rover");
                logCountUserPlay.sexygirl = rs.getInt("sexygirl");
                logCountUserPlay.lode = rs.getInt("lode");
            }
        }catch (Exception e){
            logger.info(e.getMessage());
        }
        return logCountUserPlay;
    }

    public static boolean insertNewLogSQL(LogCountUserPlay logReportModel){
        boolean value =false;
        String sql = "INSERT INTO vinplay.log_count_user_play(time_report,nick_name,wm,ibc,ag," +
                "tlmn,bacay,xocdia,minipoker,slot_pokemon," +
                "baucua,taixiu,caothap,slot_bitcoin,slot_taydu," +
                "slot_angrybird,slot_thantai,slot_thethao,deposit,withdraw,cmd,slot_chiemtinh,taixiu_st,fish,"
                + "slot_thanbai,ebet,sbo, slot_bikini, slot_galaxy, attendance,"
                + "sam,binh,tala,lieng,xito,baicao,poker,xidzach,"
                + "hamcamap,taixiu_sicbo,over_under,samtruyen,range_rover,sexygirl,lode"
                + ")  VALUES (" +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?,"+ 
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?,?,?,?," +
                "?,?,?,?,?,?,?)";

        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")){
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setDate(param++,  java.sql.Date.valueOf(logReportModel.time));
            stm.setString(param++, logReportModel.nick_name);

            stm.setInt(param++, logReportModel.wm);

            stm.setInt(param++, logReportModel.ibc);

            stm.setInt(param++, logReportModel.ag);

            stm.setInt(param++, logReportModel.tlmn);

            stm.setInt(param++, logReportModel.bacay);

            stm.setInt(param++, logReportModel.xocdia);

            stm.setInt(param++, logReportModel.minipoker);

            stm.setInt(param++, logReportModel.slot_pokemon);

            stm.setInt(param++, logReportModel.baucua);

            stm.setInt(param++, logReportModel.taixiu);

            stm.setInt(param++, logReportModel.caothap);

            stm.setInt(param++, logReportModel.slot_bitcoin);

            stm.setInt(param++, logReportModel.slot_taydu);

            stm.setInt(param++, logReportModel.slot_angrybird);

            stm.setInt(param++, logReportModel.slot_thantai);

            stm.setInt(param++, logReportModel.slot_thethao);

            stm.setInt(param++, logReportModel.deposit);
            stm.setInt(param++, logReportModel.withdraw);
            stm.setInt(param++, logReportModel.cmd);
            stm.setInt(param++, logReportModel.slot_chiemtinh);
            stm.setInt(param++, logReportModel.taixiu_st);
            stm.setInt(param++, logReportModel.fish);
            
            stm.setInt(param++, logReportModel.slot_thanbai);
            stm.setInt(param++, logReportModel.ebet);
            stm.setInt(param++, logReportModel.sbo);
            stm.setInt(param++, logReportModel.slot_bikini);
            stm.setInt(param++, logReportModel.slot_galaxy);
            stm.setInt(param++, logReportModel.attendance);

            stm.setInt(param++, logReportModel.sam);
            stm.setInt(param++, logReportModel.binh);
            stm.setInt(param++, logReportModel.tala);
            stm.setInt(param++, logReportModel.lieng);
            stm.setInt(param++, logReportModel.xito);
            stm.setInt(param++, logReportModel.baicao);
            stm.setInt(param++, logReportModel.poker);
            stm.setInt(param++, logReportModel.xidzach);
            stm.setInt(param++, logReportModel.hamcamap);
            stm.setInt(param++, logReportModel.taixiu_sicbo);
            stm.setInt(param++, logReportModel.over_under);
            stm.setInt(param++, logReportModel.samtruyen);
            stm.setInt(param++, logReportModel.range_rover);
            stm.setInt(param++, logReportModel.sexygirl);
            stm.setInt(param++, logReportModel.lode);

            value = stm.executeUpdate() == 1;
        }catch (Exception e){
            logger.info(e.getMessage());
        }
        return value;
    }

    public static boolean updateLogSQL(LogCountUserPlay logReportModel){
        // logger.info("insert 0");
        boolean value = false;
        String sql = "UPDATE vinplay.log_count_user_play SET wm=?, ibc=?, ag=?,tlmn=?,bacay=?," +
                "xocdia=?, minipoker=?, slot_pokemon=?, baucua=?," +
                "taixiu=?, caothap=?, slot_bitcoin=?, slot_taydu=?," +
                "slot_angrybird=?, slot_thantai=?,slot_thethao=?," +
                "deposit=?, withdraw=? ,cmd=? , slot_chiemtinh=?," +
                "taixiu_st=?,fish=?,slot_thanbai=?,ebet=?,sbo=?, " +
                "slot_bikini=?, slot_galaxy=?, attendance=?," +
                "sam=?,binh=?,tala=?,lieng=?,xito=?,baicao=?,poker=?,xidzach=?," +
                "hamcamap=?,taixiu_sicbo=?,over_under=?,samtruyen=?,range_rover=?,sexygirl=?,lode=? " +
                "WHERE nick_name=? AND time_report=?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname")) {
            PreparedStatement stm = conn.prepareStatement(sql);
            int param = 1;
            stm.setInt(param++, logReportModel.wm);

            stm.setInt(param++, logReportModel.ibc);

            stm.setInt(param++, logReportModel.ag);

            stm.setInt(param++, logReportModel.tlmn);

            stm.setInt(param++, logReportModel.bacay);

            stm.setInt(param++, logReportModel.xocdia);

            stm.setInt(param++, logReportModel.minipoker);

            stm.setInt(param++, logReportModel.slot_pokemon);

            stm.setInt(param++, logReportModel.baucua);

            stm.setInt(param++, logReportModel.taixiu);

            stm.setInt(param++, logReportModel.caothap);

            stm.setInt(param++, logReportModel.slot_bitcoin);

            stm.setInt(param++, logReportModel.slot_taydu);

            stm.setInt(param++, logReportModel.slot_angrybird);

            stm.setInt(param++, logReportModel.slot_thantai);

            stm.setInt(param++, logReportModel.slot_thethao);

            stm.setInt(param++, logReportModel.deposit);
            stm.setInt(param++, logReportModel.withdraw);
            
            stm.setInt(param++, logReportModel.cmd);
            stm.setInt(param++, logReportModel.slot_chiemtinh);
            stm.setInt(param++, logReportModel.taixiu_st);
            stm.setInt(param++, logReportModel.fish);
            
            stm.setInt(param++, logReportModel.slot_thanbai);
            stm.setInt(param++, logReportModel.ebet);
            stm.setInt(param++, logReportModel.sbo);

            stm.setInt(param++, logReportModel.slot_bikini);
            stm.setInt(param++, logReportModel.slot_galaxy);
            
            stm.setInt(param++, logReportModel.attendance);

            stm.setInt(param++, logReportModel.sam);
            stm.setInt(param++, logReportModel.binh);
            stm.setInt(param++, logReportModel.tala);
            stm.setInt(param++, logReportModel.lieng);
            stm.setInt(param++, logReportModel.xito);
            stm.setInt(param++, logReportModel.baicao);
            stm.setInt(param++, logReportModel.poker);
            stm.setInt(param++, logReportModel.xidzach);
            stm.setInt(param++, logReportModel.hamcamap);
            stm.setInt(param++, logReportModel.taixiu_sicbo);
            stm.setInt(param++, logReportModel.over_under);
            stm.setInt(param++, logReportModel.samtruyen);
            stm.setInt(param++, logReportModel.range_rover);
            stm.setInt(param++, logReportModel.sexygirl);
            stm.setInt(param++, logReportModel.lode);

            stm.setString(param++, logReportModel.nick_name);
            stm.setDate(param++,  java.sql.Date.valueOf(logReportModel.time));
            value = stm.executeUpdate() == 1;
        }catch (Exception e){
            logger.info(e.getMessage());
        }
        return value;
    }
}
