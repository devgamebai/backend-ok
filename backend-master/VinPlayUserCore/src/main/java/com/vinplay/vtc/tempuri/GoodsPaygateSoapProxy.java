/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.ServiceException
 *  javax.xml.rpc.Stub
 */
package com.vinplay.vtc.tempuri;

import com.vinplay.vtc.tempuri.GoodsPaygateLocator;
import com.vinplay.vtc.tempuri.GoodsPaygateSoap;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

public class GoodsPaygateSoapProxy
implements GoodsPaygateSoap {
    private String _endpoint = null;
    private GoodsPaygateSoap goodsPaygateSoap = null;

    public GoodsPaygateSoapProxy() {
        this._initGoodsPaygateSoapProxy();
    }

    public GoodsPaygateSoapProxy(String endpoint) {
        this._endpoint = endpoint;
        this._initGoodsPaygateSoapProxy();
    }

    private void _initGoodsPaygateSoapProxy() {
        try {
            this.goodsPaygateSoap = new GoodsPaygateLocator().getGoodsPaygateSoap();
            if (this.goodsPaygateSoap != null) {
                if (this._endpoint != null) {
                    ((Stub)this.goodsPaygateSoap)._setProperty("javax.xml.rpc.service.endpoint.address", this._endpoint);
                } else {
                    this._endpoint = (String)((Stub)this.goodsPaygateSoap)._getProperty("javax.xml.rpc.service.endpoint.address");
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
        if (this.goodsPaygateSoap != null) {
            ((Stub)this.goodsPaygateSoap)._setProperty("javax.xml.rpc.service.endpoint.address", this._endpoint);
        }
    }

    public GoodsPaygateSoap getGoodsPaygateSoap() {
        if (this.goodsPaygateSoap == null) {
            this._initGoodsPaygateSoapProxy();
        }
        return this.goodsPaygateSoap;
    }

    @Override
    public String requestTransaction(String requesData, String partnerCode, String commandType, String version) throws RemoteException {
        if (this.goodsPaygateSoap == null) {
            this._initGoodsPaygateSoapProxy();
        }
        return this.goodsPaygateSoap.requestTransaction(requesData, partnerCode, commandType, version);
    }
}

