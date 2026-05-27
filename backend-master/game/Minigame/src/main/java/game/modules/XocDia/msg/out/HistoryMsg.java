package game.modules.XocDia.msg.out;

import com.vinplay.game.XocDia.XocDiaHistoryItem;
import com.vinplay.game.XocDia.XocDiaHistoryModel;
import game.BaseMsgEx;
import game.modules.XocDia.GameXocDiaCmdDefine;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Response: game history (cmd 8012)
 * Returns last N rounds of dice results
 */
public class HistoryMsg extends BaseMsgEx {
    public XocDiaHistoryModel historyModel;

    public HistoryMsg() {
        super(GameXocDiaCmdDefine.HISTORY);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        if (this.historyModel != null && this.historyModel.data != null) {
            List<XocDiaHistoryItem> items = this.historyModel.data;
            bf.putInt(items.size());
            for (int i = 0; i < items.size(); i++) {
                XocDiaHistoryItem item = items.get(i);
                bf.putLong(item.gamePlayId);
                bf.putInt(item.data.length);
                for (int j = 0; j < item.data.length; j++) {
                    bf.put(item.data[j]);
                }
            }
        } else {
            bf.putInt(0);
        }
        return this.packBuffer(bf);
    }
}
