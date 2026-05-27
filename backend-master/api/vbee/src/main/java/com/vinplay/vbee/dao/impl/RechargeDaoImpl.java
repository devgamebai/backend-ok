/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.mongodb.client.MongoCollection
 *  com.mongodb.client.MongoDatabase
 *  com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage
 *  com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage
 *  com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage
 *  com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory
 *  org.bson.Document
 */
package com.vinplay.vbee.dao.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vinplay.vbee.common.messages.dvt.RechargeByBankMessage;
import com.vinplay.vbee.common.messages.dvt.RechargeByCardMessage;
import com.vinplay.vbee.common.messages.dvt.RefundFeeAgentMessage;
import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.vinplay.vbee.dao.RechargeDao;
import java.util.Date;
import org.bson.Document;

public class RechargeDaoImpl
implements RechargeDao {
    @Override
    public void logRechargeByCard(RechargeByCardMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("dvt_recharge_by_card");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.getReferenceId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("provider", (Object)message.getProvider());
        doc.append("serial", (Object)message.getSerial());
        doc.append("pin", (Object)message.getPin());
        doc.append("amount", (Object)message.getAmount());
        doc.append("money", (Object)message.getMoney());
        doc.append("status", (Object)message.getStatus());
        doc.append("message", (Object)message.getMessage());
        doc.append("code", (Object)message.getCode());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("update_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        doc.append("partner", (Object)message.getPartner());
        doc.append("platform", (Object)message.getPlatform());
        doc.append("user_mega", (Object)message.getUserNameMega());
        doc.append("tran_id", (Object)message.getTranId());
        doc.append("request_id", (Object)message.getRequestId());
        col.insertOne((Object)doc);


    }

    @Override
    public void logRechargeByVinCard(RechargeByCardMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("dvt_recharge_by_vin_card");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.getReferenceId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("provider", (Object)message.getProvider());
        doc.append("serial", (Object)message.getSerial());
        doc.append("pin", (Object)message.getPin());
        doc.append("amount", (Object)message.getAmount());
        doc.append("money", (Object)message.getMoney());
        doc.append("status", (Object)message.getStatus());
        doc.append("message", (Object)message.getMessage());
        doc.append("code", (Object)message.getCode());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("update_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        doc.append("platform", (Object)message.getPlatform());
        col.insertOne((Object)doc);
    }

    @Override
    public void logRechargeByMegaCard(RechargeByCardMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("epay_recharge_by_mega_card");
        Document doc = new Document();
        doc.append("reference_id", (Object)message.getReferenceId());
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("provider", (Object)message.getProvider());
        doc.append("serial", (Object)message.getSerial());
        doc.append("pin", (Object)message.getPin());
        doc.append("amount", (Object)message.getAmount());
        doc.append("money", (Object)message.getMoney());
        doc.append("status", (Object)message.getStatus());
        doc.append("message", (Object)message.getMessage());
        doc.append("code", (Object)message.getCode());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("update_time", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        doc.append("partner", (Object)message.getPartner());
        doc.append("platform", (Object)message.getPlatform());
        col.insertOne((Object)doc);
    }

    @Override
    public void logRechargeByBank(RechargeByBankMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("dvt_recharge_by_bank");
        Document doc = new Document();
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("money", (Object)message.getMoney());
        doc.append("bank", (Object)message.getBank());
        doc.append("trans_id", (Object)message.getTransId());
        doc.append("amount", (Object)message.getAmount());
        doc.append("order_info", (Object)message.getOrderInfo());
        doc.append("ticket_no", (Object)message.getTicketNo());
        doc.append("trans_time", (Object)message.getCreateTime());
        doc.append("response_code", (Object)"");
        doc.append("description", (Object)"");
        doc.append("amount_receive", (Object)"");
        doc.append("transaction_no", (Object)"");
        doc.append("message", (Object)"");
        doc.append("update_time", (Object)"");
        doc.append("create_time", (Object)new Date());
        doc.append("platform", (Object)message.getPlatform());
        col.insertOne((Object)doc);
    }

    @Override
    public void logRefundFeeAgent(RefundFeeAgentMessage message) {
        MongoDatabase db = MongoDBConnectionFactory.getDB();
        MongoCollection col = db.getCollection("log_refund_fee_agent");
        Document doc = new Document();
        doc.append("nick_name", (Object)message.getNickname());
        doc.append("fee_1", (Object)message.getFee1());
        doc.append("ratio_1", (Object)message.getRatio1());
        doc.append("fee_2", (Object)message.getFee2());
        doc.append("ratio_2", (Object)message.getRatio2());
        doc.append("fee_2_more", (Object)message.getFee2More());
        doc.append("ratio_2_more", (Object)message.getRatio2More());
        doc.append("fee", (Object)message.getFee());
        doc.append("code", (Object)message.getCode());
        doc.append("month", (Object)message.getMonth());
        doc.append("description", (Object)message.getDescription());
        doc.append("time_log", (Object)message.getCreateTime());
        doc.append("create_time", (Object)new Date());
        doc.append("fee_vinplay_card", (Object)message.getFeeVinplayCard());
        doc.append("fee_vin_cash", (Object)message.getFeeVinCash());
        doc.append("percent", (Object)message.getPercent());
        doc.append("end_time", (Object)message.getEndTime());
        doc.append("start_time", (Object)message.getStartTime());
        col.insertOne((Object)doc);
    }
}

