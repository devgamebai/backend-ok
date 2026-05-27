/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  javax.servlet.http.HttpServletRequest
 *  org.json.JSONObject
 */
package com.payment.provider.thesieutoc;

import com.payment.config.PaymentConfigLoad;
import com.payment.config.TheSieuTocConfig;
import com.payment.core.common.HttpUtils;
import com.payment.core.common.StringUtil;
import com.payment.core.hook.Param;
import com.payment.entities.TopUpEntity;
import com.payment.model.Code;
import com.payment.provider.BaseProvider;
import com.payment.provider.oneVnPay.BaseResponse;
import com.payment.provider.thesieutoc.Status;
import com.payment.response.CardInResult;
import com.payment.response.HookCardInResult;
import com.vinplay.vbee.common.models.UserModel;
import java.util.Arrays;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

public class TheSieuTocProvider
extends BaseProvider {
    @Override
    public String name() {
        return "TheSieuToc";
    }

    private boolean isValidType(String type) {
        return Arrays.asList("Viettel", "Vinaphone", "Mobifone", "Zing", "Vietnamobile", "Vcoin", "Gate", "Garena").contains(type);
    }

    @Override
    public CardInResult CardIn(UserModel userInfo, String code, String serial, String type, int amount) throws Exception {
        String requestId = StringUtil.timestampToText() + StringUtil.digitalText(5);
        CardInResult cardInResult = new CardInResult(Code.ERROR);
        TheSieuTocConfig theSieuTocConfig = PaymentConfigLoad.getTheSieuTocConfig();
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("APIkey", theSieuTocConfig.getAPIKey());
        data.put("mathe", code);
        data.put("seri", serial);
        data.put("type", type);
        data.put("menhgia", String.valueOf(amount));
        data.put("content", requestId);
        String json = HttpUtils.postFormData("https://thesieutoc.net/chargingws/v2", data);
        JSONObject obj = new JSONObject(json);
        String statusStr = obj.getString("status");
        if (obj.has("status")) {
            Status status = Status.findByStatus(obj.getString("status"));
            switch (status) {
                case THE_CHO_XU_LY: {
                    cardInResult.setCode(Code.SUCCESS);
                    cardInResult.setMsg("Th\u1ebb \u0111\u00e3 g\u1eedi l\u00ean h\u1ec7 th\u1ed1ng ch\u1edd x\u1eed l\u00fd!");
                    TopUpEntity topUpEntity = new TopUpEntity();
                    topUpEntity.setCash(amount);
                    topUpEntity.setRequest_id(requestId);
                    topUpEntity.setSerial(serial);
                    topUpEntity.setCode(code);
                    topUpEntity.setType(type);
                    cardInResult.setTopUpEntity(topUpEntity);
                    break;
                }
                case THE_DA_DUOC_SU_DUNG: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("Th\u1ebb \u0111\u00e3 \u0111\u01b0\u1ee3c s\u1eed d\u1ee5ng tr\u00ean h\u1ec7 th\u1ed1ng");
                    break;
                }
                case THE_DANG_BAO_TRI: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("Th\u1ebb \u0111ang b\u1ea3o tr\u00ec");
                    break;
                }
                case CHUA_NHAP_API_KEY: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("Ch\u01b0a nh\u1eadp API key");
                    break;
                }
                case SAI_THONG_TIN_API: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("Sai th\u00f4ng tin API");
                    break;
                }
                case TAI_KHOAN_BI_KHOA: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("T\u00e0i kho\u1ea3n \u0111\u00e3 b\u1ecb kh\u00f3a");
                    break;
                }
                case CHUA_NHAP_SERI: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("Ch\u01b0a nh\u1eadp seri");
                    break;
                }
                case CHUA_NHAP_MA_THE: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("Ch\u01b0a nh\u1eadp m\u00e3 th\u1ebb");
                    break;
                }
                case CHUA_CHON_MENH_GIA: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("Ch\u01b0a ch\u1ecdn m\u1ec7nh gi\u00e1");
                    break;
                }
                case LOI_KHONG_XAC_DINH: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh");
                    break;
                }
                default: {
                    cardInResult.setCode(Code.NOT_SUCCESS);
                    cardInResult.setMsg("L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh");
                }
            }
            return cardInResult;
        }
        cardInResult.setMsg(obj.getString("message"));
        return cardInResult;
    }

    @Override
    public HookCardInResult hookCardIn(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        String status = request.getParameter("status");
        String serial = request.getParameter("serial");
        String pin = request.getParameter("pin");
        String cardType = request.getParameter("card_type");
        String amountStr = request.getParameter("amount");
        String receiveAmountStr = request.getParameter("receive_amount");
        String realAmountStr = request.getParameter("real_amount");
        String noidung = request.getParameter("noidung");
        String content = request.getParameter("content");
        int amount = 0;
        try {
            amount = Integer.parseInt(realAmountStr);
        }
        catch (Exception e) {
            e.printStackTrace();
            return HookCardInResult.error(BaseResponse.New(4).toJson());
        }
        HookCardInResult hookCardInResult = new HookCardInResult(Code.ERROR);
        if ("thanhcong".equals(status)) {
            hookCardInResult.setCode(Code.SUCCESS);
            hookCardInResult.setResult_message("Th\u1ebb \u0111\u00fang.");
        } else if ("thatbai".equals(status)) {
            hookCardInResult.setCode(Code.NOT_SUCCESS);
            hookCardInResult.setResult_message("Th\u1ebb sai.");
        } else if ("saimenhgia".equals(status)) {
            hookCardInResult.setCode(Code.NOT_SUCCESS);
            hookCardInResult.setResult_message("Th\u1ebb ch\u1ecdn sai m\u1ec7nh gi\u00e1.");
        } else {
            hookCardInResult.setCode(Code.NOT_SUCCESS);
            hookCardInResult.setResult_message("Tr\u1ea1ng th\u00e1i kh\u00f4ng x\u00e1c \u0111\u1ecbnh.");
        }
        hookCardInResult.setRequestId(content);
        hookCardInResult.setAmount(amount);
        return hookCardInResult;
    }
}

