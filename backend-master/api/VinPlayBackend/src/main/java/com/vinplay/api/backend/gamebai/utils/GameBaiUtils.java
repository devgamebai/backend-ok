/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.dao.impl.GameTourDaoImpl
 *  com.vinplay.vbee.common.gencode.CodeGenerator
 */
package com.vinplay.api.backend.gamebai.utils;

import com.vinplay.usercore.dao.impl.GameTourDaoImpl;
import com.vinplay.vbee.common.gencode.CodeGenerator;
import java.sql.SQLException;
import java.util.Set;

public class GameBaiUtils {
    public static void init() throws SQLException {
        GameTourDaoImpl dao = new GameTourDaoImpl();
        Set allCode = dao.getAllCode();
        CodeGenerator.loadGeneratedcode((Set)allCode);
    }
}

