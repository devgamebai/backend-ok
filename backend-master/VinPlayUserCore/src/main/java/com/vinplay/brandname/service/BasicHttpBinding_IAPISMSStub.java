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
package com.vinplay.brandname.service;

import com.vinplay.brandname.enties.DataSms;
import com.vinplay.brandname.enties.ListPhone;
import com.vinplay.brandname.service.IAPISMS;
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

public class BasicHttpBinding_IAPISMSStub
extends Stub
implements IAPISMS {
    private Vector cachedSerClasses = new Vector();
    private Vector cachedSerQNames = new Vector();
    private Vector cachedSerFactories = new Vector();
    private Vector cachedDeserFactories = new Vector();
    static OperationDesc[] _operations = new OperationDesc[8];

    private static void _initOperationDesc1() {
        OperationDesc oper = new OperationDesc();
        oper.setName("PushMsg2Phone");
        ParameterDesc param = new ParameterDesc(new QName("http://tempuri.org/", "Sender"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Msg"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Phone"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "PushMsg2PhoneResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[0] = oper;
        oper = new OperationDesc();
        oper.setName("PushMsg2PhoneDLVR");
        param = new ParameterDesc(new QName("http://tempuri.org/", "Sender"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Msg"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Phone"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "RequestId"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "idclient"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "PushMsg2PhoneDLVRResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[1] = oper;
        oper = new OperationDesc();
        oper.setName("PushMsgAdvDirect");
        param = new ParameterDesc(new QName("http://tempuri.org/", "Sender"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Msg"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Phone"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "PushMsgAdvDirectResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[2] = oper;
        oper = new OperationDesc();
        oper.setName("SendOTP");
        param = new ParameterDesc(new QName("http://tempuri.org/", "Sender"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Msg"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Phone"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "SendOTPResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[3] = oper;
        oper = new OperationDesc();
        oper.setName("PushMsg2ListPhone");
        param = new ParameterDesc(new QName("http://tempuri.org/", "Sender"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Msg"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Phone"), (byte)1, new QName("http://schemas.microsoft.com/2003/10/Serialization/Arrays", "ArrayOfstring"), String[].class, false, false);
        param.setItemQName(new QName("http://schemas.microsoft.com/2003/10/Serialization/Arrays", "string"));
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "PushMsg2ListPhoneResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[4] = oper;
        oper = new OperationDesc();
        oper.setName("PushMsg2ListPhoneDLVR");
        param = new ParameterDesc(new QName("http://tempuri.org/", "Sender"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Msg"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Phone"), (byte)1, new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ArrayOfListPhone"), ListPhone[].class, false, false);
        param.setItemQName(new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ListPhone"));
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "idclient"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "int"), Integer.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "PushMsg2ListPhoneDLVRResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[5] = oper;
        oper = new OperationDesc();
        oper.setName("PushBulkSms");
        param = new ParameterDesc(new QName("http://tempuri.org/", "Sender"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "data"), (byte)1, new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ArrayOfDataSms"), DataSms[].class, false, false);
        param.setItemQName(new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "DataSms"));
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "PushBulkSmsResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[6] = oper;
        oper = new OperationDesc();
        oper.setName("GetTelco");
        param = new ParameterDesc(new QName("http://tempuri.org/", "Phone"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Username"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        param = new ParameterDesc(new QName("http://tempuri.org/", "Password"), (byte)1, new QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        param.setNillable(true);
        oper.addParameter(param);
        oper.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new QName("http://tempuri.org/", "GetTelcoResult"));
        oper.setStyle(Style.WRAPPED);
        oper.setUse(Use.LITERAL);
        BasicHttpBinding_IAPISMSStub._operations[7] = oper;
    }

    public BasicHttpBinding_IAPISMSStub() throws AxisFault {
        this(null);
    }

    public BasicHttpBinding_IAPISMSStub(URL endpointURL, Service service) throws AxisFault {
        this(service);
        this.cachedEndpoint = endpointURL;
    }

    public BasicHttpBinding_IAPISMSStub(Service service) throws AxisFault {
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
        QName qName = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ArrayOfDataSms");
        this.cachedSerQNames.add(qName);
        Class cls = DataSms[].class;
        this.cachedSerClasses.add(cls);
        qName = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "DataSms");
        QName qName2 = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "DataSms");
        this.cachedSerFactories.add(new ArraySerializerFactory(qName, qName2));
        this.cachedDeserFactories.add(new ArrayDeserializerFactory());
        qName = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ArrayOfListPhone");
        this.cachedSerQNames.add(qName);
        cls = ListPhone[].class;
        this.cachedSerClasses.add(cls);
        qName = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ListPhone");
        qName2 = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ListPhone");
        this.cachedSerFactories.add(new ArraySerializerFactory(qName, qName2));
        this.cachedDeserFactories.add(new ArrayDeserializerFactory());
        qName = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "DataSms");
        this.cachedSerQNames.add(qName);
        cls = DataSms.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://schemas.datacontract.org/2004/07/Stel.WCF.APISMS.WorldSMS2._0", "ListPhone");
        this.cachedSerQNames.add(qName);
        cls = ListPhone.class;
        this.cachedSerClasses.add(cls);
        this.cachedSerFactories.add(beansf);
        this.cachedDeserFactories.add(beandf);
        qName = new QName("http://schemas.microsoft.com/2003/10/Serialization/Arrays", "ArrayOfstring");
        this.cachedSerQNames.add(qName);
        cls = String[].class;
        this.cachedSerClasses.add(cls);
        qName = new QName("http://www.w3.org/2001/XMLSchema", "string");
        qName2 = new QName("http://schemas.microsoft.com/2003/10/Serialization/Arrays", "string");
        this.cachedSerFactories.add(new ArraySerializerFactory(qName, qName2));
        this.cachedDeserFactories.add(new ArrayDeserializerFactory());
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

    @Override
    public String pushMsg2Phone(String sender, String msg, String phone, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/PushMsg2Phone");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "PushMsg2Phone"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{sender, msg, phone, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    @Override
    public String pushMsg2PhoneDLVR(String sender, String msg, String phone, String requestId, Integer idclient, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/PushMsg2PhoneDLVR");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "PushMsg2PhoneDLVR"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{sender, msg, phone, requestId, idclient, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    @Override
    public String pushMsgAdvDirect(String sender, String msg, String phone, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/PushMsgAdvDirect");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "PushMsgAdvDirect"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{sender, msg, phone, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    @Override
    public String sendOTP(String sender, String msg, String phone, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/SendOTP");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "SendOTP"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{sender, msg, phone, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    @Override
    public String pushMsg2ListPhone(String sender, String msg, String[] phone, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/PushMsg2ListPhone");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "PushMsg2ListPhone"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{sender, msg, phone, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    @Override
    public String pushMsg2ListPhoneDLVR(String sender, String msg, ListPhone[] phone, Integer idclient, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/PushMsg2ListPhoneDLVR");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "PushMsg2ListPhoneDLVR"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{sender, msg, phone, idclient, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    @Override
    public String pushBulkSms(String sender, DataSms[] data, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/PushBulkSms");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "PushBulkSms"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{sender, data, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    @Override
    public String getTelco(String phone, String username, String password) throws RemoteException {
        if (this.cachedEndpoint == null) {
            throw new NoEndPointException();
        }
        Call _call = this.createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/IAPISMS/GetTelco");
        _call.setEncodingStyle((String)null);
        _call.setProperty("sendXsiTypes", Boolean.FALSE);
        _call.setProperty("sendMultiRefs", Boolean.FALSE);
        _call.setSOAPVersion((SOAPConstants)SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new QName("http://tempuri.org/", "GetTelco"));
        this.setRequestHeaders(_call);
        this.setAttachments(_call);
        Object _resp = _call.invoke(new Object[]{phone, username, password});
        if (_resp instanceof RemoteException) {
            throw (RemoteException)_resp;
        }
        this.extractAttachments(_call);
        try {
            return (String)_resp;
        }
        catch (Exception _exception) {
            return (String)JavaUtils.convert(_resp, String.class);
        }
    }

    static {
        BasicHttpBinding_IAPISMSStub._initOperationDesc1();
    }
}

