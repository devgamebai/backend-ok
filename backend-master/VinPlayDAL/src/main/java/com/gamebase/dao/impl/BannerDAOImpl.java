/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.gamebase.dao.impl;

import com.gamebase.dao.BannerDAO;
import com.gamebase.entities.BannerModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BannerDAOImpl
implements BannerDAO {
    @Override
    public long countlistBanner(String title, Integer status, String image_path, Integer eventId, String url) {
        long count = 0L;
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            ResultSet rs;
            int index = 1;
            Boolean b_title = title == null || title.trim().isEmpty();
            Boolean b_image_path = image_path == null || image_path.trim().isEmpty();
            Boolean b_status = status == null;
            Boolean b_url = url == null || url.trim().isEmpty();
            Boolean b_eventId = eventId == null;
            String sql = "Select count(*) as cnt from vinplay.banner where 1=1 " + (b_title != false ? "" : " and title = ?") + (b_status != false ? "" : " and status = ?") + (b_image_path != false ? "" : " and image_path = ?") + (b_eventId != false ? "" : " and event_id = ?") + (b_url != false ? "" : " and url = ?");
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (!b_title.booleanValue()) {
                stmt.setString(index++, title);
            }
            if (!b_status.booleanValue()) {
                stmt.setInt(index++, status);
            }
            if (!b_image_path.booleanValue()) {
                stmt.setString(index++, image_path);
            }
            if ((rs = stmt.executeQuery()).next()) {
                count = rs.getInt("cnt");
            }
            if (!b_eventId.booleanValue()) {
                stmt.setInt(index++, eventId);
            }
            if (!b_url.booleanValue()) {
                stmt.setString(index++, url);
            }
            rs.close();
            stmt.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public List<BannerModel> listBanner(String title, Integer status, String image_path, Integer eventId, String url, int page, int maxItem) {
        ArrayList<BannerModel> banners = new ArrayList<BannerModel>();
        Boolean b_title = title == null || title.trim().isEmpty();
        Boolean b_image_path = image_path == null || image_path.trim().isEmpty();
        Boolean b_status = status == null;
        Boolean b_url = url == null || url.trim().isEmpty();
        Boolean b_eventId = eventId == null;
        String sql = "Select * from vinplay.banner where 1=1 " + (b_title != false ? "" : " and title = ?") + (b_status != false ? "" : " and status = ?") + (b_image_path != false ? "" : " and image_path = ?") + (b_eventId != false ? "" : " and event_id = ?") + (b_url != false ? "" : " and url = ?") + " order by id desc limit ?,?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);){
            page = page - 1 < 0 ? 0 : page - 1;
            int index = 1;
            if (!b_title.booleanValue()) {
                stmt.setString(index++, title);
            }
            if (!b_status.booleanValue()) {
                stmt.setInt(index++, status);
            }
            if (!b_image_path.booleanValue()) {
                stmt.setString(index++, image_path);
            }
            if (!b_eventId.booleanValue()) {
                stmt.setInt(index++, eventId);
            }
            if (!b_url.booleanValue()) {
                stmt.setString(index++, url);
            }
            stmt.setInt(index++, page * maxItem);
            stmt.setInt(index, maxItem);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                BannerModel bannerModel = this.parseBannerModel(rs);
                banners.add(bannerModel);
            }
            rs.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return banners;
    }

    @Override
    public BannerModel BannerDetail(Integer id) {
        BannerModel bannerModel = new BannerModel();
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            String sql = "Select * from vinplay.banner where id = ? ";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                bannerModel = this.parseBannerModel(rs);
            }
            rs.close();
            stmt.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return bannerModel;
    }

    private BannerModel parseBannerModel(ResultSet rs) throws SQLException {
        BannerModel bannerModel = new BannerModel();
        bannerModel.setId(rs.getInt("id"));
        bannerModel.setTitle(rs.getString("title"));
        bannerModel.setStatus(rs.getInt("status"));
        bannerModel.setImage_path(rs.getString("image_path"));
        bannerModel.setIndex(rs.getInt("index"));
        bannerModel.setUrl(rs.getString("url"));
        bannerModel.setEventId(rs.getInt("event_id"));
        bannerModel.setActionType(rs.getString("action_type"));
        return bannerModel;
    }

    @Override
    public Boolean addNewBanner(BannerModel bannerModel) {
        String sql = "INSERT INTO vinplay.banner (title, status, image_path, action_type, event_id, `index`, url) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, bannerModel.getTitle());
            stm.setInt(2, bannerModel.getStatus());
            stm.setString(3, bannerModel.getImage_path());
            stm.setString(4, bannerModel.getActionType());
            stm.setInt(5, bannerModel.getEventId());
            stm.setInt(6, bannerModel.getIndex());
            stm.setString(7, bannerModel.getUrl());
            stm.executeUpdate();
            stm.close();
            conn.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public Boolean updateBannerById(BannerModel bannerModel) {
        String sql = "UPDATE vinplay.banner SET title = ?, status = ?, image_path = ?, action_type = ?, event_id = ?, `index` = ?, url = ? WHERE id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, bannerModel.getTitle());
            stm.setInt(2, bannerModel.getStatus());
            stm.setString(3, bannerModel.getImage_path());
            stm.setString(4, bannerModel.getActionType());
            stm.setInt(5, bannerModel.getEventId());
            stm.setInt(6, bannerModel.getIndex());
            stm.setString(7, bannerModel.getUrl());
            stm.setInt(8, bannerModel.getId());
            int rowsUpdated = stm.executeUpdate();
            stm.close();
            conn.close();
            Boolean bl = rowsUpdated > 0;
            return bl;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Boolean deleteBanner(Integer id) {
        String sql = "DELETE FROM vinplay.banner where id = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setInt(1, id);
            stm.executeUpdate();
            stm.close();
            if (conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean deleteBanner(String title) {
        String sql = "DELETE FROM vinplay.banner where title = ?";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");){
            PreparedStatement stm = conn.prepareStatement(sql);
            stm.setString(1, title);
            stm.executeUpdate();
            stm.close();
            if (conn != null) {
                conn.close();
            }
        }
        catch (SQLException e) {
            return false;
        }
        return true;
    }

    @Override
    public List<BannerModel> getListActive() {
        ArrayList<BannerModel> activeBanners = new ArrayList<BannerModel>();
        String sql = "SELECT * FROM vinplay.banner WHERE status = 1 ORDER BY `index`";
        try (Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery();){
            while (rs.next()) {
                BannerModel bannerModel = this.parseBannerModel(rs);
                activeBanners.add(bannerModel);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return activeBanners;
    }
}

