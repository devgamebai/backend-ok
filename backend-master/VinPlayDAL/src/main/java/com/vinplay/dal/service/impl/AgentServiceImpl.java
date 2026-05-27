/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.AgentModel
 *  com.vinplay.vbee.common.response.AgentResponse
 *  com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse
 *  com.vinplay.vbee.common.response.TranferAgentResponse
 */
package com.vinplay.dal.service.impl;

import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.dal.service.AgentService;
import com.vinplay.vbee.common.models.AgentModel;
import com.vinplay.vbee.common.response.AgentResponse;
import com.vinplay.vbee.common.response.LogAgentTranferMoneyResponse;
import com.vinplay.vbee.common.response.TranferAgentResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AgentServiceImpl
implements AgentService {
    @Override
    public List<AgentResponse> listAgent() throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.listAgent();
    }

    @Override
    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String top_ds, int page) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.searchAgentTranferMoney(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, top_ds, page);
    }

    @Override
    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String top_ds, int page, String process) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.searchAgentTranferMoney(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, top_ds, page);
    }

    @Override
    public List<AgentResponse> listUserAgent(String nickName) throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.listUserAgent(nickName);
    }

    @Override
    public List<TranferAgentResponse> searchAgentTranfer(String nickName, String status, String timeStart, String timeEnd, double ratio1, double ratio2, double ratio2More, long minRefundFee2More) throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
