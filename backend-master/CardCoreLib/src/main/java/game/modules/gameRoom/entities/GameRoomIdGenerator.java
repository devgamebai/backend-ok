/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.CommonHandle
 *  bitzero.util.datacontroller.business.DataController
 */
package game.modules.gameRoom.entities;

import bitzero.util.common.business.CommonHandle;
import bitzero.util.datacontroller.business.DataController;
import game.utils.GameUtils;
import java.util.concurrent.atomic.AtomicInteger;

public class GameRoomIdGenerator {
    public AtomicInteger idGen = null;
    private static GameRoomIdGenerator ins = null;

    public static GameRoomIdGenerator instance() {
        if (ins == null) {
            ins = new GameRoomIdGenerator();
        }
        return ins;
    }

    private GameRoomIdGenerator() {
        this.loadFromDB();
    }

    public synchronized int getId() {
        int id = this.idGen.getAndIncrement();
        this.saveToDB();
        return id;
    }

    private synchronized void loadFromDB() {
        Integer lastId = null;
        try {
            lastId = (Integer)DataController.getController().get(this.getKey());
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
        }
        if (this.idGen == null) {
            int fromId = 1;
            if (lastId != null) {
                fromId = lastId;
            }
            this.idGen = new AtomicInteger(fromId);
            this.saveToDB();
        }
    }

    private void saveToDB() {
        int id = this.idGen.get();
        try {
            DataController.getController().set(this.getKey(), (Object)id);
        }
        catch (Exception e) {
            CommonHandle.writeErrLog((Throwable)e);
            CommonHandle.writeErrLog((String)("Error when save last game id to DB: " + id));
        }
    }

    private String getKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(GameUtils.gameName);
        sb.append(GameRoomIdGenerator.class.getSimpleName());
        return sb.toString();
    }
}

