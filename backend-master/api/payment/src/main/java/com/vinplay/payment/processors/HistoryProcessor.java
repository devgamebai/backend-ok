/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.payment.core.common.StringUtil
 *  com.vinplay.dal.dao.impl.LogMoneyUserDaoImpl
 *  com.vinplay.dal.model.LogMoneyUserVin4Report
 *  com.vinplay.payment.model.WithDrawHistory
 *  com.vinplay.payment.service.impl.WithDrawManualBankServiceImpl
 *  com.vinplay.payment.utils.PayCommon$PAYSTATUS
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.payment.processors;

import com.payment.core.common.StringUtil;
import com.vinplay.dal.dao.impl.LogMoneyUserDaoImpl;
import com.vinplay.dal.model.LogMoneyUserVin4Report;
import com.vinplay.payment.model.WithDrawHistory;
import com.vinplay.payment.service.impl.WithDrawManualBankServiceImpl;
import com.vinplay.payment.utils.PayCommon;
import com.vinplay.response.HistoryLog;
import com.vinplay.response.LogMoneyUserResponse;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class HistoryProcessor
implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger(HistoryProcessor.class);

    public String execute(Param<HttpServletRequest> param) {
        boolean errorResult;
        HttpServletRequest request = (HttpServletRequest)param.get();
        String nickname = StringUtils.trimToEmpty((String)request.getParameter("nn"));
        String transactionType = StringUtils.trimToEmpty((String)request.getParameter("tt"));
        String accessToken = StringUtils.trimToEmpty((String)request.getParameter("at"));
        String maxItemStr = StringUtils.trimToEmpty((String)request.getParameter("mi"));
        String pageStr = StringUtils.trimToEmpty((String)request.getParameter("p"));
        String fromTime = StringUtils.trimToEmpty((String)request.getParameter("ft"));
        String endTime = StringUtils.trimToEmpty((String)request.getParameter("et"));
        if (fromTime.isEmpty()) {
            fromTime = "2023-01-04 00:00:00";
        }
        if (endTime.isEmpty()) {
            endTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 23:59:59";
        }
        if (errorResult = StringUtil.handleBlankParams((String[])new String[]{nickname, accessToken, transactionType})) {
            return BaseResponse.error((String)"5", (String)"Nickname, accessToken, transactionType not empty");
        }
        if (!transactionType.equals("0") && !transactionType.equals("1")) {
            return BaseResponse.error((String)"5", (String)"Value of transaction type is invalid");
        }
        int status = -1;
        int page = 0;
        try {
            page = Integer.parseInt(pageStr);
        }
        catch (Exception e) {
            return BaseResponse.error((String)"5", (String)"Page index not empty");
        }
        int maxItem = 0;
        try {
            maxItem = Integer.parseInt(maxItemStr);
        }
        catch (Exception e) {
            return BaseResponse.error((String)"5", (String)"Limit item per page not empty");
        }
        if (page < 0) {
            return BaseResponse.error((String)"5", (String)"page <0");
        }
        if (maxItem < 0) {
            return BaseResponse.error((String)"5", (String)"maxItem <0");
        }
        logger.info((Object)("Request payment history nickname= " + nickname + ", status: " + status + ", page: " + page + ", maxItem: " + maxItem + ", fromTime: " + fromTime + ", endTime: " + endTime + ", accessToken: " + accessToken + ", transactionType: " + transactionType));
        UserServiceImpl userService = new UserServiceImpl();
        boolean isToken = userService.isActiveToken(nickname, accessToken);
        if (isToken) {
            try {
                LogMoneyUserDaoImpl dao = new LogMoneyUserDaoImpl();
                LogMoneyUserResponse res = new LogMoneyUserResponse(false, "1001");
                if (transactionType.equals("0")) {
                    ArrayList<String> actions = new ArrayList<String>();
                    actions.add("TopupCard");
                    actions.add("BankCode");
                    LogMoneyUserVin4Report logMoneyUserVin4ReportWithListAction = dao.getLogMoneyUserVin4ReportWithListAction(nickname, fromTime, endTime, actions, page, maxItem);
                    ArrayList<HistoryLog> historyLogs = new ArrayList<HistoryLog>();
                    if (logMoneyUserVin4ReportWithListAction.getTotalData() > 0L) {
                        logMoneyUserVin4ReportWithListAction.getList().forEach(logMoneyUserNapTieuVinModel -> {
                            HistoryLog historyLog = new HistoryLog();
                            historyLog.setAmount(logMoneyUserNapTieuVinModel.getMoney_exchange());
                            if (logMoneyUserNapTieuVinModel.getAction_name().contains("TopupCard")) {
                                historyLog.setType("Card");
                            }
                            if (logMoneyUserNapTieuVinModel.getAction_name().contains("BankCode")) {
                                historyLog.setType("Bank");
                            }
                            historyLog.setDescription(logMoneyUserNapTieuVinModel.getDescription());
                            historyLog.setCreatedAt(logMoneyUserNapTieuVinModel.getTrans_time());
                            historyLog.setProviderName("");
                            historyLog.setStatus(HistoryLog.Status.SUCCESS);
                            historyLog.setTransactionId(String.valueOf(logMoneyUserNapTieuVinModel.getTrans_id()));
                            historyLogs.add(historyLog);
                        });
                    }
                    res.setList(historyLogs);
                    res.setTotalData(logMoneyUserVin4ReportWithListAction.getTotalData());
                    res.setTotalFee(logMoneyUserVin4ReportWithListAction.getTotalFee());
                    res.setTotalMoneyExchange(logMoneyUserVin4ReportWithListAction.getTotalMoneyExchange());
                } else if (transactionType.equals("1")) {
                    WithDrawManualBankServiceImpl withdrawService = new WithDrawManualBankServiceImpl();
                    WithDrawHistory history = withdrawService.historyNickName(nickname, status, page, maxItem, fromTime, endTime, "");
                    ArrayList<HistoryLog> historyLogs = new ArrayList<HistoryLog>();
                    if (history.getTotalData() > 0L) {
                        history.getList().forEach(withDrawHistory -> {
                            HistoryLog historyLog = new HistoryLog();
                            historyLog.setAmount(withDrawHistory.Amount);
                            historyLog.setType("Bank");
                            historyLog.setDescription(withDrawHistory.Description);
                            historyLog.setCreatedAt(withDrawHistory.CreatedAt);
                            historyLog.setProviderName(withDrawHistory.ProviderName);
                            PayCommon.PAYSTATUS paystatus = PayCommon.PAYSTATUS.getById((Integer)withDrawHistory.Status);
                            switch (paystatus) {
                                case SUCCESS: {
                                    historyLog.setStatus(HistoryLog.Status.SUCCESS);
                                    break;
                                }
                                case FAILED: {
                                    historyLog.setStatus(HistoryLog.Status.FAILED);
                                    break;
                                }
                                case COMPLETED: {
                                    historyLog.setStatus(HistoryLog.Status.SUCCESS);
                                    break;
                                }
                                case REVIEW: {
                                    historyLog.setStatus(HistoryLog.Status.PENDING);
                                    break;
                                }
                                case SPAM: {
                                    historyLog.setStatus(HistoryLog.Status.FAILED);
                                    break;
                                }
                                case REQUEST: {
                                    historyLog.setStatus(HistoryLog.Status.PENDING);
                                    break;
                                }
                                default: {
                                    historyLog.setStatus(HistoryLog.Status.UNKNOWN);
                                }
                            }
                            historyLogs.add(historyLog);
                        });
                    }
                    res.setList(historyLogs);
                    res.setTotalData(history.getTotalData());
                }
                res.setErrorCode("0");
                return res.toJson();
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error((Object)e);
                return new BaseResponse(true, "1001", null, null, 0L).toJson();
            }
        }
        return BaseResponse.error((String)"4", (String)"Phi\u00ean l\u00e0m vi\u1ec7c c\u1ee7a b\u1ea1n \u0111\u00e3 h\u1ebft h\u1ea1n , vui l\u00f2ng t\u1ea3i l\u1ea1i trang !");
    }
}

