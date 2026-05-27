/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.models.UserModel
 *  com.vinplay.vbee.common.response.MoneyResponse
 */
package com.vinplay.dal.service;

import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.MoneyResponse;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public interface BotService {
    public UserModel login(String var1) throws SQLException, UnsupportedEncodingException, NoSuchAlgorithmException;

    public MoneyResponse addMoney(String var1, long var2, String var4, String var5);
}

