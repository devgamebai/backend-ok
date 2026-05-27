/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.Service
 *  javax.xml.rpc.ServiceException
 */
package com.vinplay.brandname.service;

import com.vinplay.brandname.service.IAPISMS;
import java.net.URL;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;

public interface APISMS
extends Service {
    public String getBasicHttpBinding_IAPISMSAddress();

    public IAPISMS getBasicHttpBinding_IAPISMS() throws ServiceException;

    public IAPISMS getBasicHttpBinding_IAPISMS(URL var1) throws ServiceException;
}

