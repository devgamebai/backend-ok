/*
 * Decompiled with CFR 0.152.
 */
package game.third.hooks.peachTea.subProcessor;

import game.third.processors.response.BaseResponse;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.common.EncryptionUtil;

public class TransactionFishSubProcessor {
    public String execute(String methodId, String queryString, String expectedChecksum) {
        String[] params;
        BaseResponse baseResponse = new BaseResponse(false, "ServerError");
        String SHA256Key = ThirdPartyLoad.getPeachTeaConfig().getSha256Key();
        String secretKeyCfg = ThirdPartyLoad.getPeachTeaConfig().getSecretKey();
        String txId = null;
        String nickname = null;
        String token = null;
        String fishType = null;
        String totalBet = null;
        String totalReward = null;
        String timeStamp = null;
        String gameId = null;
        block20: for (String param : params = queryString.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length != 2) continue;
            switch (keyValue[0]) {
                case "TxId": {
                    txId = keyValue[1];
                    continue block20;
                }
                case "Nickname": {
                    nickname = keyValue[1];
                    continue block20;
                }
                case "Token": {
                    token = keyValue[1];
                    continue block20;
                }
                case "FishType": {
                    fishType = keyValue[1];
                    continue block20;
                }
                case "TotalBet": {
                    totalBet = keyValue[1];
                    continue block20;
                }
                case "TotalReward": {
                    totalReward = keyValue[1];
                    continue block20;
                }
                case "TimeStamp": {
                    timeStamp = keyValue[1];
                    continue block20;
                }
                case "GameId": {
                    gameId = keyValue[1];
                }
            }
        }
        String checksum = EncryptionUtil.computeChecksum(queryString, SHA256Key, secretKeyCfg);
        if (!checksum.equals(expectedChecksum)) {
            return baseResponse.toJson();
        }
        return baseResponse.toJson();
    }
}

