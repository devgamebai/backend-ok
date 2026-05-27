/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.dao.impl.MailBoxDaoImpl
 *  com.vinplay.usercore.dao.impl.VippointDaoImpl
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.vippoint.VippointEventMessage
 */
package com.vinplay.vbee.rmq.vippoint.processor;

import com.vinplay.usercore.dao.impl.MailBoxDaoImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.vippoint.VippointEventMessage;
import com.vinplay.vbee.dao.impl.VippointDaoImpl;
import java.util.ArrayList;
import java.util.List;

public class UpdateVippointEventProcessor
implements BaseProcessor<byte[], Boolean> {
    public Boolean execute(Param<byte[]> param) {
        VippointEventMessage message = (VippointEventMessage)BaseMessage.fromBytes((byte[])((byte[])param.get()));
        VippointDaoImpl dao = new VippointDaoImpl();
        UserServiceImpl service = new UserServiceImpl();
        MailBoxDaoImpl mailDao = new MailBoxDaoImpl();
        try {
            int isBot = service.checkBot(message.getNickname());
            dao.updateVippointEvent(message, isBot);
            int type = message.getType();
            if (type != 0) {
                ArrayList<String> nickname = new ArrayList<String>();
                nickname.add(message.getNickname());
                String title = "Th\u00f4ng b\u00e1o t\u1eeb s\u1ef1 ki\u1ec7n \u0111ua top Vippoint";
                String content = "";
                if (type == 1) {
                    content = "Ch\u00fac m\u1eebng " + message.getNickname() + "  v\u1eeba nh\u1eadn \u0111\u01b0\u1ee3c l\u00ec x\u00ec trong s\u1ef1 ki\u1ec7n \u0111ua top Vippoint, g\u00f3i qu\u00e0 b\u1ea1n nh\u1eadn \u0111\u01b0\u1ee3c tr\u1ecb gi\u00e1 " + message.getVp() + "(VP). Vippoint \u0111\u01b0\u1ee3c c\u1ed9ng c\u00f3 gi\u00e1 tr\u1ecb s\u1eed d\u1ee5ng trong s\u1ef1 ki\u1ec7n v\u00e0 kh\u00f4ng t\u00ednh c\u1ed9ng d\u1ed3n v\u00e0o Vippoint hi\u1ec7n c\u00f3 c\u1ee7a t\u00e0i kho\u1ea3n. Chi ti\u1ebft s\u1ef1 ki\u1ec7n xem t\u1ea1i " + GameCommon.getValueStr((String)"VIPPOINT_EVENT_URL");
                } else if (type == 2) {
                    content = "R\u1ea5t ti\u1ebfc " + message.getNickname() + " v\u1eeba b\u1ecb t\u1ea5n c\u00f4ng v\u00e0 b\u1ecb tr\u1eeb " + message.getVp() + "(VP). S\u1ed1 VipPoint b\u1ecb tr\u1eeb s\u1ebd \u0111\u01b0\u1ee3c t\u00ednh c\u1ed9ng d\u1ed3n \u0111\u1ec3 \u0111ua top 20 ki\u00ean c\u01b0\u1eddng trong su\u1ed1t s\u1ef1 ki\u1ec7n \u0111ua top Vippoint. Vippoint b\u1ecb tr\u1eeb trong s\u1ef1 ki\u1ec7n v\u00e0 kh\u00f4ng t\u00ednh tr\u1eeb v\u00e0o Vippoint hi\u1ec7n c\u00f3 c\u1ee7a t\u00e0i kho\u1ea3n. Chi ti\u1ebft s\u1ef1 ki\u1ec7n xem t\u1ea1i " + GameCommon.getValueStr((String)"VIPPOINT_EVENT_URL");
                } else if (type == 3) {
                    content = "Ch\u00fac m\u1eebng " + message.getNickname() + "  v\u1eeba nh\u1eadn \u0111\u01b0\u1ee3c l\u00ec x\u00ec trong s\u1ef1 ki\u1ec7n \u0111ua top Vippoint, g\u00f3i qu\u00e0 b\u1ea1n nh\u1eadn \u0111\u01b0\u1ee3c tr\u1ecb gi\u00e1 " + message.getVp() + "(vin). Chi ti\u1ebft s\u1ef1 ki\u1ec7n xem t\u1ea1i " + GameCommon.getValueStr((String)"VIPPOINT_EVENT_URL");
                }
                mailDao.sendMailBoxFromByNickName(nickname, "Th\u00f4ng b\u00e1o t\u1eeb s\u1ef1 ki\u1ec7n \u0111ua top Vippoint", content);
                com.vinplay.usercore.dao.impl.VippointDaoImpl vpEventDao = new com.vinplay.usercore.dao.impl.VippointDaoImpl();
                vpEventDao.logVippointEvent(message.getNickname(), type, message.getVp(), isBot);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}

