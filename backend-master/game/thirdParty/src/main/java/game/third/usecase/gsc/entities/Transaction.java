/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.usecase.gsc.entities;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Transaction {
    private String id;
    private String action;
    private String amount;
    private String valid_bet_amount;
    private String bet_amount;
    private String prize_amount;
    private String tip_amount;
    private String wager_code;
    private String wager_status;
    private Payload payload;
    private long settled_at;
    private int product_code;
    private String game_code;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return this.action;
    }

    public String action() {
        switch (this.action.toLowerCase()) {
            case "bet": {
                return "BET";
            }
            case "freebet": {
                return "FREEBET";
            }
            case "settled": {
                return "SETTLED";
            }
            case "rollback": {
                return "ROLLBACK";
            }
            case "cancel": {
                return "CANCEL";
            }
            case "adjustment": {
                return "ADJUSTMENT";
            }
            case "jackpot": {
                return "JACKPOT";
            }
            case "bonus": {
                return "BONUS";
            }
            case "tip": {
                return "TIP";
            }
            case "promo": {
                return "PROMO";
            }
            case "leaderboard": {
                return "LEADERBOARD";
            }
            case "bet_preserve": {
                return "BET_PRESERVE";
            }
            case "preserve_refund": {
                return "PRESERVE_REFUND";
            }
        }
        throw new IllegalArgumentException("Unknown action: " + this.action);
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAmount() {
        return this.amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getValid_bet_amount() {
        return this.valid_bet_amount;
    }

    public void setValid_bet_amount(String valid_bet_amount) {
        this.valid_bet_amount = valid_bet_amount;
    }

    public String getBet_amount() {
        return this.bet_amount;
    }

    public void setBet_amount(String bet_amount) {
        this.bet_amount = bet_amount;
    }

    public String getPrize_amount() {
        return this.prize_amount;
    }

    public void setPrize_amount(String prize_amount) {
        this.prize_amount = prize_amount;
    }

    public String getTip_amount() {
        return this.tip_amount;
    }

    public void setTip_amount(String tip_amount) {
        this.tip_amount = tip_amount;
    }

    public String getWager_code() {
        return this.wager_code;
    }

    public void setWager_code(String wager_code) {
        this.wager_code = wager_code;
    }

    public String getWager_status() {
        return this.wager_status;
    }

    public void setWager_status(String wager_status) {
        this.wager_status = wager_status;
    }

    public Payload getPayload() {
        return this.payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public long getSettled_at() {
        return this.settled_at;
    }

    public void setSettled_at(long settled_at) {
        this.settled_at = settled_at;
    }

    public int getProduct_code() {
        return this.product_code;
    }

    public void setProduct_code(int product_code) {
        this.product_code = product_code;
    }

    public String getGame_code() {
        return this.game_code;
    }

    public void setGame_code(String game_code) {
        this.game_code = game_code;
    }

    public static Transaction fromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return (Transaction)objectMapper.readValue(json, Transaction.class);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Payload {
        private long bet_id;
        private String parent_bet_id;
        private String platform;
        private String provider_tx_id;

        public long getBet_id() {
            return this.bet_id;
        }

        public void setBet_id(long bet_id) {
            this.bet_id = bet_id;
        }

        public String getParent_bet_id() {
            return this.parent_bet_id;
        }

        public void setParent_bet_id(String parent_bet_id) {
            this.parent_bet_id = parent_bet_id;
        }

        public String getPlatform() {
            return this.platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getProvider_tx_id() {
            return this.provider_tx_id;
        }

        public void setProvider_tx_id(String provider_tx_id) {
            this.provider_tx_id = provider_tx_id;
        }
    }
}

