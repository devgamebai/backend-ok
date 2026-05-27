/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.models.cache.UserCacheModel
 *  com.vinplay.vbee.common.pools.ConnectionPool
 */
package com.vinplay.usercore.service.impl;

import com.hazelcast.core.IMap;
import com.vinplay.usercore.service.ResetXuService;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.pools.ConnectionPool;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public class ResetXuServiceImpl
implements ResetXuService {
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int resetXuAllUsers() {
        int result = 0;
        IMap<String, UserModel> userMap = HazelcastClientFactory.getInstance().getMap("users");
        for (String username : userMap.keySet()) {
            try {
                 userMap.lock(username);
                UserCacheModel model = (UserCacheModel)userMap.get(username);
                if (model.isBot()) continue;
                long xu = model.getXu();
                long xuTotal = model.getXuTotal();
                long n = xuTotal - xu;
                long newXu = 0L;
                long newXuTotal = 0L;
                if (n >= 10000000L) {
                    newXu = 0L;
                    newXuTotal = n;
                } else {
                    newXuTotal = 10000000L;
                    newXu = newXuTotal - n;
                }
                model.setXu(newXu);
                model.setXuTotal(newXuTotal);
                 userMap.put(username, model);
            }
            catch (Exception e3) {
                result = 1;
            }
            finally {
                 userMap.unlock(username);
            }
        }
        Connection conn = ConnectionPool.getInstance().getConnection("mysqlpoolname");
        CallableStatement call = null;
        try {
            call = conn.prepareCall("CALL reset_xu()");
            call.execute();
        }
        catch (SQLException e) {
            result = 2;
            try {
                call.close();
                conn.close();
            }
            catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
        finally {
            try {
                call.close();
                conn.close();
            }
            catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }
}

