/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.Service
 *  javax.xml.rpc.encoding.SerializerFactory
 *  org.apache.axis.AxisFault
 *  org.apache.axis.NoEndPointException
 *  org.apache.axis.client.Call
 *  org.apache.axis.client.Service
 *  org.apache.axis.client.Stub
 *  org.apache.axis.constants.Style
 *  org.apache.axis.constants.Use
 *  org.apache.axis.description.OperationDesc
 *  org.apache.axis.description.ParameterDesc
 *  org.apache.axis.encoding.DeserializerFactory
 *  org.apache.axis.encoding.SerializerFactory
 *  org.apache.axis.encoding.ser.ArrayDeserializerFactory
 *  org.apache.axis.encoding.ser.ArraySerializerFactory
 *  org.apache.axis.encoding.ser.BeanDeserializerFactory
 *  org.apache.axis.encoding.ser.BeanSerializerFactory
 *  org.apache.axis.encoding.ser.EnumDeserializerFactory
 *  org.apache.axis.encoding.ser.EnumSerializerFactory
 *  org.apache.axis.encoding.ser.SimpleDeserializerFactory
 *  org.apache.axis.encoding.ser.SimpleListDeserializerFactory
 *  org.apache.axis.encoding.ser.SimpleListSerializerFactory
 *  org.apache.axis.encoding.ser.SimpleSerializerFactory
 *  org.apache.axis.soap.SOAP11Constants
 *  org.apache.axis.soap.SOAPConstants
 *  org.apache.axis.utils.JavaUtils
 */
package com.vinplay.epay.megacard;

import com.vinplay.epay.megacard.ChangeResponse;
import com.vinplay.epay.megacard.ChargeReponse;
import com.vinplay.epay.megacard.LoginResponse;
import com.vinplay.epay.megacard.LogoutResponse;
import com.vinplay.epay.megacard.Services;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.encoding.SerializerFactory;
import org.apache.axis.AxisFault;
import org.apache.axis.NoEndPointException;
import org.apache.axis.client.Call;
import org.apache.axis.client.Stub;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.encoding.DeserializerFactory;
import org.apache.axis.encoding.ser.ArrayDeserializerFactory;
import org.apache.axis.encoding.ser.ArraySerializerFactory;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.encoding.ser.EnumDeserializerFactory;
import org.apache.axis.encoding.ser.EnumSerializerFactory;
import org.apache.axis.encoding.ser.SimpleDeserializerFactory;
import org.apache.axis.encoding.ser.SimpleListDeserializerFactory;
import org.apache.axis.encoding.ser.SimpleListSerializerFactory;
import org.apache.axis.encoding.ser.SimpleSerializerFactory;
import org.apache.axis.soap.SOAP11Constants;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.JavaUtils;

