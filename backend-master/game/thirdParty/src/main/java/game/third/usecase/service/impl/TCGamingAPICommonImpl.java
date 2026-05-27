/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  bitzero.util.common.business.Debug
 *  org.apache.log4j.Logger
 */
package game.third.usecase.service.impl;

import bitzero.util.common.business.Debug;
import game.third.usecase.core.common.DESEncrypt;
import game.third.usecase.core.common.HashUtil;
import game.third.usecase.core.common.HttpUtils;
import game.third.usecase.core.common.JsonUtil;
import game.third.usecase.core.obj.BalanceResponse;
import game.third.usecase.core.obj.CreateRegisterPlayerApiRequest;
import game.third.usecase.core.obj.FundTransactionInApiRequest;
import game.third.usecase.core.obj.FundTransactionOutAllApiRequest;
import game.third.usecase.core.obj.GetBalanceMemberApiRequest;
import game.third.usecase.core.obj.LaunchGameApiRequest;
import game.third.usecase.core.obj.LaunchGameResponse;
import game.third.usecase.core.obj.TCGBaseResponse;
import game.third.usecase.core.obj.TCGamingConfigObj;
import game.third.usecase.service.TCGamingAPICommon;
import game.third.usecase.service.exception.ProcessException;
import game.third.usecase.service.exception.RemoteException;
import game.third.usecase.service.exception.TransportException;
import java.net.URLEncoder;
import org.apache.log4j.Logger;

public class TCGamingAPICommonImpl
implements TCGamingAPICommon {
    private static final Logger logger = Logger.getLogger(TCGamingAPICommonImpl.class);
    private static final String UNKNOWN_TRANS_STATUS = "UNKNOWN";
    private TCGamingConfigObj configObj;

    public TCGamingAPICommonImpl(TCGamingConfigObj configObj) {
        this.configObj = configObj;
    }

    @Override
    public TCGBaseResponse registerMember(String username, String password, String currency) throws RemoteException, ProcessException, TransportException {
        CreateRegisterPlayerApiRequest request = new CreateRegisterPlayerApiRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setCurrency(currency);
        String json = JsonUtil.toJson(request);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse " + json});
        String result = this.doRequest(this.configObj, json);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse result" + result});
        TCGBaseResponse response = JsonUtil.fromJson(result, TCGBaseResponse.class);
        return response;
    }

    @Override
    public BalanceResponse getBalanceMember(String username, int productType) throws RemoteException, ProcessException, TransportException {
        GetBalanceMemberApiRequest request = new GetBalanceMemberApiRequest();
        request.setUsername(username);
        request.setProduct_type(productType);
        String json = JsonUtil.toJson(request);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse getBalanceMember" + json});
        String result = this.doRequest(this.configObj, json);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse getBalanceMember result" + result});
        BalanceResponse response = JsonUtil.fromJson(result, BalanceResponse.class);
        return response;
    }

    @Override
    public TCGBaseResponse fundTransferIn(String username, int productType, double amount, String transactionId) throws RemoteException, ProcessException, TransportException {
        FundTransactionInApiRequest request = new FundTransactionInApiRequest();
        request.setUsername(username);
        request.setProduct_type(productType);
        request.setAmount(amount);
        request.setReference_no(transactionId);
        String json = JsonUtil.toJson(request);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse fundTransferIn" + json});
        String result = this.doRequest(this.configObj, json);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse fundTransferIn result" + result});
        TCGBaseResponse response = JsonUtil.fromJson(result, TCGBaseResponse.class);
        return response;
    }

    @Override
    public TCGBaseResponse fundTransferOutAll(String username, int productType, String transactionId) throws RemoteException, ProcessException, TransportException {
        FundTransactionOutAllApiRequest request = new FundTransactionOutAllApiRequest();
        request.setUsername(username);
        request.setProduct_type(productType);
        request.setReference_no(transactionId);
        String json = JsonUtil.toJson(request);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse fundTransferOutAll" + json});
        String result = this.doRequest(this.configObj, json);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse fundTransferOutAll result" + result});
        TCGBaseResponse response = JsonUtil.fromJson(result, TCGBaseResponse.class);
        return response;
    }

    @Override
    public LaunchGameResponse launchGame(String username, int productType, String platform, String game_mode, String game_code) throws RemoteException, ProcessException, TransportException {
        LaunchGameApiRequest request = new LaunchGameApiRequest();
        request.setUsername(username);
        request.setProduct_type(productType);
        request.setPlatform(platform);
        request.setGame_mode(game_mode);
        request.setGame_code(game_code);
        String json = JsonUtil.toJson(request);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse launchGame" + json});
        String result = this.doRequest(this.configObj, json);
        Debug.trace((Object[])new Object[]{"TCGBaseResponse launchGame result" + result});
        LaunchGameResponse response = JsonUtil.fromJson(result, LaunchGameResponse.class);
        return response;
    }

    protected String doRequest(TCGamingConfigObj configObj, String json) {
        System.out.println("json :\n " + json);
        try {
            DESEncrypt des = new DESEncrypt(configObj.getDesKey());
            String encryptedParams = des.encrypt(json);
            String sign = HashUtil.sha256(encryptedParams + configObj.getSha256Key());
            String data = "merchant_code=" + URLEncoder.encode(configObj.getMerchantCode(), "UTF-8") + "&params=" + URLEncoder.encode(encryptedParams, "UTF-8") + "&sign=" + URLEncoder.encode(sign, "UTF-8");
            System.out.println("data :\n " + data);
            return HttpUtils.newPost(configObj.getApiUrl(), data).execute();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

