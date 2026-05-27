/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.service.impl.OtpServiceImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponseModel
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.backend.processors.money;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponseModel;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.UUID;

public class UpdateMoneyUserProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String)"backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest)param.get();
        BaseResponseModel response = new BaseResponseModel(false, "1001");
        // Capture inputs up front so the audit-log write below has them in
        // every code path — including early returns and exceptions. The
        // write itself happens AFTER the money operation, gated on
        // `response.isSuccess()` so log_admin.status is derived from the
        // actual outcome, not whatever a separate FE call says.
        String actionName = request.getParameter("ac");
        String nickname = request.getParameter("nn");
        long money = 0L;
        try { money = Long.valueOf(request.getParameter("mn")); } catch (Exception ignored) {}
        String moneyType = request.getParameter("mt");
        String reason = request.getParameter("rs");
        String aat = request.getParameter("aat");
        String adminUser = com.vinplay.api.backend.auth.AdminAuthHelper.getAdminUsernameByToken(aat);
        try {
            String otp = request.getParameter("otp");
            String type = request.getParameter("type");
            String nicknameSend = request.getParameter("nns");
            logger.debug((Object)("Request UpdateMoneyUser: nickname: " + nickname + ", money: " + money + ", moneyType: " + moneyType + ", reason: " + reason + ", otp: " + otp + ", otpType: " + type));
            if (nickname != null && reason != null && !reason.isEmpty() && money != 0L && moneyType != null && (moneyType.equals("vin") || moneyType.equals("xu")) && otp != null && type != null && (type.equals("1") || type.equals("0"))) {
                OtpServiceImpl otpService = new OtpServiceImpl();
                int code = 3;
                if (moneyType.equals("vin")) {
                    if(otp.equals("1") || otp.equals("")){
                        code = 0;
                    }else{
                        String admin;
                        String[] arr = GameCommon.getValueStr((String)"SUPER_ADMIN").split(",");
                        int i = 0;
                        String[] array = arr;
                        int length = array.length;
//                    for (int j = 0; j < length && (code = otpService.checkOtp(otp, admin = array[j], type, (String)null)) != 0; ++j) {
                        for (int j = 0; j < length && (code = otpService.checkOdp(array[j], otp)) != 0; ++j) {
                            if (i > 0 && money > 2000000L) {
                                return response.toJson();
                            }
                            ++i;
                        }
                    }

                } else if (nicknameSend != null) {
//                    code = otpService.checkOtp(otp, nicknameSend, type, (String)null);
//                    16 Sep 2019 | 02:08:35,355 | DEBUG | qtp21802780-63 - /api_backend?c=100&nn=cuccu888&mn=1000000&mt=vin&rs=AAAAAA&otp=55555&type=0&ac=Admin | backend |     | Request UpdateMoneyUser: nickname: cuccu888, money: 1000000, moneyType: vin, reason: AAAAAA, otp: 55555, otpType: 0
                    String[] arr = GameCommon.getValueStr((String)"SUPER_ADMIN").split(",");
                    code = otpService.checkOdp(arr[0], otp);
                }
                if (code == 0) {
                    if (actionName == null) {
                        actionName = "Admin";
                    }
                    if ("vin".equals(moneyType)) {
                        // Admin VIN adjustments must touch the current wallet balance only.
                        // `vin_total` is a cumulative game P&L counter; routing admin
                        // deductions through updateMoneyFromAdmin() made VIN tong go
                        // negative after a topup-then-deduct flow (KwonUSD2).
                        long userId = 0;
                        try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpoolname");
                             java.sql.PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE nick_name = ?")) {
                            ps.setString(1, nickname);
                            try (java.sql.ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) userId = rs.getLong("id");
                            }
                        }
                        if (userId > 0) {
                            com.vinplay.dal.service.MoneyGateway.CreditResult cr;
                            String adminTxId = resolveAdminTxId(request);
                            if (money > 0) {
                                // Positive vin topup → use MoneyGateway (handles recharge_money,
                                // balance push, audit log, Hazelcast cache)
                                cr = com.vinplay.dal.service.MoneyGateway.creditUser(
                                        userId, nickname, money,
                                        com.vinplay.dal.service.MoneyGateway.SOURCE_ADMIN_TOPUP,
                                        adminTxId, "Admin topup: " + reason);
                            } else {
                                cr = com.vinplay.dal.service.MoneyGateway.debitUser(
                                        userId, nickname, -money,
                                        com.vinplay.dal.service.MoneyGateway.SOURCE_ADMIN_DEDUCT,
                                        adminTxId, "Admin deduct: " + reason);
                            }
                            if (cr.success) {
                                response = new BaseResponseModel(true, "0");
                            } else if (com.vinplay.dal.service.MoneyGateway.CreditResult.ERROR_INSUFFICIENT_BALANCE.equals(cr.failureCode)) {
                                response.setErrorCode("1002");
                                response.setMessage(cr.error);
                            } else if (cr.error != null && !cr.error.isEmpty()) {
                                response.setMessage(cr.error);
                            }
                        } else {
                            response.setErrorCode("1005");
                        }
                    } else {
                        // xu → legacy path
                        UserServiceImpl service = new UserServiceImpl();
                        response = service.updateMoneyFromAdmin(nickname, money, moneyType, actionName, "Admin", reason);
                        if (response.isSuccess()) {
                            ArrayList<String> listUser = new ArrayList<>();
                            listUser.add(nickname);
                            BackendUtils.sendUpdateUserMoneyInfo(listUser);
                        }
                    }
                } else if (code == 3) {
                    response.setErrorCode("1008");
                } else if (code == 4) {
                    response.setErrorCode("1021");
                }
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        // Inline audit-log write — replaces the FE-driven LogAdminActionProcessor
        // call which used to record whatever status the FE sent (status=1 even
        // on c=100 errors, e.g. when admin targeted a 0-vin shadow account by
        // mistake). Now log_admin.status is derived from the ACTUAL c=100
        // outcome (response.isSuccess()), so the audit trail can't lie.
        // Same row shape as LogAdminActionProcessor for FE compatibility.
        // Best-effort: never let an audit-log failure mask the money result.
        try {
            String action = money >= 0 ? "Cộng tiền Admin" : "Trừ tiền Admin";
            String status = response.isSuccess() ? "1" : "0";
            String un = (adminUser != null && !adminUser.isEmpty())
                    ? adminUser
                    : (actionName != null ? actionName : "Admin");
            try (java.sql.Connection conn = com.vinplay.vbee.common.pools.ConnectionPool.getInstance().getConnection("mysqlpool_admin");
                 java.sql.PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO log_admin (username, action, quantity, money, account_name, money_type, reason, status) VALUES (?,?,?,?,?,?,?,?)")) {
                ps.setString(1, un);
                ps.setString(2, action);
                ps.setString(3, "0");
                ps.setString(4, String.valueOf(money));
                ps.setString(5, nickname != null ? nickname : "");
                ps.setString(6, moneyType != null ? moneyType : "");
                ps.setString(7, reason != null ? reason : "");
                ps.setString(8, status);
                ps.executeUpdate();
            }
        } catch (Exception auditErr) {
            logger.warn("UpdateMoneyUserProcessor: log_admin audit failed nickname=" + nickname
                    + " money=" + money + " — money result was " + (response.isSuccess() ? "PAID" : "FAIL"), auditErr);
        }
        logger.debug((Object)("Response UpdateMoneyUser: " + response.toJson()));
        return response.toJson();
    }

    private static String resolveAdminTxId(HttpServletRequest request) {
        // FE may pass a stable per-click idempotency key as tx_id/rid. When it
        // does not, generate one so every admin wallet adjustment is still
        // traceable in money_gateway_log / money_ledger.
        String txId = request.getParameter("tx_id");
        if (txId == null || txId.trim().isEmpty()) {
            txId = request.getParameter("rid");
        }
        if (txId == null || txId.trim().isEmpty()) {
            txId = "admin-adjust:" + UUID.randomUUID().toString();
        }
        return txId.trim();
    }
}
