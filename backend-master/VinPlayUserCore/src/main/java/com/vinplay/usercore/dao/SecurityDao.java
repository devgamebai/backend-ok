/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 */
package com.vinplay.usercore.dao;

import com.vinplay.vbee.common.models.UserModel;
import java.sql.SQLException;
import java.text.ParseException;

public interface SecurityDao {
    public boolean updateUserInfo(int var1, String var2, int var3) throws SQLException;

    public boolean updateUserInfos(int var1, String var2, String var3, String var4) throws SQLException;

    public boolean updateUserVipInfo(int var1, String var2, boolean var3, String var4) throws SQLException, ParseException;

    public boolean checkEmail(String var1) throws SQLException;

    public boolean checkMobile(String var1) throws SQLException;

    public boolean checkIdentification(String var1) throws SQLException;

    public UserModel getStatus(String var1) throws SQLException;

    public boolean updateNewMobile(String var1, String var2, String var3);

    public boolean checkNewMobile(String var1);

    public boolean updateInfoLogin(int var1, String var2, String var3, String var4, String var5, int var6);
}

