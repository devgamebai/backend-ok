/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  bitzero.server.extensions.data.BaseMsg
 */
package game.modules.tour.control.cmd.send;

import bitzero.server.extensions.data.BaseMsg;
import java.nio.ByteBuffer;

public class SendRegisterTour
extends BaseMsg {
    public int tourId;
    public int timeBuyTicket;
    public int maxTimeBuyTicket;

    public SendRegisterTour() {
        super((short)5200);
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.put((byte)this.timeBuyTicket);
        bf.put((byte)this.maxTimeBuyTicket);
        bf.putInt(this.tourId);
        return this.packBuffer(bf);
    }

    public static enum ERR {
        SUCCES,
        UNDEFINED_ERROR,
        UN_EXIST,
        END_REGISTER,
        LIMIT_REBUY,
        LIMIT_LEVEL,
        CHARGE_MONEY_ERROR,
        REGISTERED_BUT_TOUR_NOT_STARTED,
        END_TIME,
        VIP_TOUR_TICKET_REQUIRED,
        PLAYING_OTHER_TOUR,
        UPCOMING_TOUR,
        FINAL_TABLE,
        GAME_MAIN_TAIN;
        

        private ERR() {
        }
    }

}

