/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dal.entities.log;

import com.vinplay.dal.entities.log.LogMoneyUserNapTieuVinModel;
import java.io.Serializable;
import java.util.Date;

public class LogMoneyUserVinModel
extends LogMoneyUserNapTieuVinModel
implements Serializable {
    private Boolean is_bot;
    private Boolean play_game;

    public LogMoneyUserVinModel() {
    }

    public LogMoneyUserVinModel(Long trans_id, Integer user_id, String nick_name, String service_name, Long current_money, Long money_exchange, String description, String trans_time, String action_name, Long fee, Date create_time, Boolean is_bot, Boolean play_game) {
        super(trans_id, user_id, nick_name, service_name, current_money, money_exchange, description, trans_time, action_name, fee, create_time);
        this.is_bot = is_bot;
        this.play_game = play_game;
    }

    public Boolean getIs_bot() {
        return this.is_bot;
    }

    public void setIs_bot(Boolean is_bot) {
        this.is_bot = is_bot;
    }

    public Boolean getPlay_game() {
        return this.play_game;
    }

    public void setPlay_game(Boolean play_game) {
        this.play_game = play_game;
    }
}