public class ServicesSoapBindingStub
extends Stub
implements Services {
    private Vector cachedSerClasses = new Vector();
    private Vector cachedSerQNames = new Vector();
    private Vector cachedSerFactories = new Vector();
    private Vector cachedDeserFactories = new Vector();
    static OperationDesc[] _operations = new OperationDesc[6];

    private static void _initOperationDesc1() {
        OperationDesc oper = new OperationDesc();
        oper.setName("changePassword");
        ParameterDesc param = new ParameterDesc(new QName("http://interfaces", "transid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "partnerID"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "old_password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "new_password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "md5sessionid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://entities", "ChangeResponse"));
        oper.setReturnClass(ChangeResponse.class);
        oper.setReturnQName(new QName("http://interfaces", "changePasswordReturn"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        ServicesSoapBindingStub._operations[0] = oper;
        oper = new OperationDesc();
        oper.setName("logout");
        param = new ParameterDesc(new QName("http://interfaces", "username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "partnerID"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "Md5sessionID"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://entities", "LogoutResponse"));
        oper.setReturnClass(LogoutResponse.class);
        oper.setReturnQName(new QName("http://interfaces", "logoutReturn"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        ServicesSoapBindingStub._operations[1] = oper;
        oper = new OperationDesc();
        oper.setName("login");
        param = new ParameterDesc(new QName("http://interfaces", "username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "partnerID"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://entities", "LoginResponse"));
        oper.setReturnClass(LoginResponse.class);
        oper.setReturnQName(new QName("http://interfaces", "loginReturn"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        ServicesSoapBindingStub._operations[2] = oper;
        oper = new OperationDesc();
        oper.setName("changeMPIN");
        param = new ParameterDesc(new QName("http://interfaces", "transid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "partnerID"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "old_mpin"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "new_mpin"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "md5sessionid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://entities", "ChangeResponse"));
        oper.setReturnClass(ChangeResponse.class);
        oper.setReturnQName(new QName("http://interfaces", "changeMPINReturn"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        ServicesSoapBindingStub._operations[3] = oper;
        oper = new OperationDesc();
        oper.setName("cardCharging");
        param = new ParameterDesc(new QName("http://interfaces", "transid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "partnerID"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "mpin"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "target"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "card_data"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "md5sessionid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://entities", "ChargeReponse"));
        oper.setReturnClass(ChargeReponse.class);
        oper.setReturnQName(new QName("http://interfaces", "cardChargingReturn"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        ServicesSoapBindingStub._operations[4] = oper;
        oper = new OperationDesc();
        oper.setName("getTransactionStatus");
        param = new ParameterDesc(new QName("http://interfaces", "transid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "partnerID"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://interfaces", "md5sessionid"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://entities", "ChargeReponse"));
        oper.setReturnClass(ChargeReponse.class);
        oper.setReturnQName(new QName("http://interfaces", "getTransactionStatusReturn"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        ServicesSoapBindingStub._operations[5] = oper;
    }

    public ServicesSoapBindingStub() throws AxisFault {
        this(null);
    }

    public ServicesSoapBindingStub(URL endpointURL, Service service) throws AxisFault {
        this(service);
        this.cachedEndpoint = endpointURL;
    }

    public ServicesSoapBindingStub(Service service) throws AxisFault {
        this.service = service == null ? new org.apache.axis.client.Service() : service;
        ((org.apache.axis.client.Service)this.service).setTypeMappingVersion("1.2");
        Class<BeanSerializerFactory> beansf = BeanSerializerFactory.class;
        Class<BeanDeserializerFactory> beandf = BeanDeserializerFactory.class;
        Class<EnumSerializerFactory> enumsf = EnumSerializerFactory.class;
        Class<EnumDeserializerFactory> enumdf = EnumDeserializerFactory.class;
        Class<ArraySerializerFactory> arraysf = ArraySerializerFactory.class;
        Class<ArrayDeserializerFactory> arraydf = ArrayDeserializerFactory.class;
        Class<SimpleSerializerFactory> simplesf = SimpleSerializerFactory.class;
        Class<SimpleDeserializerFactory> simpledf = SimpleDeserializerFactory.class;
        Class<SimpleListSerializerFactory> simplelistsf = SimpleListSerializerFactory.class;
        Class<SimpleListDeserializerFactory> simplelistdf = SimpleListDeserializerFactory.class;
        QName qName = new QName("http://entities", "ChangeResponse");
        this.cachedSerQNames.add(qName);
        Class cls = ChangeResponse.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://entities", "ChargeReponse");
        this.cachedSerQNames.add(qName);
        cls = ChargeReponse.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://entities", "LoginResponse");
        this.cachedSerQNames.add(qName);
        cls = LoginResponse.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://entities", "LogoutResponse");
        this.cachedSerQNames.add(qName);
        cls = LogoutResponse.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected Call createCall() throws RemoteException {
        try {
            Object key;
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
                key = (String)keys.nextElement();
                _call.setProperty((String)key, this.cachedProperties.get(key));
            }
            key = this;
            synchronized (key) {
                if (this.firstCall()) {
                    _call.setEncodingStyle((String)null);
                    for (int i = 0; i < this.cachedSerFactories.size(); ++i) {
                        Class cls = (Class)this.cachedSerClasses.get(i);
                        QName qName = (QName)this.cachedSerQNames.get(i);
                        Object x = this.cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            Class sf = (Class)this.cachedSerFactories.get(i);
                            Class df = (Class)this.cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                            continue;
                        }
                        if (!(x instanceof SerializerFactory)) continue;
                        org.apache.axis.encoding.SerializerFactory sf2 = (org.apache.axis.encoding.SerializerFactory)this.cachedSerFactories.get(i);
                        DeserializerFactory df2 = (DeserializerFactory)this.cachedDeserFactories.get(i);
                        _call.registerTypeMapping(cls, qName, sf2, df2, false);
                    }
                }
            }
            return _call;
        }
        catch (Throwable _t) {
            throw new AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public ChangeResponse changePassword(String transid, String username, String partnerID, String old_password, String new_password, String md5Sessionid) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://interfaces", "changePassword"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{transid, username, partnerID, old_password, new_password, md5Sessionid});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (ChangeResponse)_resp;
        }
        catch (Exception _exception) {
            return (ChangeResponse)JavaUtils.convert(_resp, ChangeResponse.class);
        }
    }

    public LogoutResponse logout(String username, String partnerID, String md5SessionID) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://interfaces", "logout"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{username, partnerID, md5SessionID});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (LogoutResponse)_resp;
        }
        catch (Exception _exception) {
            return (LogoutResponse)JavaUtils.convert(_resp, LogoutResponse.class);
        }
    }

    public LoginResponse login(String username, String password, String partnerID) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://interfaces", "login"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{username, password, partnerID});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (LoginResponse)_resp;
        }
        catch (Exception _exception) {
            return (LoginResponse)JavaUtils.convert(_resp, LoginResponse.class);
        }
    }

    public ChangeResponse changeMPIN(String transid, String username, String partnerID, String old_mpin, String new_mpin, String md5Sessionid) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://interfaces", "changeMPIN"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{transid, username, partnerID, old_mpin, new_mpin, md5Sessionid});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (ChangeResponse)_resp;
        }
        catch (Exception _exception) {
            return (ChangeResponse)JavaUtils.convert(_resp, ChangeResponse.class);
        }
    }

    @Override
    public ChargeReponse cardCharging(String transid, String username, String partnerID, String mpin, String target, String card_data, String md5Sessionid) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://interfaces", "cardCharging"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{transid, username, partnerID, mpin, target, card_data, md5Sessionid});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (ChargeReponse)_resp;
        }
        catch (Exception _exception) {
            return (ChargeReponse)JavaUtils.convert(_resp, ChargeReponse.class);
        }
    }

    public ChargeReponse getTransactionStatus(String transid, String username, String partnerID, String md5Sessionid) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://interfaces", "getTransactionStatus"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{transid, username, partnerID, md5Sessionid});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (ChargeReponse)_resp;
        }
        catch (Exception _exception) {
            return (ChargeReponse)JavaUtils.convert(_resp, ChargeReponse.class);
        }
    }

    static {
        ServicesSoapBindingStub._initOperationDesc1();
    }
}

