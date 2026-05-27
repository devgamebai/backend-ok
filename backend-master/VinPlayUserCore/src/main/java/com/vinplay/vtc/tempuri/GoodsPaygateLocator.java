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
package com.vinplay.vtc.tempuri;

import com.vinplay.vtc.tempuri.GoodsPaygate;
import com.vinplay.vtc.tempuri.GoodsPaygateSoap;
import com.vinplay.vtc.tempuri.GoodsPaygateSoapStub;
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

public class GoodsPaygateLocator
extends org.apache.axis.client.Service
implements GoodsPaygate {
    private static final long serialVersionUID = 1L;
    private String GoodsPaygateSoap_address = "https://pay.vtc.vn/WS/GoodsPaygate.asmx";
    private String GoodsPaygateSoapWSDDServiceName = "GoodsPaygateSoap";
    private HashSet ports = null;

    public GoodsPaygateLocator() {
    }

    public GoodsPaygateLocator(EngineConfiguration config) {
        super(config);
    }

    public GoodsPaygateLocator(String wsdlLoc, QName sName) throws ServiceException {
        super(wsdlLoc, sName);
    }

    @Override
    public String getGoodsPaygateSoapAddress() {
        return this.GoodsPaygateSoap_address;
    }

    public String getGoodsPaygateSoapWSDDServiceName() {
        return this.GoodsPaygateSoapWSDDServiceName;
    }

    public void setGoodsPaygateSoapWSDDServiceName(String name) {
        this.GoodsPaygateSoapWSDDServiceName = name;
    }

    @Override
    public GoodsPaygateSoap getGoodsPaygateSoap() throws ServiceException {
        URL endpoint;
        try {
            endpoint = new URL(this.GoodsPaygateSoap_address);
        }
        catch (MalformedURLException e) {
            throw new ServiceException((Throwable)e);
        }
        return this.getGoodsPaygateSoap(endpoint);
    }

    @Override
    public GoodsPaygateSoap getGoodsPaygateSoap(URL portAddress) throws ServiceException {
        try {
            GoodsPaygateSoapStub _stub = new GoodsPaygateSoapStub(portAddress, this);
            _stub.setPortName(this.getGoodsPaygateSoapWSDDServiceName());
            return _stub;
        }
        catch (AxisFault e) {
            return null;
        }
    }

    public void setGoodsPaygateSoapEndpointAddress(String address) {
        this.GoodsPaygateSoap_address = address;
    }

    public Remote getPort(Class serviceEndpointInterface) throws ServiceException {
        try {
            if (GoodsPaygateSoap.class.isAssignableFrom(serviceEndpointInterface)) {
                GoodsPaygateSoapStub _stub = new GoodsPaygateSoapStub(new URL(this.GoodsPaygateSoap_address), this);
                _stub.setPortName(this.getGoodsPaygateSoapWSDDServiceName());
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
        if ("GoodsPaygateSoap".equals(inputPortName)) {
            return this.getGoodsPaygateSoap();
        }
        Remote _stub = this.getPort(serviceEndpointInterface);
        ((Stub)_stub).setPortName(portName);
        return _stub;
    }

    public QName getServiceName() {
        return new QName("http://tempuri.org/", "GoodsPaygate");
    }

    public Iterator getPorts() {
        if (this.ports == null) {
            this.ports = new HashSet<QName>();
            this.ports.add(new QName("http://tempuri.org/", "GoodsPaygateSoap"));
        }
        return this.ports.iterator();
    }

    public void setEndpointAddress(String portName, String address) throws ServiceException {
        if ("GoodsPaygateSoap".equals(portName)) {
            this.setGoodsPaygateSoapEndpointAddress(address);
            return;
        }
        throw new ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
    }

    public void setEndpointAddress(QName portName, String address) throws ServiceException {
        this.setEndpointAddress(portName.getLocalPart(), address);
    }
}

