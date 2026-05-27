/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.bacay.server.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.bacay.server.GamePlayer;
import game.bacay.server.logic.GroupCard;
import game.bacay.server.sPlayerInfo;
import game.bacay.server.sResultInfo;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SendEndGame
extends BaseMsg {
    public sResultInfo result;
    public byte[] playerStatus = new byte[8];
    public GamePlayer[] gamePlayers = new GamePlayer[8];
    public List<Long> tongThangDatCuoc = new ArrayList<Long>();
    public List<Long> tongThangDanhBien = new ArrayList<Long>();
    public List<Long> tongThangKeCua = new ArrayList<Long>();
    public List<Long> tongCuocGa = new ArrayList<Long>();
    public List<Long> tongKetThangThua = new ArrayList<Long>();
    public List<Long> currentMoneyList = new ArrayList<Long>();

    public SendEndGame() {
        super((short)3103);
    }

    public void copyData(SendEndGame msg) {
        this.result = msg.result;
        this.playerStatus = msg.playerStatus;
        this.gamePlayers = msg.gamePlayers;
        this.tongThangDatCuoc = msg.tongThangDatCuoc;
        this.tongThangDanhBien = msg.tongThangDanhBien;
        this.tongThangKeCua = msg.tongThangKeCua;
        this.tongCuocGa = msg.tongCuocGa;
        this.tongKetThangThua = msg.tongKetThangThua;
        this.currentMoneyList = msg.currentMoneyList;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        this.putByteArray(bf, this.playerStatus);
        for (int i = 0; i < this.gamePlayers.length; ++i) {
            GamePlayer gp = this.gamePlayers[i];
            if (gp == null) continue;
            this.putByteArray(bf, gp.spInfo.handCards.toByteArray());
        }
        bf.putLong(this.result.tienThangChuong);
        bf.putLong(this.result.tienThangGa);
        this.putLongArray(bf, this.result.tienThangKeCua);
        this.putLongArray(bf, this.result.tienThangBien);
        bf.putLong(this.result.tongTienCuoiVan);
        this.putLongArray(bf, this.convert(this.tongThangDatCuoc));
        this.putLongArray(bf, this.convert(this.tongThangDanhBien));
        this.putLongArray(bf, this.convert(this.tongThangKeCua));
        this.putLongArray(bf, this.convert(this.tongCuocGa));
        this.putLongArray(bf, this.convert(this.tongKetThangThua));
        this.putLongArray(bf, this.convert(this.currentMoneyList));
        bf.put((byte)12);
        return this.packBuffer(bf);
    }

    public long[] convert(List<Long> list) {
        long[] d = new long[list.size()];
        for (int i = 0; i < list.size(); ++i) {
            d[i] = list.get(i);
        }
        return d;
    }
}

