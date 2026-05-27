/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package com.vbee.security.firewall;

import com.vbee.security.config.FWConfig;
import com.vbee.security.enums.FWStatus;
import com.vbee.security.model.AgentIP;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

public class FirewallHttp {
    private ConcurrentHashMap<String, AgentIP> mapAgent = new ConcurrentHashMap();
    private Logger logger = Logger.getLogger((String)"csvRequest");

    public FWStatus checkRequest(String ip, String url) {
        FWStatus status = FWStatus.OPEN;
        long deltaTime = 0L;
        if (this.mapAgent.contains(ip)) {
            AgentIP agent = this.mapAgent.get(ip);
            long currentTime = System.currentTimeMillis();
            long delta = agent.getLastRequest() - currentTime;
            if (delta < FWConfig.RANGE_2_REQUEST) {
                status = FWStatus.CLOSE;
            } else {
                agent.setLastRequest(currentTime);
                this.mapAgent.put(ip, agent);
            }
        } else {
            AgentIP agent = new AgentIP();
            this.mapAgent.put(ip, agent);
        }
        this.logByStatus(ip, deltaTime, status, url);
        return status;
    }

    private void logByStatus(String IP, long deltaTime, FWStatus status, String url) {
        if (FWConfig.logLevel == 0L) {
            return;
        }
        if ((long)status.getId() > FWConfig.logLevel) {
            this.log(IP, deltaTime, status, url);
        }
    }

    private void log(String IP, long deltaTime, FWStatus status, String url) {
        StringBuilder sb = new StringBuilder();
        sb.append(",\t");
        sb.append(IP);
        sb.append(",\t");
        sb.append(deltaTime);
        sb.append(",\t");
        sb.append(status.getName());
        sb.append(",\t");
        sb.append(url);
        sb.append(",\t");
        this.logger.debug((Object)sb.toString());
    }
}

