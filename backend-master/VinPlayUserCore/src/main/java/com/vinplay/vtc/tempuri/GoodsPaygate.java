/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  javax.xml.rpc.Service
 *  javax.xml.rpc.ServiceException
 */
package com.vinplay.vtc.tempuri;

import com.vinplay.vtc.tempuri.GoodsPaygateSoap;
import java.net.URL;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;

public interface GoodsPaygate
extends Service {
    public String getGoodsPaygateSoapAddress();

    public GoodsPaygateSoap getGoodsPaygateSoap() throws ServiceException;

    public GoodsPaygateSoap getGoodsPaygateSoap(URL var1) throws ServiceException;
}

