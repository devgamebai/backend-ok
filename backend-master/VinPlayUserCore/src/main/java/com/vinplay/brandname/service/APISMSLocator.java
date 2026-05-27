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
package com.vinplay.brandname.service;

import com.vinplay.brandname.service.APISMS;
import com.vinplay.brandname.service.BasicHttpBinding_IAPISMSStub;
import com.vinplay.brandname.service.IAPISMS;
import com.vinplay.usercore.utils.GameCommon;
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

public class APISMSLocator
extends org.apache.axis.client.Service
implements APISMS {
    private static final long serialVersionUID = 1L;
    private String BasicHttpBinding_IAPISMS_address = GameCommon.BRANDNAME_URL;
    private String BasicHttpBinding_IAPISMSWSDDServiceName = "BasicHttpBinding_IAPISMS";
    private HashSet ports = null;

    public APISMSLocator() {
    }

    public APISMSLocator(EngineConfiguration config) {
        super(config);
    }

    public APISMSLocator(String wsdlLoc, QName sName) throws ServiceException {
        super(wsdlLoc, sName);
    }

    @Override
    public String getBasicHttpBinding_IAPISMSAddress() {
        return this.BasicHttpBinding_IAPISMS_address;
    }

    public String getBasicHttpBinding_IAPISMSWSDDServiceName() {
        return this.BasicHttpBinding_IAPISMSWSDDServiceName;
    }

    public void setBasicHttpBinding_IAPISMSWSDDServiceName(String name) {
        this.BasicHttpBinding_IAPISMSWSDDServiceName = name;
    }

    @Override
    public IAPISMS getBasicHttpBinding_IAPISMS() throws ServiceException {
        URL endpoint;
        try {
            endpoint = new URL(this.BasicHttpBinding_IAPISMS_address);
        }
        catch (MalformedURLException e) {
            throw new ServiceException((Throwable)e);
        }
        return this.getBasicHttpBinding_IAPISMS(endpoint);
    }

    @Override
    public IAPISMS getBasicHttpBinding_IAPISMS(URL portAddress) throws ServiceException {
        try {
            BasicHttpBinding_IAPISMSStub _stub = new BasicHttpBinding_IAPISMSStub(portAddress, this);
            _stub.setPortName(this.getBasicHttpBinding_IAPISMSWSDDServiceName());
            return _stub;
        }
        catch (AxisFault e) {
            return null;
        }
    }

    public void setBasicHttpBinding_IAPISMSEndpointAddress(String address) {
        this.BasicHttpBinding_IAPISMS_address = address;
    }

    public Remote getPort(Class serviceEndpointInterface) throws ServiceException {
        try {
            if (IAPISMS.class.isAssignableFrom(serviceEndpointInterface)) {
                BasicHttpBinding_IAPISMSStub _stub = new BasicHttpBinding_IAPISMSStub(new URL(this.BasicHttpBinding_IAPISMS_address), this);
                _stub.setPortName(this.getBasicHttpBinding_IAPISMSWSDDServiceName());
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
        if ("BasicHttpBinding_IAPISMS".equals(inputPortName)) {
            return this.getBasicHttpBinding_IAPISMS();
        }
        Remote _stub = this.getPort(serviceEndpointInterface);
        ((Stub)_stub).setPortName(portName);
        return _stub;
    }

    public QName getServiceName() {
        return new QName("http://tempuri.org/", "APISMS");
    }

    public Iterator getPorts() {
        if (this.ports == null) {
            this.ports = new HashSet<QName>();
            this.ports.add(new QName("http://tempuri.org/", "BasicHttpBinding_IAPISMS"));
        }
        return this.ports.iterator();
    }

    public void setEndpointAddress(String portName, String address) throws ServiceException {
        if ("BasicHttpBinding_IAPISMS".equals(portName)) {
            this.setBasicHttpBinding_IAPISMSEndpointAddress(address);
            return;
        }
        throw new ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
    }

    public void setEndpointAddress(QName portName, String address) throws ServiceException {
        this.setEndpointAddress(portName.getLocalPart(), address);
    }
}

