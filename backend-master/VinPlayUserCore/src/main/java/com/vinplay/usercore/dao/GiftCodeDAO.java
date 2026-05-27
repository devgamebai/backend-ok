/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.hazelcast.core.IMap
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
package com.vinplay.usercore.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.core.IMap;
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
import java.util.List;

public interface GiftCodeDAO {
    public boolean xuatGiftCode(GiftCodeMessage var1);

    public GiftCodeUpdateResponse updateGiftCode(String var1, String var2) throws SQLException;

    public List<GiftCodeResponse> searchAllGiftCode(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, int var9, int var10, String var11, String var12, String var13, String var14);

    public List<GiftCodeResponse> searchAllGiftCodeAdmin(String var1, String var2, String var3, String var4, String var5, String var6, int var7, int var8, String var9);

    public GiftCodeCountResponse countGiftCodeByPrice(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8);

    public List<ReportGiftCodeResponse> ToolReportGiftCode(String var1, String var2, String var3, String var4, String var5, String var6, String var7) throws SQLException, ParseException, JsonProcessingException;

    public List<String> ListAllPrice(int var1) throws SQLException;

    public boolean genGiftCode(GiftCodeMessage var1);

    public GiftCodeCountResponse countGiftCodeByPriceAdmin(String var1, String var2, String var3, String var4, String var5, String var6);

    public long countsearchAllGiftCode(String var1, String var2, String var3, String var4, String var5, String var6, String var7, String var8, String var9, String var10, String var11, String var12);

    public long countsearchAllGiftCodeAdmin(String var1, String var2, String var3, String var4, String var5, String var6, String var7);

    public List<ReportGiftCodeResponse> ToolReportGiftCodeBySource(String var1, String var2, String var3, String var4, String var5, int var6, String var7, String var8) throws SQLException, ParseException, JsonProcessingException;

    public String uploadFileGiftCode(String var1, long var2, long var4);

    public boolean RestoreGiftCode(String var1, String var2, String var3, String var4);

    public List<GiftCodeResponse> searchAllGiftCodeByNickName(String var1, int var2);

    public long countAllGiftCodeByNickName(String var1);

    public GiftCodeByNickNameResponse getUserInfoByGiftCode(String var1, IMap<String, UserCacheModel> var2, int var3, int var4);

    public GiftCodeDeleteResponse DeleteGiftCode(String var1, String var2, String var3, String var4);

    public List<String> loadAllGiftcode();

    public List<GiftcodeStatisticObj> thongKeGiftcodeDaXuat(String var1, String var2, String var3, String var4, String var5);
}

