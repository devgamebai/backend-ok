/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.ServiceException
 *  javax.xml.rpc.Stub
 */
package com.vinplay.vmq.service;

import com.vinplay.vmq.service.ServiceProvider;
import com.vinplay.vmq.service.ServiceProviderServiceLocator;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

public class ServiceProviderProxy
implements ServiceProvider {
    private String _endpoint = null;
    private ServiceProvider serviceProvider = null;

    public ServiceProviderProxy() {
        this._initServiceProviderProxy();
    }

    public ServiceProviderProxy(String endpoint) {
        this._endpoint = endpoint;
        this._initServiceProviderProxy();
    }

    private void _initServiceProviderProxy() {
        try {
            this.serviceProvider = new ServiceProviderServiceLocator().getsendMT();
            if (this.serviceProvider != null) {
                if (this._endpoint != null) {
                    ((Stub)this.serviceProvider)._setProperty("javax.xml.rpc.service.endpoint.address", this._endpoint);
                } else {
                    this._endpoint = (String)((Stub)this.serviceProvider)._getProperty("javax.xml.rpc.service.endpoint.address");
                }
            }
        }
        catch (ServiceException serviceException) {
            // empty catch block
        }
    }

    public String getEndpoint() {
        return this._endpoint;
    }

    public void setEndpoint(String endpoint) {
        this._endpoint = endpoint;
        if (this.serviceProvider != null) {
            ((Stub)this.serviceProvider)._setProperty("javax.xml.rpc.service.endpoint.address", this._endpoint);
        }
    }

    public ServiceProvider getServiceProvider() {
        if (this.serviceProvider == null) {
            this._initServiceProviderProxy();
        }
        return this.serviceProvider;
    }

    @Override
    public int sendMT(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType) throws RemoteException {
        if (this.serviceProvider == null) {
            this._initServiceProviderProxy();
        }
        return this.serviceProvider.sendMT(userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType);
    }

    @Override
    public int sendMTEx(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType) throws RemoteException {
        if (this.serviceProvider == null) {
            this._initServiceProviderProxy();
        }
        return this.serviceProvider.sendMTEx(userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType);
    }

    @Override
    public int sendMTEx2(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType, int messageClass, int mwi, int dataCoding) throws RemoteException {
        if (this.serviceProvider == null) {
            this._initServiceProviderProxy();
        }
        return this.serviceProvider.sendMTEx2(userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType, messageClass, mwi, dataCoding);
    }

    @Override
    public int sendMTPush(String userID, String message, String serviceID, String commandCode, String messageType, String requestID, String totalMessage, String messageIndex, String isMore, String contentType) throws RemoteException {
        if (this.serviceProvider == null) {
            this._initServiceProviderProxy();
        }
        return this.serviceProvider.sendMTPush(userID, message, serviceID, commandCode, messageType, requestID, totalMessage, messageIndex, isMore, contentType);
    }
}

