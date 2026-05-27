/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.utils.GameCommon
 *  org.apache.log4j.Logger
 */
package com.vinplay.api.otp;

import com.sun.net.httpserver.HttpExchange;
import com.vinplay.usercore.utils.GameCommon;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import org.apache.log4j.Logger;

public class IPValidatorHandler
implements SOAPHandler<SOAPMessageContext> {
    private static final Logger logger = Logger.getLogger((String)"api");

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean isRequest = (Boolean)context.get("javax.xml.ws.handler.message.outbound");
        if (!isRequest.booleanValue()) {
            SOAPMessage soapMsg = context.getMessage();
            HttpExchange exchange = (HttpExchange)context.get("com.sun.xml.internal.ws.http.exchange");
            InetSocketAddress remoteAddress = exchange.getRemoteAddress();
            String clientIP = remoteAddress.getAddress().getHostAddress();
            if (!clientIP.equals(GameCommon.OTP_IP_FILTER)) {
                logger.debug((String.valueOf(clientIP) + " - IP address invalid. Access is denied !"));
                this.generateSOAPErrMessage(soapMsg, "IP address invalid. Access is denied !");
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    private void generateSOAPErrMessage(SOAPMessage msg, String reason) {
        try {
            SOAPBody soapBody = msg.getSOAPPart().getEnvelope().getBody();
            SOAPFault soapFault = soapBody.addFault();
            soapFault.setFaultString(reason);
            throw new SOAPFaultException(soapFault);
        }
        catch (SOAPException e) {
            logger.debug(e);
            return;
        }
    }
}

