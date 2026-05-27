/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.SendMailDao;
import com.vinplay.usercore.dao.impl.SendMailDaoImpl;
import com.vinplay.usercore.entities.SendMail;
import com.vinplay.usercore.service.SendMailService;
import java.sql.SQLException;
import java.util.List;

public class SendMailServiceImpl
implements SendMailService {
    SendMailDao sendMailDao = new SendMailDaoImpl();

    @Override
    public List<SendMail> search(String search, int status, int type, String timeStart, String timeEnd, int page, int totalRecord) throws SQLException {
        return this.sendMailDao.search(search, status, type, timeStart, timeEnd, page, totalRecord);
    }

    @Override
    public int count(String search, int status, int type, String timeStart, String timeEnd) throws SQLException {
        return this.sendMailDao.count(search, status, type, timeStart, timeEnd);
    }

    @Override
    public List<SendMail> getListRegister() throws SQLException {
        return this.sendMailDao.getListByType(1);
    }

    @Override
    public List<SendMail> getListByType(int type) throws SQLException {
        return this.sendMailDao.getListByType(type);
    }

    @Override
    public SendMail getMail(int id) throws SQLException {
        return this.sendMailDao.getMail(id);
    }

    @Override
    public boolean createMail(SendMail mail) throws SQLException {
        return this.sendMailDao.createMail(mail);
    }

    @Override
    public boolean updateMail(SendMail mail) throws SQLException {
        return this.sendMailDao.updateMail(mail);
    }

    @Override
    public boolean deleteMail(int id) throws SQLException {
        return this.sendMailDao.deleteMail(id);
    }
}

