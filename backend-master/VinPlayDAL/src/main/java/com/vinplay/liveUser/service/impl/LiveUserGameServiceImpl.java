/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.liveUser.service.impl;

import com.vinplay.liveUser.dao.LiveUserGameDAO;
import com.vinplay.liveUser.dao.impl.LiveUserGameDAOImpl;
import com.vinplay.liveUser.entities.LiveUserGameEntity;
import com.vinplay.liveUser.service.LiveUserGameService;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class LiveUserGameServiceImpl
implements LiveUserGameService {
    private LiveUserGameDAO liveUserGameDAO = new LiveUserGameDAOImpl();

    @Override
    public boolean create(LiveUserGameEntity entity, String userAction) throws SQLException {
        entity.setLast_updated_by(userAction);
        entity.setLast_updated_at(new Date());
        entity.setCreated_by(userAction);
        entity.setCreated_at(new Date());
        return this.liveUserGameDAO.create(entity);
    }

    @Override
    public boolean update(LiveUserGameEntity entity, String userAction) throws SQLException {
        entity.setLast_updated_by(userAction);
        entity.setLast_updated_at(new Date());
        return this.liveUserGameDAO.update(entity);
    }

    @Override
    public boolean delete(int id, String userAction) throws SQLException {
        return this.liveUserGameDAO.delete(id, userAction);
    }

    @Override
    public LiveUserGameEntity get(int id) throws SQLException {
        return this.liveUserGameDAO.get(id);
    }

    @Override
    public int count(String nickname, String timeExpired, String status) throws SQLException {
        return this.liveUserGameDAO.count(nickname, timeExpired, status);
    }

    @Override
    public List<LiveUserGameEntity> search(String nickname, String timeExpired, String status, int page, int totalRecord) throws SQLException {
        return this.liveUserGameDAO.search(nickname, timeExpired, status, page, totalRecord);
    }
}

