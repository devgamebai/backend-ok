/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import com.vinplay.usercore.entities.SendMail;
import java.sql.SQLException;
import java.util.List;

public interface SendMailService {
    public List<SendMail> search(String var1, int var2, int var3, String var4, String var5, int var6, int var7) throws SQLException;

    public int count(String var1, int var2, int var3, String var4, String var5) throws SQLException;

    public List<SendMail> getListRegister() throws SQLException;

    public List<SendMail> getListByType(int var1) throws SQLException;

    public SendMail getMail(int var1) throws SQLException;

    public boolean createMail(SendMail var1) throws SQLException;

    public boolean updateMail(SendMail var1) throws SQLException;

    public boolean deleteMail(int var1) throws SQLException;
}

