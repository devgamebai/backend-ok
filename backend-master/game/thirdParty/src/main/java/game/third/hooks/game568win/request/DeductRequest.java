/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package game.third.hooks.game568win.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.third.usecase.game568win.entities.Status;
import game.third.usecase.game568win.entities.TransactionGame568Win;
import game.third.usecase.game568win.model.ExtraInfo;
import game.third.usecase.game568win.model.SeamlessGameExtraInfo;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeductRequest {
    @JsonProperty(value="Amount")
    private double amount;
    @JsonProperty(value="TransferCode")
    private String transferCode;
    @JsonProperty(value="TransactionId")
    private String transactionId;
    @JsonProperty(value="BetTime")
    private String betTime;
    @JsonProperty(value="GameRoundId")
    private String gameRoundId;
    @JsonProperty(value="GamePeriodId")
    private String gamePeriodId;
    @JsonProperty(value="OrderDetail")
    private String orderDetail;
    @JsonProperty(value="PlayerIp")
    private String playerIp;
    @JsonProperty(value="GameTypeName")
    private String gameTypeName;
    @JsonProperty(value="CompanyKey")
    private String companyKey;
    @JsonProperty(value="Username")
    private String username;
    @JsonProperty(value="ProductType")
    private int productType;
    @JsonProperty(value="GameType")
    private int gameType;
    @JsonProperty(value="GameId")
    private int gameId;
    @JsonProperty(value="Gpid")
    private int gpid;
    @JsonProperty(value="ExtraInfo")
    private ExtraInfo extraInfo;
    @JsonProperty(value="SeamlessGameExtraInfo")
    private SeamlessGameExtraInfo seamlessGameExtraInfo;

    public static DeductRequest fromJson(String string) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return (DeductRequest)objectMapper.readValue(string, DeductRequest.class);
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransferCode() {
        return this.transferCode;
    }

    public void setTransferCode(String transferCode) {
        this.transferCode = transferCode;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getBetTime() {
        return this.betTime;
    }

    public void setBetTime(String betTime) {
        this.betTime = betTime;
    }

    public String getGameRoundId() {
        return this.gameRoundId;
    }

    public void setGameRoundId(String gameRoundId) {
        this.gameRoundId = gameRoundId;
    }

    public String getGamePeriodId() {
        return this.gamePeriodId;
    }

    public void setGamePeriodId(String gamePeriodId) {
        this.gamePeriodId = gamePeriodId;
    }

    public String getOrderDetail() {
        return this.orderDetail;
    }

    public void setOrderDetail(String orderDetail) {
        this.orderDetail = orderDetail;
    }

    public String getPlayerIp() {
        return this.playerIp;
    }

    public void setPlayerIp(String playerIp) {
        this.playerIp = playerIp;
    }

    public String getGameTypeName() {
        return this.gameTypeName;
    }

    public void setGameTypeName(String gameTypeName) {
        this.gameTypeName = gameTypeName;
    }

    public String getCompanyKey() {
        return this.companyKey;
    }

    public void setCompanyKey(String companyKey) {
        this.companyKey = companyKey;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getProductType() {
        return this.productType;
    }

    public void setProductType(int productType) {
        this.productType = productType;
    }

    public int getGameType() {
        return this.gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getGameId() {
        return this.gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getGpid() {
        return this.gpid;
    }

    public void setGpid(int gpid) {
        this.gpid = gpid;
    }

    public ExtraInfo getExtraInfo() {
        return this.extraInfo;
    }

    public void setExtraInfo(ExtraInfo extraInfo) {
        this.extraInfo = extraInfo;
    }

    public SeamlessGameExtraInfo getSeamlessGameExtraInfo() {
        return this.seamlessGameExtraInfo;
    }

    public void setSeamlessGameExtraInfo(SeamlessGameExtraInfo seamlessGameExtraInfo) {
        this.seamlessGameExtraInfo = seamlessGameExtraInfo;
    }

    public static boolean validate(DeductRequest deductRequest) {
        if (deductRequest.getCompanyKey() == null || deductRequest.getCompanyKey().isEmpty()) {
            return false;
        }
        String username = deductRequest.getUsername();
        if (username == null || username.isEmpty() || username.length() > 20 || !username.matches("[a-zA-Z0-9_]+")) {
            return false;
        }
        if (deductRequest.getProductType() <= 0) {
            return false;
        }
        if (deductRequest.getGameType() <= 0) {
            return false;
        }
        if (deductRequest.getAmount() <= 0.0) {
            return false;
        }
        if (deductRequest.getTransferCode() == null || deductRequest.getTransferCode().isEmpty()) {
            return false;
        }
        if (deductRequest.getTransactionId() == null || deductRequest.getTransactionId().isEmpty()) {
            return false;
        }
        return deductRequest.getBetTime() != null;
    }

    public TransactionGame568Win getTransaction() {
        TransactionGame568Win transaction = new TransactionGame568Win();
        transaction.setAmount(this.amount);
        transaction.setTransferCode(this.transferCode);
        transaction.setTransactionId(this.transactionId);
        transaction.setBetTime(LocalDateTime.parse(this.betTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        transaction.setGameRoundId(this.gameRoundId);
        transaction.setGamePeriodId(this.gamePeriodId);
        transaction.setOrderDetail(this.orderDetail);
        transaction.setPlayerIp(this.playerIp);
        transaction.setGameTypeName(this.gameTypeName);
        transaction.setCompanyKey(this.companyKey);
        transaction.setUsername(this.username);
        transaction.setProductType(this.productType);
        transaction.setGameType(this.gameType);
        transaction.setGameId(this.gameId);
        transaction.setGpid(this.gpid);
        transaction.setExtraInfo(this.extraInfo);
        transaction.setSeamlessGameExtraInfo(this.seamlessGameExtraInfo);
        transaction.setStatus(Status.Running);
        return transaction;
    }
}

