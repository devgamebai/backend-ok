/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 */
package game.third.usecase.gsc.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class GSCGameProviderResponse
implements Serializable {
    private int code;
    private String message;
    @SerializedName(value="provider_games")
    private List<ProviderGame> providerGames;
    private Pagination pagination;

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ProviderGame> getProviderGames() {
        return this.providerGames;
    }

    public void setProviderGames(List<ProviderGame> providerGames) {
        this.providerGames = providerGames;
    }

    public Pagination getPagination() {
        return this.pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public static class Pagination
    implements Serializable {
        private int size;
        private int offset;
        private String total;

        public int getSize() {
            return this.size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getOffset() {
            return this.offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public String getTotal() {
            return this.total;
        }

        public int getIntTotal() {
            return Integer.parseInt(this.total);
        }

        public void setTotal(String total) {
            this.total = total;
        }
    }

    public static class ProviderGame
    implements Serializable {
        @SerializedName(value="game_code")
        private String gameCode;
        @SerializedName(value="game_name")
        private String gameName;
        @SerializedName(value="game_type")
        private String gameType;
        @SerializedName(value="product_id")
        private int productId;
        @SerializedName(value="product_code")
        private int productCode;
        @SerializedName(value="support_currency")
        private String supportCurrency;
        @SerializedName(value="status")
        private String status;
        @SerializedName(value="image_url")
        private String imageUrl;

        public String getGameCode() {
            return this.gameCode;
        }

        public void setGameCode(String gameCode) {
            this.gameCode = gameCode;
        }

        public String getGameName() {
            return this.gameName;
        }

        public void setGameName(String gameName) {
            this.gameName = gameName;
        }

        public String getGameType() {
            return this.gameType;
        }

        public void setGameType(String gameType) {
            this.gameType = gameType;
        }

        public int getProductId() {
            return this.productId;
        }

        public void setProductId(int productId) {
            this.productId = productId;
        }

        public int getProductCode() {
            return this.productCode;
        }

        public void setProductCode(int productCode) {
            this.productCode = productCode;
        }

        public String getSupportCurrency() {
            return this.supportCurrency;
        }

        public void setSupportCurrency(String supportCurrency) {
            this.supportCurrency = supportCurrency;
        }

        public String getStatus() {
            return this.status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getImageUrl() {
            return this.imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}

