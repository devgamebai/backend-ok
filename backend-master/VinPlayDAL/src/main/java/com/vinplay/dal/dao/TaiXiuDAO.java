/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.cache.ThanhDuTXModel
 *  com.vinplay.vbee.common.models.minigame.TopWin
 *  com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel
 */
package com.vinplay.dal.dao;

import com.vinplay.dal.entities.report.ReportMoneySystemModel;
import com.vinplay.dal.entities.taixiu.ResultTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiu;
import com.vinplay.dal.entities.taixiu.TransactionTaiXiuDetail;
import com.vinplay.dal.entities.taixiu.VinhDanhRLTLModel;
import com.vinplay.vbee.common.models.cache.ThanhDuTXModel;
import com.vinplay.vbee.common.models.minigame.TopWin;
import com.vinplay.vbee.common.models.minigame.taixiu.XepHangRLTLModel;
import java.sql.SQLException;
import java.util.List;

public interface TaiXiuDAO {
    public List<ResultTaiXiu> getLichSuPhien(int var1, int var2) throws SQLException;

    public ResultTaiXiu getKetQuaPhien(long var1, int var3) throws SQLException;

    public List<TransactionTaiXiu> getLichSuGiaoDich(String var1, int var2, int var3) throws SQLException;

    public int countLichSuGiaoDichTX(String var1, int var2) throws SQLException;

    public List<TopWin> getTopTaiXiu(int var1) throws SQLException;

    public List<TransactionTaiXiuDetail> getChiTietPhien(long var1, int var3) throws SQLException;

    public List<ThanhDuTXModel> getTopThanhDuDaily(String var1, String var2, short var3) throws SQLException;

    public int getMaxThanhDu(String var1, short var2) throws SQLException;

    public int getSoLanRutLoc(String var1) throws SQLException;

    public List<XepHangRLTLModel> getXepHangTanLoc();

    public List<VinhDanhRLTLModel> getVinhDanhTanLoc();

    public long getTongTienTanLoc(String var1);

    public List<XepHangRLTLModel> getXepHangRutLoc();

    public List<VinhDanhRLTLModel> getVinhDanhRutLoc();

    public long getTongTienRutLoc(String var1);

    public ReportMoneySystemModel getReportTXToDay();

    public ReportMoneySystemModel getReportTX(String var1, String var2);
}

