/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.GiftCodeMessage
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.response.GiftCodeByNickNameResponse
 *  com.vinplay.vbee.common.response.GiftCodeCountResponse
 *  com.vinplay.vbee.common.response.GiftCodeDeleteResponse
 *  com.vinplay.vbee.common.response.GiftCodeResponse
 *  com.vinplay.vbee.common.response.GiftCodeUpdateResponse
 *  com.vinplay.vbee.common.response.ReportGiftCodeResponse
 *  com.vinplay.vbee.common.response.giftcode.GiftcodeStatisticObj
 */
package com.vinplay.usercore.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.usercore.dao.impl.GiftCodeDAOImpl;
import com.vinplay.usercore.service.GiftCodeService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.GiftCodeMessage;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.GiftCodeByNickNameResponse;
import com.vinplay.vbee.common.response.GiftCodeCountResponse;
import com.vinplay.vbee.common.response.GiftCodeDeleteResponse;
import com.vinplay.vbee.common.response.GiftCodeResponse;
import com.vinplay.vbee.common.response.GiftCodeUpdateResponse;
import com.vinplay.vbee.common.response.ReportGiftCodeResponse;
import com.vinplay.vbee.common.response.giftcode.GiftcodeStatisticObj;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GiftCodeServiceImpl
implements GiftCodeService {
    @Override
    public boolean xuatGiftCode(GiftCodeMessage msg) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.xuatGiftCode(msg);
    }

    @Override
    public GiftCodeUpdateResponse updateGiftCode(String userName, String giftCode) throws SQLException, ParseException {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        GiftCodeUpdateResponse response = new GiftCodeUpdateResponse(false, "10001");
        if (userMap.containsKey(userName)) {
            UserCacheModel user = (UserCacheModel)userMap.get(userName);
            if (user.getMobile() != null && user.isHasMobileSecurity()) {
                if (giftCode.toUpperCase().contains("MK1")) {
                    String beforeDate = "2017-04-01 00:00:00";
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date1 = sdf.parse("2017-04-01 00:00:00");
                    Date date2 = user.getCreateTime();
                    if (date2.before(date1)) {
                        response.setErrorCode("10004");
                        return response;
                    }
                }
                GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
                response = dao.updateGiftCode(userName, giftCode);
                return response;
            }
            response.setErrorCode("10003");
        }
        return response;
    }

    @Override
    public List<GiftCodeResponse> searchAllGiftCode(String nickName, String giftcode, String price, String source, String timeStart, String timeEnd, String moneyType, String usegift, int page, int totalRecord, String type, String release, String timeType, String block) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.searchAllGiftCode(nickName, giftcode, price, source, timeStart, timeEnd, moneyType, usegift, page, totalRecord, type, release, timeType, block);
    }

    @Override
    public List<GiftCodeResponse> searchAllGiftCodeAdmin(String price, String timeStart, String timeEnd, String moneyType, String usegift, String source, int page, int totalRecord, String block) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.searchAllGiftCodeAdmin(price, timeStart, timeEnd, moneyType, usegift, source, page, totalRecord, block);
    }

    @Override
    public List<GiftCodeCountResponse> countGiftCodeByPrice(String price, String source, String timeStart, String timeEnd, String moneyType, String type, String timeType, String block) throws SQLException {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        ArrayList<GiftCodeCountResponse> res = new ArrayList<GiftCodeCountResponse>();
        GiftCodeCountResponse model = null;
        List<String> listprice = new ArrayList<String>();
        if (price.isEmpty()) {
            listprice = dao.ListAllPrice(Integer.parseInt(moneyType));
        } else {
            listprice.add(price);
        }
        for (String mp : listprice) {
            model = dao.countGiftCodeByPrice(mp, source, timeStart, timeEnd, moneyType, type, timeType, block);
            if (model == null) continue;
            res.add(model);
        }
        return res;
    }

    @Override
    public List<GiftcodeStatisticObj> thongKeGiftcodeDaXuat(String source, String timeStart, String timeEnd, String moneyType, String timeType) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.thongKeGiftcodeDaXuat(source, timeStart, timeEnd, moneyType, timeType);
    }

    @Override
    public List<GiftCodeCountResponse> countGiftCodeByPriceAdmin(String price, String timeStart, String timeEnd, String moneyType, String type, String block) throws SQLException {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        ArrayList<GiftCodeCountResponse> res = new ArrayList<GiftCodeCountResponse>();
        GiftCodeCountResponse model = null;
        List<String> listprice = new ArrayList<String>();
        if (price.isEmpty()) {
            listprice = dao.ListAllPrice(Integer.parseInt(moneyType));
        } else {
            listprice.add(price);
        }
        for (String mp : listprice) {
            model = dao.countGiftCodeByPriceAdmin(mp, timeStart, timeEnd, moneyType, type, block);
            if (model == null) continue;
            res.add(model);
        }
        return res;
    }

    @Override
    public List<ReportGiftCodeResponse> ToolReportGiftCode(String nickName, String source, String timeStart, String timeEnd, String moneyType, String timeType, String block) throws SQLException, ParseException, JsonProcessingException {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.ToolReportGiftCode(nickName, source, timeStart, timeEnd, moneyType, timeType, block);
    }

    @Override
    public boolean genGiftCode(GiftCodeMessage msg) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.genGiftCode(msg);
    }

    @Override
    public List<String> loadAllGiftcode() {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.loadAllGiftcode();
    }

    @Override
    public long countsearchAllGiftCode(String nickName, String giftCode, String price, String source, String timeStart, String timeEnd, String moneyType, String usegift, String type, String release, String timeType, String block) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        long totalRecords = dao.countsearchAllGiftCode(nickName, giftCode, price, source, timeStart, timeEnd, moneyType, usegift, type, release, timeType, block);
        return totalRecords;
    }

    @Override
    public long countsearchAllGiftCodeAdmin(String price, String source, String timeStart, String timeEnd, String moneyType, String usegift, String block) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.countsearchAllGiftCodeAdmin(price, source, timeStart, timeEnd, moneyType, usegift, block);
    }

    @Override
    public List<ReportGiftCodeResponse> ToolReportGiftCodeBySource(String source, String timeStart, String timeEnd, String moneyType, String type, int page, String timeType, String block) throws SQLException, ParseException, JsonProcessingException {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.ToolReportGiftCodeBySource(source, timeStart, timeEnd, moneyType, type, page, timeType, block);
    }

    @Override
    public String uploadFileGiftCode(String lstNickName, long vin, long xu) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.uploadFileGiftCode(lstNickName, vin, xu);
    }

    @Override
    public boolean RestoreGiftCode(String price, String source, String giftcode, String release) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.RestoreGiftCode(price, source, giftcode, release);
    }

    @Override
    public List<GiftCodeResponse> searchAllGiftCodeByNickName(String nickName, int page) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.searchAllGiftCodeByNickName(nickName, page);
    }

    @Override
    public long countAllGiftCodeByNickName(String nickName) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.countAllGiftCodeByNickName(nickName);
    }

    @Override
    public GiftCodeByNickNameResponse getUserInfoByGiftCode(String giftCode, IMap<String, UserCacheModel> userMap, int page, int totalRecord) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.getUserInfoByGiftCode(giftCode, userMap, page, totalRecord);
    }

    @Override
    public GiftCodeDeleteResponse DeleteGiftCode(String startDate, String endDate, String source, String price) {
        GiftCodeDAOImpl dao = new GiftCodeDAOImpl();
        return dao.DeleteGiftCode(startDate, endDate, source, price);
    }
}

