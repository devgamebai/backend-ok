/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.brandname.dao.impl.BrandnameDaoImpl
 *  com.vinplay.usercore.utils.GameCommon
 *  com.vinplay.vbee.common.messages.BaseMessage
 *  com.vinplay.vbee.common.messages.BrandnameDLVRMessage
 *  com.vinplay.vbee.common.rmq.RMQApi
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.brandname;

import com.vinplay.brandname.dao.impl.BrandnameDaoImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.BrandnameDLVRMessage;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.apache.log4j.Logger;

@WebService(name="Service", serviceName="Service", portName="ServiceSoap", targetNamespace="http://wspay.xeng.club/")
@SOAPBinding(style=SOAPBinding.Style.RPC)
public class Service {
    private static final Logger logger = Logger.getLogger(Service.class);

    @WebMethod
    public String getdlvr(@WebParam(name="RequestId") String requestId, @WebParam(name="SmsStatus") Integer smsStatus, @WebParam(name="Count") Integer count, @WebParam(name="StatusDesc") String statusDesc, @WebParam(name="SentDate") String sentDate, @WebParam(name="Username") String username, @WebParam(name="Password") String password) {
        logger.debug((Object)("BRANDNAME Client Get Delivery Report From ST: \n requestId: " + requestId + "\n smsStatus: " + smsStatus + "\n count: " + count + "\n statusDesc: " + statusDesc + "\n sentDate: " + sentDate + "\n username: " + username + "\n password: " + password));
        String res = "0";
        try {
            if (requestId != null && smsStatus != null && count != null && statusDesc != null && sentDate != null && username != null && password != null && username.equals(GameCommon.getValueStr((String)"BRANDNAME_CLIENT_USER")) && password.equals(GameCommon.getValueStr((String)"BRANDNAME_CLIENT_PASS"))) {
                res = "1";
                BrandnameDaoImpl dao = new BrandnameDaoImpl();
                BrandnameDLVRMessage message = new BrandnameDLVRMessage(requestId, smsStatus.intValue(), count.intValue(), statusDesc, sentDate);
                if (!dao.updateMessageDLVR(message)) {
                    // P1 cleanup: queue_otp retired (no consumer); SaveLogBrandnameDLVRProcessor deleted.
                }
            }
        }
        catch (Exception e) {
            logger.debug((Object)e);
        }
        return res;
    }
}

