/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MailBoxResponse
 *  com.vinplay.vbee.common.response.SendMailResponse
 */
package com.vinplay.usercore.service;

import com.vinplay.vbee.common.response.MailBoxResponse;
import com.vinplay.vbee.common.response.SendMailResponse;
import java.sql.SQLException;
import java.util.List;

public interface MailBoxService {
    public boolean sendMailBoxFromByNickName(List<String> var1, String var2, String var3);

    public boolean sendMailBoxFromByNickNameAdmin(String var1, String var2, String var3);

    public List<MailBoxResponse> listMailBox(String var1, int var2);

    public int updateStatusMailBox(String var1);

    public int deleteMailBox(String var1);

    public int countMailBox(String var1);

    public SendMailResponse sendmailGiftCode(String var1, String var2, String var3, String var4) throws SQLException;

    public int countMailBoxInActive(String var1) throws SQLException;

    public SendMailResponse sendMailGiftCode(String var1, String var2, String var3, String var4) throws SQLException;

    public int deleteMailBoxAdmin(String var1);

    public int deleteMutilMailBox(String var1, String var2);

    public SendMailResponse sendMailCardMobile(String var1, String var2, String var3, String var4, String var5, String var6) throws SQLException;
}

