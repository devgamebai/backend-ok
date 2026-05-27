/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.GroupType
 */
package game.modules.minigame.config;

import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.GroupType;
import java.util.ArrayList;
import java.util.List;

public class MinipokerResultData {
    public List<Card> cards = new ArrayList<Card>();
    public boolean isJackPot = false;
    public GroupType groupType;
    public short result = 0;
    public long moneyWin = 0L;
}

