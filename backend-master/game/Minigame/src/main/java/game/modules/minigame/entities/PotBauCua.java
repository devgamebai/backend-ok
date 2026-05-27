/*
 * Decompiled with CFR 0.144.
 */
package game.modules.minigame.entities;

import game.modules.minigame.entities.Pot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PotBauCua
extends Pot {
    public PotType type;
    public Map<String, Long> contributors = new HashMap<String, Long>();
    public List<String> users = new ArrayList<String>();

    public PotBauCua(int id) {
        this.type = PotType.findPotType((byte)id);
    }

    public void bet(String username, long betValue) {
        if (this.contributors.containsKey(username)) {
            long currentBetValue = this.contributors.get(username);
            this.contributors.put(username, currentBetValue + betValue);
        } else {
            this.contributors.put(username, betValue);
        }
        this.totalValue += betValue;
    }

    @Override
    public void renew() {
        super.renew();
        this.contributors.clear();
        this.users.clear();
    }

    public long getTotalBetByUsername(String username) {
        long totalValue = 0L;
        if (this.contributors.containsKey(username)) {
            totalValue = this.contributors.get(username);
        }
        return totalValue;
    }

    public static enum PotType {
        BAU(0),
        CUA(1),
        TOM(2),
        CA(3),
        GA(4),
        HUOU(5);
        
        public byte id;

        private PotType(int id) {
            this.id = (byte)id;
        }

        public static PotType findPotType(byte id) {
            for (PotType entry : PotType.values()) {
                if (entry.id != id) continue;
                return entry;
            }
            return null;
        }
    }

}

