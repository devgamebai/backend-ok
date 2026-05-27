/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.response;

import game.third.usecase.game568win.response.ApiError;
import java.util.List;

public class GetGameListResult {
    private List<SeamlessGameProviderGame> seamlessGameProviderGames;
    private String serverId;
    private ApiError error;

    public List<SeamlessGameProviderGame> getSeamlessGameProviderGames() {
        return this.seamlessGameProviderGames;
    }

    public void setSeamlessGameProviderGames(List<SeamlessGameProviderGame> seamlessGameProviderGames) {
        this.seamlessGameProviderGames = seamlessGameProviderGames;
    }

    public String getServerId() {
        return this.serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public ApiError getError() {
        return this.error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    public static class SeamlessGameProviderGame {
        private int gameProviderId;
        private int gameID;
        private String gameType;
        private String newGameType;
        private int rank;
        private String device;
        private String platform;
        private String provider;
        private double rtp;
        private int rows;
        private int reels;
        private int lines;
        private List<String> gameInfos;
        private String language;
        private String gameName;
        private String gameIconUrl;
        private String[] supportedCurrencies;
        private String[] blockCountries;
        private boolean isMaintain;
        private boolean isEnabled;
        private ApiError error;
        private String serverId;
        private boolean isProvideCommission;
        private boolean hasHedgeBet;

        public int getGameProviderId() {
            return this.gameProviderId;
        }

        public void setGameProviderId(int gameProviderId) {
            this.gameProviderId = gameProviderId;
        }

        public int getGameID() {
            return this.gameID;
        }

        public void setGameID(int gameID) {
            this.gameID = gameID;
        }

        public String getGameType() {
            return this.gameType;
        }

        public void setGameType(String gameType) {
            this.gameType = gameType;
        }

        public String getNewGameType() {
            return this.newGameType;
        }

        public void setNewGameType(String newGameType) {
            this.newGameType = newGameType;
        }

        public int getRank() {
            return this.rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getDevice() {
            return this.device;
        }

        public void setDevice(String device) {
            this.device = device;
        }

        public String getPlatform() {
            return this.platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getProvider() {
            return this.provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public double getRtp() {
            return this.rtp;
        }

        public void setRtp(double rtp) {
            this.rtp = rtp;
        }

        public int getRows() {
            return this.rows;
        }

        public void setRows(int rows) {
            this.rows = rows;
        }

        public int getReels() {
            return this.reels;
        }

        public void setReels(int reels) {
            this.reels = reels;
        }

        public int getLines() {
            return this.lines;
        }

        public void setLines(int lines) {
            this.lines = lines;
        }

        public List<String> getGameInfos() {
            return this.gameInfos;
        }

        public void setGameInfos(List<String> gameInfos) {
            this.gameInfos = gameInfos;
        }

        public String getLanguage() {
            return this.language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getGameName() {
            return this.gameName;
        }

        public void setGameName(String gameName) {
            this.gameName = gameName;
        }

        public String getGameIconUrl() {
            return this.gameIconUrl;
        }

        public void setGameIconUrl(String gameIconUrl) {
            this.gameIconUrl = gameIconUrl;
        }

        public String[] getSupportedCurrencies() {
            return this.supportedCurrencies;
        }

        public void setSupportedCurrencies(String[] supportedCurrencies) {
            this.supportedCurrencies = supportedCurrencies;
        }

        public String[] getBlockCountries() {
            return this.blockCountries;
        }

        public void setBlockCountries(String[] blockCountries) {
            this.blockCountries = blockCountries;
        }

        public boolean isMaintain() {
            return this.isMaintain;
        }

        public void setMaintain(boolean maintain) {
            this.isMaintain = maintain;
        }

        public boolean isEnabled() {
            return this.isEnabled;
        }

        public void setEnabled(boolean enabled) {
            this.isEnabled = enabled;
        }

        public ApiError getError() {
            return this.error;
        }

        public void setError(ApiError error) {
            this.error = error;
        }

        public String getServerId() {
            return this.serverId;
        }

        public void setServerId(String serverId) {
            this.serverId = serverId;
        }

        public boolean isProvideCommission() {
            return this.isProvideCommission;
        }

        public void setProvideCommission(boolean provideCommission) {
            this.isProvideCommission = provideCommission;
        }

        public boolean isHasHedgeBet() {
            return this.hasHedgeBet;
        }

        public void setHasHedgeBet(boolean hasHedgeBet) {
            this.hasHedgeBet = hasHedgeBet;
        }
    }
}

