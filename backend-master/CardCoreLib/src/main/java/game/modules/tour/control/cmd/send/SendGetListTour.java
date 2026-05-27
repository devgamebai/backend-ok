/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import game.modules.tour.control.Tour;
import game.modules.tour.control.TourSetting;
import java.nio.ByteBuffer;
import java.util.List;

public class SendGetListTour
extends BaseMsg {
    public List<Tour> playingAndWaitingTours = null;

    public SendGetListTour() {
        super((short)5201);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        int tourSize = this.playingAndWaitingTours.size();
        bf.putShort((short)tourSize);
        for (int i = 0; i < tourSize; ++i) {
            Tour tour = this.playingAndWaitingTours.get(i);
            bf.put((byte)tour.index);
            bf.putInt(tour.tourId);
            bf.put((byte)tour.tourState);
            bf.put((byte)tour.startHour);
            bf.put((byte)tour.startMinute);
            bf.putInt(tour.ticket);
            bf.putShort((short)tour.playingCount());
            bf.putShort((short)tour.totalCount());
            bf.putInt(tour.prizePool);
            bf.putShort((short)tour.tourSetting.chip);
            bf.putShort((short)tour.tourSetting.timeLevelUp);
            bf.putShort((short)tour.tourSetting.levels[1]);
            bf.put((byte)tour.endRegisterHour);
            bf.put((byte)tour.endRegisterMinute);
            bf.put((byte)tour.tourType);
            bf.putInt(tour.getCountDownToStart());
        }
        return this.packBuffer(bf);
    }
}

