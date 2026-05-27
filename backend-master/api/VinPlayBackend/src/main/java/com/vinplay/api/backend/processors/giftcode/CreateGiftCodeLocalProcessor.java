package com.vinplay.api.backend.processors.giftcode;

import com.vinplay.giftcode.GiftCodeModel;
import com.vinplay.giftcode.GiftCodeType;
import com.vinplay.giftcode.GiftCodeUtil;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Date;

public class CreateGiftCodeLocalProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String) "backend");

    // Defaults per yêu cầu
    private static final int DEFAULT_EVENT = 15;
    private static final int DEFAULT_TYPE = GiftCodeType.ONE_FOR_ONE_USER_IN_EVENT; // = 3
    private static final String DEFAULT_CREATED_BY = "CONCHYMNON";
    private static final long EXPIRE_AFTER_MS = 7L * 24 * 60 * 60 * 1000; // 7 ngày

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        if (!isLocalRequest(request)) {
            return BaseResponse.error("-1", "Forbidden: API chỉ cho phép gọi local");
        }

        String giftCode = request.getParameter("giftCode");
        String amountStr = request.getParameter("Amount");

        if (giftCode == null || giftCode.trim().isEmpty()) {
            return BaseResponse.error("-1", "giftCode: Thiếu giftCode");
        }
        giftCode = giftCode.trim();

        int money;
        try {
            money = Integer.parseInt(amountStr);
            if (money <= 0) {
                return BaseResponse.error("-1", "Amount: Giá trị giftcode cần > 0");
            } else if (money > 7000000) {
                return BaseResponse.error("-1", "Amount: Giá trị giftcode tối đa là 7 triệu Win");
            }
        } catch (Exception e) {
            return BaseResponse.error("-1", "Amount: Giá trị giftcode phải là định dạng số");
        }

        try {
            if (GiftCodeUtil.giftCodeIsExits(giftCode)) {
                return BaseResponse.error("-1", "giftCode: Gift code đã tồn tại");
            }

            Timestamp startDate = new Timestamp(new Date().getTime());
            Timestamp endDate = new Timestamp(startDate.getTime() + EXPIRE_AFTER_MS);

            GiftCodeModel giftCodeModel = new GiftCodeModel();
            giftCodeModel.type = DEFAULT_TYPE;          // type=3
            giftCodeModel.money = money;                // am=Amount
            giftCodeModel.max_use = 1;                  // nu="" -> mặc định 1
            giftCodeModel.time_used = 0;
            giftCodeModel.from = startDate;             // st=now
            giftCodeModel.exprired = endDate;           // et=now+7 ngày
            giftCodeModel.created_by = DEFAULT_CREATED_BY; // cb="CONCHYMNON"
            giftCodeModel.event = DEFAULT_EVENT;        // ev=15
            giftCodeModel.user_name = "";               // nn=""
            giftCodeModel.giftcode = giftCode;          // gc=giftCode

            GiftCodeUtil.insertGiftCode(giftCodeModel);
            return BaseResponse.success(giftCode, 1);
        } catch (Exception e) {
            logger.trace(e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }

    private boolean isLocalRequest(HttpServletRequest request) {
        try {
            String remote = request.getRemoteAddr();
            // Chỉ cho phép loopback
            return "127.0.0.1".equals(remote) || "::1".equals(remote) || "0:0:0:0:0:0:0:1".equals(remote);
        } catch (Exception e) {
            return false;
        }
    }
}

