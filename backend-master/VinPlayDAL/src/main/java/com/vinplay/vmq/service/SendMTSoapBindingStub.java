/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.Service
 *  org.apache.axis.AxisFault
 *  org.apache.axis.NoEndPointException
 *  org.apache.axis.client.Call
 *  org.apache.axis.client.Service
 *  org.apache.axis.client.Stub
 *  org.apache.axis.constants.Style
 *  org.apache.axis.constants.Use
 *  org.apache.axis.description.OperationDesc
 *  org.apache.axis.description.ParameterDesc
 *  org.apache.axis.soap.SOAPConstants
 *  org.apache.axis.utils.JavaUtils
 */
package com.vinplay.vmq.service;

import com.vinplay.vmq.service.ServiceProvider;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import org.apache.axis.AxisFault;
import org.apache.axis.NoEndPointException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Stub;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.JavaUtils;

public class SendMTSoapBindingStub
extends Stub
implements ServiceProvider {
    static OperationDesc[] _operations = new OperationDesc[4];

    private static void _initOperationDesc1() {
        OperationDesc oper = new OperationDesc();
        oper.setName("sendMT");
        ParameterDesc param = new ParameterDesc(new QName("", "userID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "message"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "serviceID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "commandCode"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "requestID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "totalMessage"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageIndex"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "isMore"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "contentType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        oper.setReturnClass(Integer.TYPE);
        oper.setReturnQName(new QName("", "sendMTReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        SendMTSoapBindingStub._operations[0] = oper;
        oper = new OperationDesc();
        oper.setName("sendMTEx");
        param = new ParameterDesc(new QName("", "userID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "message"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "serviceID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "commandCode"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "requestID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "totalMessage"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageIndex"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "isMore"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "contentType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        oper.setReturnClass(Integer.TYPE);
        oper.setReturnQName(new QName("", "sendMTExReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        SendMTSoapBindingStub._operations[1] = oper;
        oper = new OperationDesc();
        oper.setName("sendMTEx2");
        param = new ParameterDesc(new QName("", "userID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "message"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "serviceID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "commandCode"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "requestID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "totalMessage"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageIndex"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "isMore"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "contentType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageClass"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "mwi"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "dataCoding"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        oper.setReturnClass(Integer.TYPE);
        oper.setReturnQName(new QName("", "sendMTEx2Return"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        SendMTSoapBindingStub._operations[2] = oper;
        oper = new OperationDesc();
        oper.setName("sendMTPush");
        param = new ParameterDesc(new QName("", "userID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "message"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "serviceID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "commandCode"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "requestID"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "totalMessage"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "messageIndex"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "isMore"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "contentType"), (byte)1, new QName("http://schemas.xmlsoap.org/soap/encoding/", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        oper.setReturnClass(Integer.TYPE);
        oper.setReturnQName(new QName("", "sendMTPushReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        SendMTSoapBindingStub._operations[3] = oper;
    }

    public SendMTSoapBindingStub() throws AxisFault {
        this(null);
    }

    public SendMTSoapBindingStub(URL endpointURL, Service service) throws AxisFault {
        this(service);
        this.cachedEndpoint = endpointURL;
    }

    public SendMTSoapBindingStub(Service service) throws AxisFault {
        this.service = service == null ? new org.apache.axis.client.Service() : service;
        ((org.apache.axis.client.Service)this.service).setTypeMappingVersion("1.2");
    }

    protected Call createCall() throws RemoteException {
        try {
            Call _call = super._createCall();
            if (this.maintainSessionSet) {
                _call.setMaintainSession(this.maintainSession);
            }
            if (this.cachedUsername != null) {
                _call.setUsername(this.cachedUsername);
            }
            if (this.cachedPassword != null) {
                _call.setPassword(this.cachedPassword);
            }
            if (this.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(this.cachedEndpoint);
            }
            if (this.cachedTimeout != null) {
                _call.setTimeout(this.cachedTimeout);
            }
            if (this.cachedPortName != null) {
                _call.setPortName(this.cachedPortName);
            }
            Enumeration keys = this.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                _call.setProperty(key, this.cachedProperties.get(key));
            }
            return _call;
        }
        catch (Throwable _t) {
            throw new AxisFault("Failure trying to get the Call object", _t);
        }
    }

    @Override
    public int sendMT(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://mt.webservice.ems.vmg.com", "sendMT"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (Integer)_resp;
        }
        catch (Exception _exception) {
            return (Integer)JavaUtils.convert(_resp, Integer.TYPE);
        }
    }

    @Override
    public int sendMTEx(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://mt.webservice.ems.vmg.com", "sendMTEx"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (Integer)_resp;
        }
        catch (Exception _exception) {
            return (Integer)JavaUtils.convert(_resp, Integer.TYPE);
        }
    }

    @Override
    public int sendMTEx2(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType, int messageClass, int mwi, int dataCoding) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://mt.webservice.ems.vmg.com", "sendMTEx2"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType, new Integer(messageClass), new Integer(mwi), new Integer(dataCoding)});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (Integer)_resp;
        }
        catch (Exception _exception) {
            return (Integer)JavaUtils.convert(_resp, Integer.TYPE);
        }
    }

    @Override
    public int sendMTPush(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://mt.webservice.ems.vmg.com", "sendMTPush"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (Integer)_resp;
        }
        catch (Exception _exception) {
            return (Integer)JavaUtils.convert(_resp, Integer.TYPE);
        }
    }

    static {
        SendMTSoapBindingStub._initOperationDesc1();
    }
}

