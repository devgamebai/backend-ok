/*
 * Decompiled with CFR 0.152.
 */
package game.third.hooks.peachTea.subProcessor;

import game.third.hooks.peachTea.response.BaseResponse;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.common.EncryptionUtil;
import game.third.usecase.peachtea.resoponse.BalanceResponse;
import game.third.usecase.peachtea.service.impl.PeachTeaServiceImpl;

public class GetUserBalanceSubProcessor {
    public String execute(String methodId, String queryString, String expectedChecksum) {
        BaseResponse baseResponse = new BaseResponse(false, 1009);
        String SHA256Key = ThirdPartyLoad.getPeachTeaConfig().getSha256Key();
        String secretKeyCfg = ThirdPartyLoad.getPeachTeaConfig().getSecretKey();
        String[] params = queryString.split("&");
        String secretKey = null;
        String timeString = null;
        String token = null;
        block10: for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length != 2) continue;
            switch (keyValue[0]) {
                case "Key": {
                    secretKey = keyValue[1];
                    continue block10;
                }
                case "Time": {
                    timeString = keyValue[1];
                    continue block10;
                }
                case "Token": {
                    token = keyValue[1];
                }
            }
        }
        if (token == null) {
            return baseResponse.toJson();
        }
        String checksum = EncryptionUtil.computeChecksum(queryString, SHA256Key, timeString, secretKeyCfg);
        if (!checksum.equals(expectedChecksum)) {
            return baseResponse.toJson();
        }
        PeachTeaServiceImpl peachTeaService = new PeachTeaServiceImpl();
        BalanceResponse balanceResponse = peachTeaService.getBalanceByToken(token);
        if (balanceResponse != null) {
            if (balanceResponse.getAccountBalance() > 0) {
                baseResponse.setDataJson(balanceResponse);
                baseResponse.setResult(true);
            } else {
                baseResponse.setErrorCode(1004);
            }
        } else {
            baseResponse.setMessage("Not found user");
            baseResponse.setErrorCode(1009);
        }
        return baseResponse.toJson();
    }
}

