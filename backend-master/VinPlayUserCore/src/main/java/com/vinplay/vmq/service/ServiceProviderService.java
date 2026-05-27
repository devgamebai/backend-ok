/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.Service
 *  javax.xml.rpc.ServiceException
 */
package com.vinplay.vmq.service;

import com.vinplay.vmq.service.ServiceProvider;
import java.net.URL;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;

public interface ServiceProviderService
extends Service {
    public String getsendMTAddress();

    public ServiceProvider getsendMT() throws ServiceException;

    public ServiceProvider getsendMT(URL var1) throws ServiceException;
}

