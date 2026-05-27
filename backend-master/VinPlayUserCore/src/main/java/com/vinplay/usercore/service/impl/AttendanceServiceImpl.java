/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  org.apache.commons.lang3.ArrayUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.commons.lang3.time.DateUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.AttendanceConfigDao;
import com.vinplay.usercore.dao.UserAttendanceDao;
import com.vinplay.usercore.dao.impl.AttendanceConfigDaoImpl;
import com.vinplay.usercore.dao.impl.UserAttendanceDaoImpl;
import com.vinplay.usercore.entities.AttendanceConfig;
import com.vinplay.usercore.entities.UserAttendance;
import com.vinplay.usercore.service.AttendanceService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.response.BaseResponseModel;
import com.vinplay.vbee.common.rmq.RMQPublishTask;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

public class AttendanceServiceImpl
implements AttendanceService {
    private static final Logger logger = Logger.getLogger((String)"user_core");
    private UserAttendanceDao dao = new UserAttendanceDaoImpl();
    private AttendanceConfigDao daoConf = new AttendanceConfigDaoImpl();
    private static final SplittableRandom rdom = new SplittableRandom();
    private static final short[] SO_BIG_VALUE = new short[]{4, 5, 7, 8, 10, 13, 14, 16, 17, 11, 3, 6, 9, 12, 15, 18};
    private static final short[] BIG_VALUE = new short[]{4, 5, 7, 8, 10, 13, 14, 16, 17, 11, 3, 6, 9, 12, 5, 7, 8, 10, 13};
    private static final short[] BIG_BIG_VALUE = new short[]{4, 5, 7, 8, 10, 13, 14, 16, 17, 11, 15, 18, 3, 6, 9, 12, 5, 7, 8, 10, 13};
    private static final short[] MIN_VALUE = new short[]{4, 5, 7, 8, 10, 4, 5, 7, 8};
    private static final short[] MEDIUM_VALUE = new short[]{4, 5, 7, 8, 10, 13, 14, 16, 17, 11, 8, 10, 13};

    private String ValidateConfig(AttendanceConfig attendanceConfig) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            if (StringUtils.isBlank((CharSequence)attendanceConfig.getStart_date())) {
                return "Ng\u00e0y b\u1eaft \u0111\u1ea7u c\u1ee7a chu k\u1ef3 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng";
            }
            if (StringUtils.isBlank((CharSequence)attendanceConfig.getEnd_date())) {
                return "Ng\u00e0y k\u1ebft th\u00fac c\u1ee7a chu k\u1ef3 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1eafng";
            }
            if (df.parse(attendanceConfig.getEnd_date()).getTime() < df.parse(attendanceConfig.getStart_date()).getTime()) {
                return "Ng\u00e0y b\u1eaft \u0111\u1ea7u ph\u1ea3i nh\u1ecf h\u01a1n ng\u00e0y k\u1ebft th\u00fac";
            }
            if (attendanceConfig.getMoney() < 1L) {
                return "S\u1ed1 ti\u1ec1n ph\u1ea3i l\u1edbn h\u01a1n 0";
            }
            AttendanceConfig configLastest = this.getConfigLastestFromCached();
            AttendanceConfig attendanceConfig2 = configLastest = configLastest == null ? this.getConfigLastest() : configLastest;
            if (configLastest != null && df.parse(configLastest.getEnd_date()).getTime() > df.parse(attendanceConfig.getStart_date()).getTime()) {
                return "Ng\u00e0y b\u1eaft \u0111\u1ea7u ph\u1ea3i nh\u1ecf h\u01a1n ng\u00e0y k\u1ebft th\u00fac c\u1ee7a chu k\u1ef3 tr\u01b0\u1edbc";
            }
            return "success";
        }
        catch (Exception e) {
            logger.error(("Error ValidateConfig: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public boolean createConfig(long money) {
        try {
            AttendanceConfig attendanceConfig = new AttendanceConfig();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date start = DateUtils.truncate((Date)new Date(), (int)5);
            Date end = DateUtils.truncate((Date)new Date(), (int)5);
            end.setTime(end.getTime() + 518400000L);
            attendanceConfig.setStart_date(df.format(start) + " 00:00:00");
            attendanceConfig.setEnd_date(df.format(end) + " 23:59:59");
            attendanceConfig.setMoney(money);
            attendanceConfig.setCreate_at(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
            return this.createConfig(attendanceConfig).equalsIgnoreCase("success");
        }
        catch (Exception e) {
            logger.error(("Error createConfig: " + e.getMessage()));
            return false;
        }
    }

    @Override
    public String createConfig(AttendanceConfig attendanceConfig) {
        try {
            String result = "";
            result = this.ValidateConfig(attendanceConfig);
            if (!"success".equals(result)) {
                return result;
            }
            String rs = this.daoConf.insert(attendanceConfig);
            this.setAttendanceConfigToCached();
            return rs;
        }
        catch (Exception e) {
            logger.error(("Error createConfig: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public AttendanceConfig getConfigLastest() {
        try {
            return this.daoConf.getLastest();
        }
        catch (Exception e) {
            logger.error(("GETCONFIG Attendance lastest from DB: " + e.getMessage()));
            return null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean setAttendanceConfigToCached() {
        HazelcastInstance instance = HazelcastClientFactory.getInstance();
        if (instance == null) {
            logger.error("[SETCACHECONFIG Attendance] Can't connect cached server");
            return false;
        }
        IMap configCache = instance.getMap("cacheConfig");
        if (configCache.containsKey("ATTENDANCE_CONFIG")) {
            try {
                configCache.lock("ATTENDANCE_CONFIG");
                AttendanceConfig attendanceConfig = this.getConfigLastest();
                configCache.put("ATTENDANCE_CONFIG", (attendanceConfig == null ? "" : attendanceConfig.toJson()));
                boolean bl = true;
                return bl;
            }
            catch (Exception e) {
                logger.error(("[SETCACHECONFIG Attendance] Exception: " + e.getMessage()));
                boolean bl = false;
                return bl;
            }
            finally {
                if (configCache.isLocked("ATTENDANCE_CONFIG")) {
                    try {
                        configCache.unlock("ATTENDANCE_CONFIG");
                    }
                    catch (Exception e) {
                        logger.error(("[SETCACHECONFIG Attendance]: " + e.getMessage()));
                    }
                }
            }
        }
        logger.error("[SETCACHECONFIG Attendance] Can't found key ATTENDANCE_CONFIG into cached server");
        return false;
    }

    @Override
    public AttendanceConfig getConfigLastestFromCached() {
        try {
            HazelcastInstance instance = HazelcastClientFactory.getInstance();
            IMap configCache = instance.getMap("cacheConfig");
            String value = ((String)configCache.get("ATTENDANCE_CONFIG")).toString();
            Type type = new TypeToken<AttendanceConfig>(){}.getType();
            AttendanceConfig attendanceConfig = (AttendanceConfig)new Gson().fromJson(value, type);
            if (attendanceConfig == null) {
                attendanceConfig = this.getConfigLastest();
            }
            return attendanceConfig;
        }
        catch (Exception e) {
            logger.error(("[GETCONFIG Attendance lastest from cache server] Exception: " + e.getMessage()));
            AttendanceConfig attendanceConfig = new AttendanceConfig();
            attendanceConfig = this.getConfigLastest();
            return attendanceConfig;
        }
    }

    public BaseResponseModel addMoneyDiemDanh(UserAttendance userAttendance, long money) {
        UserServiceImpl userService = new UserServiceImpl();
        BaseResponseModel baseResponseModel = userService.updateMoneyFromAdmin(userAttendance.getNick_name(), money, "vin", Games.DIEM_DANH.getName(), Games.DIEM_DANH.getId() + "", userAttendance.toJson());
        return baseResponseModel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Map<String, Object> Attendance(String nickname, String ip) {
        HashMap<String, Object> result = new HashMap<String, Object>();
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        if (client == null) {
            result.put("code", "failed");
            result.put("msg", "L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
            return result;
        }
        IMap userMap = client.getMap("users");
        try {
            AttendanceConfig config = this.getConfigLastestFromCached();
            AttendanceConfig attendanceConfig = config = config == null ? this.getConfigLastest() : config;
            if (config.getId() == 0) {
                config = this.getConfigLastest();
            }
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            UserAttendance userAttendance = new UserAttendance();
            UserAttendance userAttendanceLastest = this.getAttendLastest(nickname);
            Date today = DateUtils.truncate((Date)new Date(), (int)5);
            if (today.getTime() < df.parse(config.getStart_date()).getTime() || today.getTime() > df.parse(config.getEnd_date()).getTime()) {
                result.put("code", "invalid");
                result.put("msg", "Ng\u00e0y \u0111i\u1ec3m danh kh\u00f4ng n\u1eb1m trong chu k\u1ef3 \u0111i\u1ec3m danh");
                HashMap<String, Object> hashMap = result;
                return hashMap;
            }
            if (userMap.containsKey(nickname)) {
                block43: {
                    if (!userMap.isLocked(nickname)) break block43;
                    result.put("code", "failed");
                    result.put("msg", "B\u1ea1n ch\u1ec9 \u0111\u01b0\u1ee3c quay nh\u1eadn th\u01b0\u1edfng m\u1ed9t l\u1ea7n trong ng\u00e0y.");
                    HashMap<String, Object> hashMap = result;
                    return hashMap;
                }
                try {
                    userMap.lock(nickname);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (userAttendanceLastest == null) {
                userAttendance.setAttend_id(config.getId());
                userAttendance.setConsecutive(1);
                userAttendance.setNick_name(nickname);
                userAttendance.setDate_attend(df.format(new Date()));
                short[] diceResult = this.getDice(nickname, ip);
                long[] moneys = this.CalculatorMoney(diceResult, config, userAttendance.getConsecutive());
                userAttendance.setBonus_basic(moneys[0]);
                userAttendance.setBonus_consecutive(moneys[1]);
                userAttendance.setBonus_vip(moneys[2]);
                userAttendance.setSpin(StringUtils.join((Object[])ArrayUtils.toObject((short[])diceResult), (String)"-"));
                userAttendance.setResult(userAttendance.getSpin());
                userAttendance.setIp(ip);
            } else {
                Date lastDate = DateUtils.truncate((Date)df.parse(userAttendanceLastest.getDate_attend()), (int)5);
                if (DateUtils.isSameDay((Date)lastDate, (Date)today)) {
                    result.put("code", "exist");
                    result.put("msg", "B\u1ea1n \u0111\u00e3 nh\u1eadn qu\u00e0 \u0111i\u1ec3m danh v\u00e0o l\u00fac: " + userAttendanceLastest.getDate_attend());
                    HashMap<String, Object> moneys = result;
                    return moneys;
                }
                if (DateUtils.isSameDay((Date)lastDate, (Date)DateUtils.addDays((Date)today, (int)-1))) {
                    if (userAttendanceLastest.getAttend_id() == config.getId()) {
                        userAttendance.setConsecutive(userAttendanceLastest.getConsecutive() + 1);
                    } else {
                        userAttendance.setConsecutive(1);
                    }
                } else {
                    userAttendance.setConsecutive(1);
                }
                userAttendance.setNick_name(nickname);
                userAttendance.setDate_attend(df.format(new Date()));
                short[] diceResult = this.getDice(nickname, ip);
                long[] moneys = this.CalculatorMoney(diceResult, config, userAttendance.getConsecutive());
                userAttendance.setBonus_basic(moneys[0]);
                userAttendance.setBonus_consecutive(moneys[1]);
                userAttendance.setBonus_vip(moneys[2]);
                userAttendance.setSpin(StringUtils.join((Object[])ArrayUtils.toObject((short[])diceResult), (String)"-"));
                userAttendance.setResult(userAttendance.getSpin());
                userAttendance.setAttend_id(config.getId());
                userAttendance.setIp(ip);
            }
            String rs = this.dao.insert(userAttendance);
            if ("success".equals(rs)) {
                long money = userAttendance.getBonus_basic() + userAttendance.getBonus_consecutive() + userAttendance.getBonus_vip();
                BaseResponseModel baseResponseModel = this.addMoneyDiemDanh(userAttendance, money);
                if (baseResponseModel.isSuccess()) {
                    LogMoneyUserMessage message = new LogMoneyUserMessage(0, userAttendance.getNick_name(), "DIEM_DANH", Games.DIEM_DANH.getId() + "", 0L, money, "vin", "", 0L, false, false);
                    RMQPublishTask taskReportUser = new RMQPublishTask(message, "queue_log_report_user_balance", 602);
                    taskReportUser.start();
                    result.put("code", "success");
                    result.put("msg", userAttendance.toJson());
                    HashMap<String, Object> hashMap = result;
                    return hashMap;
                }
                userAttendance = this.getAttendLastest(nickname);
                this.dao.delete(userAttendance.getAttend_id());
                result.put("code", "failed");
                result.put("msg", "L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
                HashMap<String, Object> hashMap = result;
                return hashMap;
            }
            result.put("code", "failed");
            result.put("msg", "L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
            HashMap<String, Object> hashMap = result;
            return hashMap;
        }
        catch (Exception e) {
            logger.error(("Error insertUserAttend: " + e.getMessage()));
            result.put("code", "exception");
            result.put("msg", "L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
            HashMap<String, Object> hashMap = result;
            return hashMap;
        }
        finally {
            if (userMap.isLocked(nickname)) {
                try {
                    userMap.unlock(nickname);
                }
                catch (Exception e) {
                    logger.error(("Error insertUserAttend: " + e.getMessage()));
                }
            }
        }
    }

    @Override
    public Map<String, Object> CheckAttendance(String nickname, String ip) {
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            AttendanceConfig config = this.getConfigLastestFromCached();
            config = config == null ? this.getConfigLastest() : config;
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            UserAttendance userAttendanceLastest = this.getAttendLastest(nickname);
            Date today = DateUtils.truncate((Date)new Date(), (int)5);
            if (today.getTime() < df.parse(config.getStart_date()).getTime() || today.getTime() > df.parse(config.getEnd_date()).getTime()) {
                result.put("code", "invalid");
                result.put("consecutive", -1);
                result.put("msg", "Ng\u00e0y \u0111i\u1ec3m danh kh\u00f4ng n\u1eb1m trong chu k\u1ef3 \u0111i\u1ec3m danh");
                return result;
            }
            if (userAttendanceLastest == null) {
                result.put("code", "success");
                result.put("consecutive", 0);
                result.put("msg", "B\u1ea1n ch\u01b0a \u0111i\u1ec3m danh ng\u00e0y h\u00f4m nay");
                return result;
            }
            Date lastDate = DateUtils.truncate((Date)df.parse(userAttendanceLastest.getDate_attend()), (int)5);
            if (DateUtils.isSameDay((Date)lastDate, (Date)today)) {
                result.put("code", "exist");
                result.put("consecutive", userAttendanceLastest.getConsecutive());
                result.put("msg", "B\u1ea1n \u0111\u00e3 nh\u1eadn qu\u00e0 \u0111i\u1ec3m danh v\u00e0o l\u00fac: " + userAttendanceLastest.getDate_attend());
                return result;
            }
            result.put("code", "success");
            result.put("consecutive", userAttendanceLastest.getConsecutive());
            result.put("msg", "B\u1ea1n ch\u01b0a \u0111i\u1ec3m danh ng\u00e0y h\u00f4m nay");
            return result;
        }
        catch (Exception e) {
            logger.error(("Error insertUserAttend: " + e.getMessage()));
            result.put("code", "exception");
            result.put("consecutive", -1);
            result.put("msg", "L\u1ed7i h\u1ec7 th\u1ed1ng. Vui l\u00f2ng li\u00ean h\u1ec7 b\u1ed9 ph\u1eadn CSKH \u0111\u1ec3 \u0111\u01b0\u1ee3c gi\u00fap \u0111\u1ee1.");
            return result;
        }
    }

    private static short[] genarateRandomNum(int totalNum) {
        int t1 = totalNum / 3;
        int t2 = 0;
        t2 = totalNum - t1 <= 6 ? rdom.nextInt(totalNum - t1 - 1 - 1 + 1) + 1 : rdom.nextInt(5) + 1;
        int t3 = totalNum - t1 - t2;
        return new short[]{(short)t1, (short)t2, (short)t3};
    }

    private short[] getDice(String nickName, String ip) {
        if (this.checkIp(nickName, ip)) {
            return new short[]{0, 0, 0};
        }
        UserServiceImpl userService = new UserServiceImpl();
        long value = userService.getUserValue(nickName);
        if (value <= 10000000L) {
            int index = rdom.nextInt(MIN_VALUE.length);
            return AttendanceServiceImpl.genarateRandomNum(MIN_VALUE[index]);
        }
        if (value <= 100000000L) {
            int index = rdom.nextInt(MEDIUM_VALUE.length);
            return AttendanceServiceImpl.genarateRandomNum(MEDIUM_VALUE[index]);
        }
        if (value <= 500000000L) {
            int index = rdom.nextInt(BIG_VALUE.length);
            return AttendanceServiceImpl.genarateRandomNum(BIG_VALUE[index]);
        }
        if (value <= 1000000000L) {
            int index = rdom.nextInt(BIG_BIG_VALUE.length);
            return AttendanceServiceImpl.genarateRandomNum(BIG_BIG_VALUE[index]);
        }
        int index = rdom.nextInt(SO_BIG_VALUE.length);
        return AttendanceServiceImpl.genarateRandomNum(SO_BIG_VALUE[index]);
    }

    public static void main(String[] args) {
        int index = rdom.nextInt(MIN_VALUE.length);
        System.out.println(Arrays.toString(AttendanceServiceImpl.genarateRandomNum(MIN_VALUE[index])));
    }

    @Override
    public UserAttendance getAttendLastest(String nickname) {
        try {
            return this.dao.getLastest(nickname);
        }
        catch (Exception e) {
            logger.error(("Error getUserAttendLastest: " + e.getMessage()));
            return null;
        }
    }

    @Override
    public Map<String, Object> search(Integer attendId, String nickname, String fromTime, String endTime, int pageIndex, int limit) {
        try {
            return this.dao.search4BO(attendId, nickname, fromTime, endTime, pageIndex, limit);
        }
        catch (Exception e) {
            logger.error(("Error getUserAttendLastest: " + e.getMessage()));
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("userAttendances", new ArrayList());
            data.put("totalRecord", 0);
            data.put("totalMoney", 0);
            data.put("totalPlayer", 0);
            return data;
        }
    }

    @Override
    public List<Map<String, Object>> historyInRound(String nickname) {
        try {
            ArrayList<Map<String, Object>> rs = new ArrayList<Map<String, Object>>();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            AttendanceConfig attendanceConfig = this.getConfigLastestFromCached();
            Date createDate = format.parse(attendanceConfig.getCreate_at());
            Date endDate = format.parse(attendanceConfig.getEnd_date());
            int offset = 1;
            while (createDate.getTime() <= endDate.getTime()) {
                UserAttendance userAttendance = new UserAttendance();
                userAttendance = this.dao.getDetail(nickname, attendanceConfig.getId(), format.format(createDate));
                HashMap<String, Object> detail = new HashMap<String, Object>();
                detail.put("date", format.format(createDate));
                if (userAttendance == null) {
                    detail.put("ratioBonus", 0);
                    detail.put("consecutive", 0);
                } else {
                    detail.put("ratioBonus", (userAttendance.getConsecutive() - 1) * 10);
                    detail.put("consecutive", userAttendance.getConsecutive());
                }
                detail.put("offsetDay", String.valueOf(offset));
                ++offset;
                createDate.setTime(createDate.getTime() + 86400000L);
                rs.add(detail);
            }
            return rs;
        }
        catch (Exception e) {
            logger.error(("Error getUserAttendLastest: " + e.getMessage()));
            return null;
        }
    }

    private long[] CalculatorMoney(short[] diceResult, AttendanceConfig attendanceConfig, int consecutive) {
        int index = 0;
        for (int i = 1; i < 7; ++i) {
            short[] diceSpec = new short[]{(short)i, (short)i, (short)i};
            if (!Arrays.equals(diceResult, diceSpec)) continue;
            index = i;
            break;
        }
        switch (index) {
            case 1: {
                return new long[]{40000L, 0L, 0L};
            }
            case 2: {
                return new long[]{70000L, 0L, 0L};
            }
            case 3: {
                return new long[]{100000L, 0L, 0L};
            }
            case 4: {
                return new long[]{130000L, 0L, 0L};
            }
            case 5: {
                return new long[]{160000L, 0L, 0L};
            }
            case 6: {
                return new long[]{200000L, 0L, 0L};
            }
        }
        long bonus_consecutive = 0L;
        int totalDice = 0;
        for (short s : diceResult) {
            totalDice += Integer.valueOf(s).intValue();
        }
        long bonus_basic = attendanceConfig.getMoney() * (long)totalDice;
        bonus_consecutive = (long)((double)bonus_basic * ((double)(consecutive - 1) * 0.1));
        return new long[]{bonus_basic, bonus_consecutive, 0L};
    }

    @Override
    public boolean checkIp(String nickName, String ip) {
        return this.daoConf.isCheckSameIP();
    }
}

