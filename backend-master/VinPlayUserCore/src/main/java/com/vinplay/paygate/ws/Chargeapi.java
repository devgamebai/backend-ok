/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.paygate.ws;

import com.vinplay.paygate.ws.CardResp;
import com.vinplay.paygate.ws.ChargeRespBean;
import com.vinplay.paygate.ws.LoginRespBean;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Action;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name="chargeapi", targetNamespace="http://ws.payapi/")
public interface Chargeapi {
    @WebMethod
    @WebResult(targetNamespace="")
    @RequestWrapper(localName="topup", targetNamespace="http://ws.payapi/", className="sample.ws.Topup")
    @ResponseWrapper(localName="topupResponse", targetNamespace="http://ws.payapi/", className="sample.ws.TopupResponse")
    @Action(input="http://ws.payapi/chargeapi/topupRequest", output="http://ws.payapi/chargeapi/topupResponse")
    public CardResp topup(@WebParam(name = "transactionId", targetNamespace = "") String var1, @WebParam(name = "userName", targetNamespace = "") String var2, @WebParam(name = "partner_id", targetNamespace = "") String var3, @WebParam(name = "mPin", targetNamespace = "") String var4, @WebParam(name = "cardData", targetNamespace = "") String var5, @WebParam(name = "md5SessionId", targetNamespace = "") String var6);

    @WebMethod
    @WebResult(targetNamespace="")
    @RequestWrapper(localName="charge", targetNamespace="http://ws.payapi/", className="sample.ws.Charge")
    @ResponseWrapper(localName="chargeResponse", targetNamespace="http://ws.payapi/", className="sample.ws.ChargeResponse")
    @Action(input="http://ws.payapi/chargeapi/chargeRequest", output="http://ws.payapi/chargeapi/chargeResponse")
    public ChargeRespBean charge(@WebParam(name = "transactionId", targetNamespace = "") String var1, @WebParam(name = "userName", targetNamespace = "") String var2, @WebParam(name = "partner_id", targetNamespace = "") String var3, @WebParam(name = "mPin", targetNamespace = "") String var4, @WebParam(name = "cardData", targetNamespace = "") String var5, @WebParam(name = "md5SessionId", targetNamespace = "") String var6);

    @WebMethod
    @WebResult(targetNamespace="")
    @RequestWrapper(localName="login", targetNamespace="http://ws.payapi/", className="sample.ws.Login")
    @ResponseWrapper(localName="loginResponse", targetNamespace="http://ws.payapi/", className="sample.ws.LoginResponse")
    @Action(input="http://ws.payapi/chargeapi/loginRequest", output="http://ws.payapi/chargeapi/loginResponse")
    public LoginRespBean login(@WebParam(name = "user_name", targetNamespace = "") String var1, @WebParam(name = "encode_password", targetNamespace = "") String var2, @WebParam(name = "partner_id", targetNamespace = "") String var3);
}

