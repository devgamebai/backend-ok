/*
 * Decompiled with CFR 0_116.
 */
package game.modules.gameRoom.entities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockingData {
    private static BlockingData ins = null;
    public Map<String, Long> inviteBlocks = new ConcurrentHashMap<String, Long>();

    public static BlockingData instance() {
        if (ins == null) {
            ins = new BlockingData();
        }
        return ins;
    }

    public void addSpamInvite(String nickName) {
        this.inviteBlocks.put(nickName, System.currentTimeMillis());
    }

    public boolean preventSpamInvite(String nickName) {
        Long timeBlock = this.inviteBlocks.get(nickName);
        if (timeBlock == null) {
            return false;
        }
        long inteval = System.currentTimeMillis() - timeBlock;
        if (inteval > 1800000) {
            this.inviteBlocks.remove(nickName);
            return false;
        }
        return true;
    }
}

