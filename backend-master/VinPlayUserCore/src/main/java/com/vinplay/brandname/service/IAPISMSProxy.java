/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.ServiceException
 *  javax.xml.rpc.Stub
 */
package com.vinplay.brandname.service;

import com.vinplay.brandname.enties.DataSms;
import com.vinplay.brandname.enties.ListPhone;
import com.vinplay.brandname.service.APISMSLocator;
import com.vinplay.brandname.service.IAPISMS;
import java.rmi.RemoteException;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

public class IAPISMSProxy
implements IAPISMS {
    private String _endpoint = null;
    private IAPISMS iAPISMS = null;

    public IAPISMSProxy() {
        this._initIAPISMSProxy();
    }

    public IAPISMSProxy(String endpoint) {
        this._endpoint = endpoint;
        this._initIAPISMSProxy();
    }

    private void _initIAPISMSProxy() {
        try {
            this.iAPISMS = new APISMSLocator().getBasicHttpBinding_IAPISMS();
            if (this.iAPISMS != null) {
                if (this._endpoint != null) {
                    ((Stub)this.iAPISMS)._setProperty("javax.xml.rpc.service.endpoint.address", this._endpoint);
                } else {
                    this._endpoint = (String)((Stub)this.iAPISMS)._getProperty("javax.xml.rpc.service.endpoint.address");
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
        if (this.iAPISMS != null) {
            ((Stub)this.iAPISMS)._setProperty("javax.xml.rpc.service.endpoint.address", this._endpoint);
        }
    }

    public IAPISMS getIAPISMS() {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS;
    }

    @Override
    public String pushMsg2Phone(String sender, String msg, String phone, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.pushMsg2Phone(sender, msg, phone, username, password);
    }

    @Override
    public String pushMsg2PhoneDLVR(String sender, String msg, String phone, String requestId, Integer idclient, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.pushMsg2PhoneDLVR(sender, msg, phone, requestId, idclient, username, password);
    }

    @Override
    public String pushMsgAdvDirect(String sender, String msg, String phone, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.pushMsgAdvDirect(sender, msg, phone, username, password);
    }

    @Override
    public String sendOTP(String sender, String msg, String phone, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.sendOTP(sender, msg, phone, username, password);
    }

    @Override
    public String pushMsg2ListPhone(String sender, String msg, String[] phone, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.pushMsg2ListPhone(sender, msg, phone, username, password);
    }

    @Override
    public String pushMsg2ListPhoneDLVR(String sender, String msg, ListPhone[] phone, Integer idclient, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.pushMsg2ListPhoneDLVR(sender, msg, phone, idclient, username, password);
    }

    @Override
    public String pushBulkSms(String sender, DataSms[] data, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.pushBulkSms(sender, data, username, password);
    }

    @Override
    public String getTelco(String phone, String username, String password) throws RemoteException {
        if (this.iAPISMS == null) {
            this._initIAPISMSProxy();
        }
        return this.iAPISMS.getTelco(phone, username, password);
    }
}