//        List<Object> agent = new ArrayList();
        List<AgentResponse> agent = new ArrayList();

        ArrayList<TranferAgentResponse> result = new ArrayList<TranferAgentResponse>();
        String[] arrNN = nickName.split(",");
        for (int i = 0; i < arrNN.length; ++i) {
            String nn = arrNN[i].trim();
            agent = dao.listUserAgent(nn);
            for (AgentResponse name : agent) {
                TranferAgentResponse tran = new TranferAgentResponse();
                tran = dao.searchAgentTranfer(name.nickName, status, timeStart, timeEnd);
                long ds2 = tran.getTotalBuy2() + tran.getTotalSale2();
                long fee2 = 0L;
                fee2 = ds2 < minRefundFee2More ? Math.round((double)(tran.getTotalFeeBuy2() + tran.getTotalFeeSale2()) * ratio2) : Math.round((double)(tran.getTotalFeeBuy2() + tran.getTotalFeeSale2()) * ratio2More);
                long totalFee = Math.round((double)(tran.getTotalFeeBuy1() + tran.getTotalFeeSale1()) * ratio1) + fee2;
                long totalFeeByVinplayCardTemp = Math.round(totalFee * (long)name.percent / 100L);
                long totalFeeByVinplayCard = AgentServiceImpl.roundVinCard(totalFee, totalFeeByVinplayCardTemp);
                long totalFeeByVinCash = totalFee - totalFeeByVinplayCard;
                tran.setTotalFee(totalFee);
                tran.setTotalFeeByVinplayCard(totalFeeByVinplayCard);
                tran.setTotalFeeByVinCash(totalFeeByVinCash);
                tran.setPercent(name.percent);
                result.add(tran);
            }
        }
        return result;
    }

    private static long roundVinCard(long moneyTotal, long moneyVinCard) {
        if (moneyVinCard < 10500L) {
            return 0L;
        }
        long division = moneyVinCard / 10500L;
        if ((division + 1L) * 10500L < moneyTotal) {
            return (division + 1L) * 10500L;
        }
        return division * 10500L;
    }

    @Override
    public long countsearchAgentTranferMoney(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String topDS) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.countsearchAgentTranferMoney(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, topDS);
    }

    @Override
    public long countsearchAgentTranferMoney(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String topDS, String process) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.countsearchAgentTranferMoney(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, topDS);
    }

    @Override
    public long totalMoneyVinReceiveFromAgent(String nickName, String status, String timeStart, String timeEnd, String topDS) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinReceiveFromAgent(nickName, status, timeStart, timeEnd, topDS);
    }

    @Override
    public long totalMoneyVinSendFromAgent(String nickName, String status, String timeStart, String timeEnd, String topDS) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinSendFromAgent(nickName, status, timeStart, timeEnd, topDS);
    }

    @Override
    public long totalMoneyVinFeeFromAgent(String nickName, String status, String timeStart, String timeEnd, String topDS) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinFeeFromAgent(nickName, status, timeStart, timeEnd, topDS);
    }

    @Override
    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoneyVinSale(String nickName, String timeStart, String timeEnd, String type, int page, int totalRecord) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.searchAgentTranferMoneyVinSale(nickName, timeStart, timeEnd, type, page, totalRecord);
    }

    @Override
    public long totalMoneyVinReceiveFromAgentByStatus(String nickName, String type, String timeStart, String timeEnd) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinReceiveFromAgentByStatus(nickName, type, timeStart, timeEnd);
    }

    @Override
    public long totalMoneyVinSendFromAgentByStatus(String nickName, String type, String timeStart, String timeEnd) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinSendFromAgentByStatus(nickName, type, timeStart, timeEnd);
    }

    @Override
    public long countSearchAgentTranferMoneyVinSale(String nickName, String timeStart, String timeEnd, String type) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.countSearchAgentTranferMoneyVinSale(nickName, timeStart, timeEnd, type);
    }

    @Override
    public boolean updateTopDsFromAgent(String nickNameSend, String nickNameReceive, String timeLog, String topds) {
        try {
            AgentDAOImpl dao = new AgentDAOImpl();
            dao.updateTopDsFromAgent(nickNameSend, nickNameReceive, timeLog, topds);
            dao.updateTopDsFromAgentMySQL(nickNameSend, nickNameReceive, timeLog, topds);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public List<TranferAgentResponse> searchAgentTranferAdmin(String nickName, String status, String timeStart, String timeEnd, double ratio1, double ratio2, double ratio2More, long minRefundFee2More) throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
        List<AgentResponse> agent = new ArrayList();
        ArrayList<TranferAgentResponse> result = new ArrayList<TranferAgentResponse>();
        String[] arrNN = nickName.split(",");
        for (int i = 0; i < arrNN.length; ++i) {
            String nn = arrNN[i].trim();
            agent = dao.listUserAgentAdmin(nn);
            for (AgentResponse name : agent) {
                TranferAgentResponse tran = new TranferAgentResponse();
                tran = dao.searchAgentTranfer(name.nickName, status, timeStart, timeEnd);
                long ds2 = tran.getTotalBuy2() + tran.getTotalSale2();
                long fee2 = 0L;
                fee2 = ds2 < minRefundFee2More ? Math.round((double)(tran.getTotalFeeBuy2() + tran.getTotalFeeSale2()) * ratio2) : Math.round((double)(tran.getTotalFeeBuy2() + tran.getTotalFeeSale2()) * ratio2More);
                long totalFee = Math.round((double)(tran.getTotalFeeBuy1() + tran.getTotalFeeSale1()) * ratio1) + fee2;
                long totalFeeByVinplayCardTemp = Math.round(totalFee * (long)name.percent / 100L);
                long totalFeeByVinplayCard = AgentServiceImpl.roundVinCard(totalFee, totalFeeByVinplayCardTemp);
                long totalFeeByVinCash = totalFee - totalFeeByVinplayCard;
                tran.setTotalFee(totalFee);
                tran.setTotalFeeByVinplayCard(totalFeeByVinplayCard);
                tran.setTotalFeeByVinCash(totalFeeByVinCash);
                tran.setPercent(name.percent);
                result.add(tran);
            }
        }
        return result;
    }

    @Override
    public List<LogAgentTranferMoneyResponse> searchAgentTranferMoney(String nickName, String status, String timeStart, String timeEnd, String top_ds, int page) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.searchAgentTranferMoney(nickName, status, timeStart, timeEnd, top_ds, page);
    }

    @Override
    public long countsearchAgentTranferMoney(String nickName, String status, String timeStart, String timeEnd, String top_ds) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.countsearchAgentTranferMoney(nickName, status, timeStart, timeEnd, top_ds);
    }

    @Override
    public long totalMoneyVinReceiveFromAgent(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String top_ds) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinReceiveFromAgent(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, top_ds);
    }

    @Override
    public long totalMoneyVinSendFromAgent(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String top_ds) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinSendFromAgent(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, top_ds);
    }

    @Override
    public long totalMoneyVinFeeFromAgent(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String topDS) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinFeeFromAgent(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, topDS);
    }

    @Override
    public List<AgentResponse> listUserAgentActive(String nickName) throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.listUserAgentActive(nickName);
    }

    @Override
    public List<AgentResponse> listUserAgentLevel1Active() throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.listUserAgentLevel1Active();
    }

    @Override
    public List<LogAgentTranferMoneyResponse> searchAgentTongTranferMoney(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String top_ds, int page) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.searchAgentTongTranferMoney(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, top_ds, page);
    }

    @Override
    public long countsearchAgentTongTranferMoney(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String topDS) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.countsearchAgentTongTranferMoney(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, topDS);
    }

    @Override
    public long totalMoneyVinReceiveFromAgentTong(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String top_ds) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinReceiveFromAgentTong(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, top_ds);
    }

    @Override
    public long totalMoneyVinSendFromAgentTong(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String top_ds) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinSendFromAgentTong(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, top_ds);
    }

    @Override
    public long totalMoneyVinFeeFromAgentTong(String nickNameSend, String nickNameRecieve, String status, String timeStart, String timeEnd, String topDS) {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.totalMoneyVinFeeFromAgentTong(nickNameSend, nickNameRecieve, status, timeStart, timeEnd, topDS);
    }

    @Override
    public List<AgentModel> getListPercentBonusVincard(String nickName) throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.getListPercentBonusVincard(nickName);
    }

    @Override
    public int registerPercentBonusVincard(String nickName, int percent) throws SQLException {
        AgentDAOImpl dao = new AgentDAOImpl();
        return dao.registerPercentBonusVincard(nickName, percent);
    }
}

