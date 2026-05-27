/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.Service
 *  javax.xml.rpc.ServiceException
 *  org.apache.axis.AxisFault
 *  org.apache.axis.EngineConfiguration
 *  org.apache.axis.client.Service
 *  org.apache.axis.client.Stub
 */
package com.vinplay.vmq.service;

import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vmq.service.SendMTSoapBindingStub;
import com.vinplay.vmq.service.ServiceProvider;
import com.vinplay.vmq.service.ServiceProviderService;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Remote;
import java.util.HashSet;
import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.client.Stub;

public class ServiceProviderServiceLocator
extends org.apache.axis.client.Service
implements ServiceProviderService {
    private static final long serialVersionUID = 1L;
    private String sendMT_address = GameCommon.OTP_URL_SEND_MT;
    private String sendMTWSDDServiceName = "sendMT";
    private HashSet ports = null;

    public ServiceProviderServiceLocator() {
    }

    public ServiceProviderServiceLocator(EngineConfiguration config) {
        super(config);
    }

    public ServiceProviderServiceLocator(String wsdlLoc, QName sName) throws ServiceException {
        super(wsdlLoc, sName);
    }

    @Override
    public String getsendMTAddress() {
        return this.sendMT_address;
    }

    public String getsendMTWSDDServiceName() {
        return this.sendMTWSDDServiceName;
    }

    public void setsendMTWSDDServiceName(String name) {
        this.sendMTWSDDServiceName = name;
    }

    @Override
    public ServiceProvider getsendMT() throws ServiceException {
        URL endpoint;
        try {
            endpoint = new URL(this.sendMT_address);
        }
        catch (MalformedURLException e) {
            throw new ServiceException((Throwable)e);
        }
        return this.getsendMT(endpoint);
    }

    @Override
    public ServiceProvider getsendMT(URL portAddress) throws ServiceException {
        try {
            SendMTSoapBindingStub _stub = new SendMTSoapBindingStub(portAddress, this);
            _stub.setPortName(this.getsendMTWSDDServiceName());
            return _stub;
        }
        catch (AxisFault e) {
            return null;
        }
    }

    public void setsendMTEndpointAddress(String address) {
        this.sendMT_address = address;
    }

    public Remote getPort(Class serviceEndpointInterface) throws ServiceException {
        try {
            if (ServiceProvider.class.isAssignableFrom(serviceEndpointInterface)) {
                SendMTSoapBindingStub _stub = new SendMTSoapBindingStub(new URL(this.sendMT_address), this);
                _stub.setPortName(this.getsendMTWSDDServiceName());
                return _stub;
            }
        }
        catch (Throwable t) {
            throw new ServiceException(t);
        }
        throw new ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    public Remote getPort(QName portName, Class serviceEndpointInterface) throws ServiceException {
        if (portName == null) {
            return this.getPort(serviceEndpointInterface);
        }
        String inputPortName = portName.getLocalPart();
        if ("sendMT".equals(inputPortName)) {
            return this.getsendMT();
        }
        Remote _stub = this.getPort(serviceEndpointInterface);
        ((Stub)_stub).setPortName(portName);
        return _stub;
    }

    public QName getServiceName() {
        return new QName(GameCommon.OTP_URL_SEND_MT, "ServiceProviderService");
    }

    public Iterator getPorts() {
        if (this.ports == null) {
            this.ports = new HashSet<QName>();
            this.ports.add(new QName(GameCommon.OTP_URL_SEND_MT, "sendMT"));
        }
        return this.ports.iterator();
    }

    public void setEndpointAddress(String portName, String address) throws ServiceException {
        if ("sendMT".equals(portName)) {
            this.setsendMTEndpointAddress(address);
            return;
        }
        throw new ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
    }

    public void setEndpointAddress(QName portName, String address) throws ServiceException {
        this.setEndpointAddress(portName.getLocalPart(), address);
    }
}

