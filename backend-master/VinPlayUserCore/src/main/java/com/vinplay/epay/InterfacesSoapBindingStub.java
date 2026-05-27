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
 *  org.apache.axis.encoding.ser.BeanDeserializerFactory
 *  org.apache.axis.encoding.ser.BeanSerializerFactory
 *  org.apache.axis.soap.SOAP11Constants
 *  org.apache.axis.soap.SOAPConstants
 *  org.apache.axis.utils.JavaUtils
 */
package com.vinplay.epay;

import com.vinplay.epay.CheckOrdesrCDVResult;
import com.vinplay.epay.CheckTransResult;
import com.vinplay.epay.DownloadSoftpinResult;
import com.vinplay.epay.Interfaces;
import com.vinplay.epay.PaymentCdvResult;
import com.vinplay.epay.QueryBalanceResult;
import com.vinplay.epay.TopupResult;
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
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.soap.SOAP11Constants;
import org.apache.axis.soap.SOAPConstants;
import org.apache.axis.utils.JavaUtils;

public class InterfacesSoapBindingStub
extends Stub
implements Interfaces {
    private Vector cachedSerClasses = new Vector();
    private Vector cachedSerQNames = new Vector();
    private Vector cachedSerFactories = new Vector();
    private Vector cachedDeserFactories = new Vector();
    static OperationDesc[] _operations = new OperationDesc[9];

    private static void _initOperationDesc1() {
        OperationDesc oper = new OperationDesc();
        oper.setName("topup");
        ParameterDesc param = new ParameterDesc(new QName("", "requestId"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "provider"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "target"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "amount"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://services.epay", "TopupResult"));
        oper.setReturnClass(TopupResult.class);
        oper.setReturnQName(new QName("", "topupReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[0] = oper;
        oper = new OperationDesc();
        oper.setName("echo");
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        oper.setReturnClass(Integer.TYPE);
        oper.setReturnQName(new QName("", "echoReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[1] = oper;
        oper = new OperationDesc();
        oper.setName("checkStore");
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "provider"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "amount"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        oper.setReturnClass(Integer.TYPE);
        oper.setReturnQName(new QName("", "checkStoreReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[2] = oper;
        oper = new OperationDesc();
        oper.setName("paymentCDV");
        param = new ParameterDesc(new QName("", "requestId"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "provider"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "type"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "account"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "amount"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "long"), Long.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "timeOut"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://services.epay", "PaymentCdvResult"));
        oper.setReturnClass(PaymentCdvResult.class);
        oper.setReturnQName(new QName("", "paymentCDVReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[3] = oper;
        oper = new OperationDesc();
        oper.setName("downloadSoftpin");
        param = new ParameterDesc(new QName("", "requestId"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "provider"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "amount"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "quantity"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://services.epay", "DownloadSoftpinResult"));
        oper.setReturnClass(DownloadSoftpinResult.class);
        oper.setReturnQName(new QName("", "downloadSoftpinReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[4] = oper;
        oper = new OperationDesc();
        oper.setName("checkTrans");
        param = new ParameterDesc(new QName("", "requestId"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "type"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.TYPE, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://services.epay", "CheckTransResult"));
        oper.setReturnClass(CheckTransResult.class);
        oper.setReturnQName(new QName("", "checkTransReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[5] = oper;
        oper = new OperationDesc();
        oper.setName("queryBalance");
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://services.epay", "QueryBalanceResult"));
        oper.setReturnClass(QueryBalanceResult.class);
        oper.setReturnQName(new QName("", "queryBalanceReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[6] = oper;
        oper = new OperationDesc();
        oper.setName("checkOrdersCDV");
        param = new ParameterDesc(new QName("", "requestId"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://services.epay", "CheckOrdesrCDVResult"));
        oper.setReturnClass(CheckOrdesrCDVResult.class);
        oper.setReturnQName(new QName("", "checkOrdersCDVReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[7] = oper;
        oper = new OperationDesc();
        oper.setName("reDownloadSoftpin");
        param = new ParameterDesc(new QName("", "requestId"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "partnerName"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("", "sign"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://services.epay", "DownloadSoftpinResult"));
        oper.setReturnClass(DownloadSoftpinResult.class);
        oper.setReturnQName(new QName("", "reDownloadSoftpinReturn"));
        oper.setStyle(Style.RPC);
        oper.setUse(Use.ENCODED);
        InterfacesSoapBindingStub._operations[8] = oper;
    }

    public InterfacesSoapBindingStub() throws AxisFault {
        this(null);
    }

    public InterfacesSoapBindingStub(URL endpointURL, Service service) throws AxisFault {
        this(service);
        this.cachedEndpoint = endpointURL;
    }

    public InterfacesSoapBindingStub(Service service) throws AxisFault {
        this.service = service == null ? new org.apache.axis.client.Service() : service;
        ((org.apache.axis.client.Service)this.service).setTypeMappingVersion("1.2");
        Class<BeanSerializerFactory> beansf = BeanSerializerFactory.class;
        Class<BeanDeserializerFactory> beandf = BeanDeserializerFactory.class;
        QName qName = new QName("http://services.epay", "CheckOrdesrCDVResult");
        this.cachedSerQNames.add(qName);
        Class cls = CheckOrdesrCDVResult.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://services.epay", "CheckTransResult");
        this.cachedSerQNames.add(qName);
        cls = CheckTransResult.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://services.epay", "DownloadSoftpinResult");
        this.cachedSerQNames.add(qName);
        cls = DownloadSoftpinResult.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://services.epay", "PaymentCdvResult");
        this.cachedSerQNames.add(qName);
        cls = PaymentCdvResult.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://services.epay", "QueryBalanceResult");
        this.cachedSerQNames.add(qName);
        cls = QueryBalanceResult.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://services.epay", "TopupResult");
        this.cachedSerQNames.add(qName);
        cls = TopupResult.class;
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
                    _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
                    _call.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");
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

    @Override
    public TopupResult topup(String requestId, String partnerName, String provider, String target, int amount, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "topup"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{requestId, partnerName, provider, target, new Integer(amount), sign});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (TopupResult)_resp;
        }
        catch (Exception _exception) {
            return (TopupResult)JavaUtils.convert(_resp, TopupResult.class);
        }
    }

    @Override
    public int echo() throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "echo"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[0]);
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
    public int checkStore(String partnerName, String provider, int amount, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "checkStore"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{partnerName, provider, new Integer(amount), sign});
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
    public PaymentCdvResult paymentCDV(String requestId, String partnerName, String provider, int type, String account, long amount, int timeOut, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "paymentCDV"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{requestId, partnerName, provider, new Integer(type), account, new Long(amount), new Integer(timeOut), sign});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (PaymentCdvResult)_resp;
        }
        catch (Exception _exception) {
            return (PaymentCdvResult)JavaUtils.convert(_resp, PaymentCdvResult.class);
        }
    }

    @Override
    public DownloadSoftpinResult downloadSoftpin(String requestId, String partnerName, String provider, int amount, int quantity, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "downloadSoftpin"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{requestId, partnerName, provider, new Integer(amount), new Integer(quantity), sign});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (DownloadSoftpinResult)_resp;
        }
        catch (Exception _exception) {
            return (DownloadSoftpinResult)JavaUtils.convert(_resp, DownloadSoftpinResult.class);
        }
    }

    @Override
    public CheckTransResult checkTrans(String requestId, String partnerName, int type, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "checkTrans"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{requestId, partnerName, new Integer(type), sign});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (CheckTransResult)_resp;
        }
        catch (Exception _exception) {
            return (CheckTransResult)JavaUtils.convert(_resp, CheckTransResult.class);
        }
    }

    @Override
    public QueryBalanceResult queryBalance(String partnerName, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "queryBalance"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{partnerName, sign});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (QueryBalanceResult)_resp;
        }
        catch (Exception _exception) {
            return (QueryBalanceResult)JavaUtils.convert(_resp, QueryBalanceResult.class);
        }
    }

    @Override
    public CheckOrdesrCDVResult checkOrdersCDV(String requestId, String partnerName, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "checkOrdersCDV"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{requestId, partnerName, sign});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (CheckOrdesrCDVResult)_resp;
        }
        catch (Exception _exception) {
            return (CheckOrdesrCDVResult)JavaUtils.convert(_resp, CheckOrdesrCDVResult.class);
        }
    }

    @Override
    public DownloadSoftpinResult reDownloadSoftpin(String requestId, String partnerName, String sign) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://services.epay", "reDownloadSoftpin"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{requestId, partnerName, sign});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (DownloadSoftpinResult)_resp;
        }
        catch (Exception _exception) {
            return (DownloadSoftpinResult)JavaUtils.convert(_resp, DownloadSoftpinResult.class);
        }
    }

    static {
        InterfacesSoapBindingStub._initOperationDesc1();
    }
}

