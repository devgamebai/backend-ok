/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.MailBoxResponse
 *  com.vinplay.vbee.common.response.SendMailResponse
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.impl.MailBoxDaoImpl;
import com.vinplay.usercore.service.MailBoxService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.MailBoxResponse;
import com.vinplay.vbee.common.response.SendMailResponse;
import java.sql.SQLException;
import java.util.List;

public class MailBoxServiceImpl
implements MailBoxService {
    @Override
    public boolean sendMailBoxFromByNickName(List<String> nickName, String title, String content) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.sendMailBoxFromByNickName(nickName, title, content);
    }

    @Override
    public boolean sendMailBoxFromByNickNameAdmin(String nickName, String title, String content) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.sendMailBoxFromByNickNameAdmin(nickName, title, content);
    }

    @Override
    public List<MailBoxResponse> listMailBox(String nickName, int page) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.listMailBox(nickName, page);
    }

    @Override
    public int updateStatusMailBox(String mailId) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.updateStatusMailBox(mailId);
    }

    @Override
    public int deleteMailBox(String mailId) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.deleteMailBox(mailId);
    }

    @Override
    public int countMailBox(String nickName) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.countMailBox(nickName);
    }

    @Override
    public SendMailResponse sendmailGiftCode(String nickName, String giftcode, String type, String price) throws SQLException {
        SendMailResponse response = new SendMailResponse(false, "1001", "");
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        UserServiceImpl user = new UserServiceImpl();
        String[] keys = nickName.split(",");
        String[] vals = giftcode.split(",");
        String title = "";
        if (type.equals("1")) {
            title = "Open Vinplay t\u1eb7ng ngay giftcode";
        }
        if (type.equals("2")) {
            title = "Vinplay t\u1eb7ng giftcode tri \u00e2n";
        }
        String lstError = "";
        if (keys.length > vals.length) {
            response.setErrorCode("10002");
            response.setSuccess(false);
            return response;
        }
        if (vals.length > keys.length) {
            response.setErrorCode("10003");
            response.setSuccess(false);
            return response;
        }
        if (vals.length == keys.length) {
            for (int i = 0; i < keys.length; ++i) {
                boolean flag = true;
                UserModel usermodel = user.getUserByNickName(keys[i]);
                if (usermodel == null) {
                    lstError = lstError + keys[i] + ",";
                    flag = false;
                }
                if (flag) {
                    keys[i] = usermodel.getNickname();
                    dao.sendmailGiftCode(keys[i], vals[i], title, type, price);
                    response.setErrorCode("0");
                    response.setSuccess(true);
                    continue;
                }
                response.setNickName(lstError);
                response.setErrorCode("10001");
                response.setSuccess(false);
            }
        }
        return response;
    }

    @Override
    public int countMailBoxInActive(String nickName) throws SQLException {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.countMailBoxInActive(nickName);
    }

    @Override
    public SendMailResponse sendMailGiftCode(String nickName, String giftcode, String title, String content) throws SQLException {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        SendMailResponse response = new SendMailResponse(false, "1001", "");
        String lstError = "";
        UserServiceImpl user = new UserServiceImpl();
        String[] keys = nickName.split(",");
        String[] vals = giftcode.split(",");
        boolean flag = true;
        if (vals.length == keys.length) {
            for (int i = 0; i < keys.length; ++i) {
                UserModel usermodel = user.getUserByNickName(keys[i]);
                if (usermodel == null) {
                    lstError = lstError + keys[i] + ",";
                    flag = false;
                }
                if (flag) {
                    keys[i] = usermodel.getNickname();
                    dao.sendMailGiftCode(keys[i], vals[i], title, content);
                    response.setErrorCode("0");
                    response.setSuccess(true);
                    continue;
                }
                response.setNickName(lstError);
                response.setErrorCode("10001");
                response.setSuccess(false);
            }
        } else {
            for (int i = 0; i < keys.length; ++i) {
                UserModel usermodel = user.getUserByNickName(keys[i]);
                if (usermodel == null) {
                    lstError = lstError + keys[i] + ",";
                    flag = false;
                }
                response.setNickName(lstError);
                response.setErrorCode("10002");
                response.setSuccess(false);
            }
        }
        return response;
    }

    @Override
    public int deleteMailBoxAdmin(String mailId) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.deleteMailBoxAdmin(mailId);
    }

    @Override
    public int deleteMutilMailBox(String nickName, String title) {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        return dao.deleteMutilMailBox(nickName, title);
    }

    @Override
    public SendMailResponse sendMailCardMobile(String nickName, String serial, String pin, String title, String content, String lstError) throws SQLException {
        MailBoxDaoImpl dao = new MailBoxDaoImpl();
        SendMailResponse response = new SendMailResponse(false, "1001", "");
        UserServiceImpl user = new UserServiceImpl();
        boolean flag = true;
        UserModel usermodel = user.getUserByNickName(nickName);
        if (usermodel == null) {
            lstError = lstError + nickName + ",";
            flag = false;
        }
        if (flag) {
            nickName = usermodel.getNickname();
            dao.sendMailCardMobile(nickName, serial, pin, title, content);
            response.setErrorCode("0");
            response.setSuccess(true);
        } else {
            response.setNickName(lstError);
            response.setErrorCode("0");
            response.setSuccess(true);
        }
        return response;
    }
}

