package game.modules.XocDia.model.bet;

import com.vinplay.game.XocDia.history.XocDiaGamePlayHistoryDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XocDiaBetDoorItem {
    public Map<Long, BetItem> listBet;
    public List<XocDiaGamePlayHistoryDetail> historyBet;

    public XocDiaBetDoorItem() {
        this.listBet = new ConcurrentHashMap<Long, BetItem>();
        this.historyBet = new ArrayList<XocDiaGamePlayHistoryDetail>();
    }

    public void clear() {
        this.listBet.clear();
        this.historyBet.clear();
    }
}
