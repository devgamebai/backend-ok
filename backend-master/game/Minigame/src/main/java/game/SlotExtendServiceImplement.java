/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hazelcast.core.HazelcastInstance
 *  com.hazelcast.core.IMap
 *  com.vinplay.dal.dao.PokeGoDAO
 *  com.vinplay.vbee.common.hazelcast.HazelcastClientFactory
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage
 *  com.vinplay.vbee.common.models.cache.TopPokeGoModel
 *  com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo
 *  com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo
 *  com.vinplay.vbee.common.rmq.RMQApi
 */
package game;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.PokeGoDAO;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.minigame.pokego.LogPokeGoMessage;
import com.vinplay.vbee.common.models.cache.TopPokeGoModel;
import com.vinplay.vbee.common.models.minigame.pokego.LSGDPokeGo;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.models.minigame.pokego.TopPokeGo;
import game.SlotExtendDaoImpl;
import game.SlotExtendService;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class SlotExtendServiceImplement
implements SlotExtendService {
    private PokeGoDAO dao = new SlotExtendDaoImpl();

    @Override
    public void logSlotExtend(long referenceId, String username, long betValue, String linesBetting, String linesWin, String prizesOnLine, short result, long totalPrizes, short moneyType, String time) throws IOException, TimeoutException, InterruptedException {
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
        MessageBusFactory.get("queue_slotExtend").publish("queue_slotExtend", (BaseMessage)message, 135);
    }

    @Override
    public int countLSDG(String username, int moneyType) {
        return this.dao.countLSGD(username, moneyType);
    }

    @Override
    public List<LSGDPokeGo> getLSGD(String username, int pageNumber, int moneyType) {
        return this.dao.getLSGD(username, moneyType, pageNumber);
    }

    @Override
    public void addTop(String username, int betValue, long totalPrizes, int moneyType, String time, int result) throws IOException, TimeoutException, InterruptedException {
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap topMap = client.getMap("cacheTop");
        TopPokeGoModel topPokeGo = (TopPokeGoModel)topMap.get(("SlotExtend_" + moneyType));
        if (topPokeGo == null) {
            topPokeGo = new TopPokeGoModel();
        }
        TopPokeGo entry = new TopPokeGo();
        entry.un = username;
        entry.bv = betValue;
        entry.pz = totalPrizes;
        entry.ts = time;
        entry.rs = result;
        topPokeGo.put(entry);
        topMap.put(("SlotExtend_" + moneyType), topPokeGo);
    }

    @Override
    public List<TopPokeGo> getTopSlotExtend(int moneyType, int page) {
        if (page <= 10) {
            HazelcastInstance client = HazelcastClientFactory.getInstance();
            IMap topMap = client.getMap("cacheTop");
            TopPokeGoModel topPokeGo = (TopPokeGoModel)topMap.get(("SlotExtend_" + moneyType));
            if (topPokeGo == null) {
                topPokeGo = new TopPokeGoModel();
            }
            if (topPokeGo.getResults().size() == 0) {
                List results = this.dao.getTop(moneyType, 100);
                topPokeGo.setResults(results);
                topMap.put(("SlotExtend_" + moneyType), topPokeGo);
            }
            return topPokeGo.getResults(page, 10);
        }
        return this.dao.getTopPokeGo(moneyType, page);
    }

    @Override
    public long getLastReferenceId() {
        return this.dao.getLastRefenceId();
    }
}

