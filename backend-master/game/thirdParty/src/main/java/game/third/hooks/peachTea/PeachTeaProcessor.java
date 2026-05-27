/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.servlet.http.HttpServletRequest
 */
package game.third.hooks.peachTea;

import game.third.hooks.peachTea.subProcessor.GetUserBalanceSubProcessor;
import game.third.hooks.peachTea.subProcessor.PlayerLeaveSubProcessor;
import game.third.hooks.peachTea.subProcessor.TransactionFishSubProcessor;
import game.third.processors.response.BaseResponse;
import game.third.usecase.config.ThirdPartyLoad;
import game.third.usecase.core.common.EncryptionUtil;
import game.third.usecase.core.hook.AbstractHookProcessor;
import game.third.usecase.core.hook.Param;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;

public class PeachTeaProcessor
extends AbstractHookProcessor<HttpServletRequest, String> {
    @Override
    public String execute(Param<HttpServletRequest> param) {
        BaseResponse baseResponse = new BaseResponse(false, "ServerError");
        String methodId = param.get().getParameter("methodId");
        String KMData = param.get().getParameter("KMData");
        String KMS = param.get().getParameter("KMS");
        try {
            String encryptionKey = ThirdPartyLoad.getPeachTeaConfig().getEncryptionKey();
            byte[] decodedKMData = Base64.getDecoder().decode(KMData);
            String queryString = EncryptionUtil.decryptDataWithAES(decodedKMData, encryptionKey);
            switch (methodId) {
                case "1": {
                    GetUserBalanceSubProcessor getBalanceSubProcessor = new GetUserBalanceSubProcessor();
                    return getBalanceSubProcessor.execute(methodId, queryString, KMS);
                }
                case "10": {
                    TransactionFishSubProcessor transactionFishSubProcessor = new TransactionFishSubProcessor();
                    return transactionFishSubProcessor.execute(methodId, queryString, KMS);
                }
                case "6": {
                    PlayerLeaveSubProcessor playerLeaveSubProcessor = new PlayerLeaveSubProcessor();
                    return playerLeaveSubProcessor.execute(methodId, queryString, KMS);
                }
            }
            return baseResponse.toJson();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

