/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.vbee.common.enums.Games
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage
 *  com.vinplay.vbee.common.models.cache.TopGalaxyModel
 *  com.vinplay.vbee.common.models.minigame.galaxy.LSGDGalaxy
 *  com.vinplay.vbee.common.models.minigame.galaxy.TopGalaxy
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package com.vinplay.dal.service.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.GalaxyDAO;
import com.vinplay.dal.dao.impl.GalaxyDAOImpl;
import com.vinplay.dal.service.GalaxyService;
import com.vinplay.vbee.common.enums.Games;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage;
import com.vinplay.vbee.common.models.cache.TopGalaxyModel;
import com.vinplay.vbee.common.models.minigame.galaxy.LSGDGalaxy;
import com.vinplay.vbee.common.models.minigame.galaxy.TopGalaxy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class GalaxyServiceImpl
implements GalaxyService {
    private GalaxyDAO dao = new GalaxyDAOImpl();

    // P1: Body intentionally retained (with no publish call) so F1–F5
    // producer migration is a 1-line mechanical swap (publish → MessageBus.publish).
    // See docs/RMQ_TO_REDIS_STREAMS_MIGRATION_PLAN.md.
    @Override
    public void logGalaxy(long referenceId, String username, long betValue, String linesBetting, String linesWin, String prizesOnLine, short result, long totalPrizes, short moneyType, String time) throws IOException, TimeoutException, InterruptedException {
        LogPokeGoMessage message = new LogPokeGoMessage();
        message.referenceId = referenceId;
        message.username = username;
        message.betValue = betValue;
        message.linesBetting = linesBetting;
        message.linesWin = linesWin;
        message.prizesOnLine = prizesOnLine;
        message.result = result;
        message.totalPrizes = totalPrizes;
        message.moneyType = moneyType;
        message.time = time;
        // P1 cleanup: queue_galaxy retired (no consumer); LogGalaxyProcessor deleted.
    }

    @Override
    public int countLSDG(String username, int moneyType) {
        return this.dao.countLSGD(username, moneyType);
    }

    @Override
    public List<LSGDGalaxy> getLSGD(String username, int pageNumber, int moneyType) {
        return this.dao.getLSGD(username, moneyType, pageNumber);
    }

    @Override
    public void addTop(String username, int betValue, long totalPrizes, int moneyType, String time, int result) throws IOException, TimeoutException, InterruptedException {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap topMap = client.getMap("cacheTop");
        TopGalaxyModel topPokeGo = (TopGalaxyModel)topMap.get((Games.GALAXY.getName() + "_" + moneyType));
        if (topPokeGo == null) {
            topPokeGo = new TopGalaxyModel();
        }
        TopGalaxy entry = new TopGalaxy();
        entry.un = username;
        entry.bv = betValue;
        entry.pz = totalPrizes;
        entry.ts = time;
        entry.rs = result;
        topPokeGo.put(entry);
        topMap.put((Games.GALAXY.getName() + "_" + moneyType), topPokeGo);
    }

    @Override
    public List<TopGalaxy> getTopGalaxy(int moneyType, int page) {
        if (page <= 10) {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap topMap = client.getMap("cacheTop");
            TopGalaxyModel topPokeGo = (TopGalaxyModel)topMap.get((Games.GALAXY.getName() + "_" + moneyType));
            if (topPokeGo == null) {
                topPokeGo = new TopGalaxyModel();
            }
            if (topPokeGo.getResults().size() == 0) {
                List<TopGalaxy> results = this.dao.getTop(moneyType, 100);
                topPokeGo.setResults(results);
                topMap.put((Games.GALAXY.getName() + "_" + moneyType), topPokeGo);
            }
            return topPokeGo.getResults(page, 10);
        }
        return this.dao.getTopGalaxy(moneyType, page);
    }

    @Override
    public long getLastReferenceId() {
        return this.dao.getLastRefenceId();
    }
}

