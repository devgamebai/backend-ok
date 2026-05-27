/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.vinplay.vbee.common.cp.BaseProcessor
 *  com.vinplay.vbee.common.cp.Param
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.response.BaseResponse
 *  javax.servlet.http.HttpServletRequest
 */
package com.vinplay.api.processors;

import com.hazelcast.core.HazelcastInstance;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.response.BaseResponse;
import javax.servlet.http.HttpServletRequest;

public class DestroyHazelcastProcessor
implements BaseProcessor<HttpServletRequest, String> {
    public String execute(Param<HttpServletRequest> param) {
        try {
            HazelcastInstance hazelcast = HazelcastClientFactory.getInstance();
            hazelcast.getDistributedObjects().forEach(distributedObject -> distributedObject.destroy());
            return new BaseResponse().success(null);
        }
        catch (Exception e) {
            e.printStackTrace();
            return BaseResponse.error((String)"1001", (String)e.getMessage());
        }
    }
}

