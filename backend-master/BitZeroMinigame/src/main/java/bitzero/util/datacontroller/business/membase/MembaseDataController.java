/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  net.spy.memcached.AddrUtil
 *  net.spy.memcached.CASResponse
 *  net.spy.memcached.CASValue
 *  net.spy.memcached.ConnectionFactory
 *  net.spy.memcached.ConnectionFactoryBuilder
 *  net.spy.memcached.MemcachedClient
 *  net.spy.memcached.internal.OperationFuture
 */
package bitzero.util.datacontroller.business.membase;

import bitzero.server.config.ConfigHandle;
import bitzero.util.common.business.CommonHandle;
import bitzero.util.datacontroller.business.DataControllerException;
import bitzero.util.datacontroller.business.IDataController;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;

public class MembaseDataController
implements IDataController {
    static final Boolean NEW_DATABASE = ConfigHandle.instance().get("odservers") != null;
    MemcachedClient cacheCli;
    MemcachedClient dataCli;
    MemcachedClient new_dataCli;

    public MembaseDataController() {
        try {
            ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder();
            cfb.setOpQueueMaxBlockTime(ConfigHandle.instance().getLong("opsBlockTime").longValue());
            cfb.setOpTimeout(ConfigHandle.instance().getLong("opsTimeOut").longValue());
            this.new_dataCli = new MemcachedClient(cfb.build(), AddrUtil.getAddresses((String)ConfigHandle.instance().get("dservers")));
            this.cacheCli = new MemcachedClient(cfb.build(), AddrUtil.getAddresses((String)ConfigHandle.instance().get("cservers")));
            if (NEW_DATABASE.booleanValue()) {
                this.dataCli = new MemcachedClient(cfb.build(), AddrUtil.getAddresses((String)ConfigHandle.instance().get("odservers")));
            }
        }
        catch (IOException e) {
            CommonHandle.writeErrLog(e);
        }
    }

    @Override
    public Object get(String name) {
        Object obj = this.new_dataCli.get(name);
        if (obj != null || !NEW_DATABASE.booleanValue()) {
            return obj;
        }
        return this.dataCli.get(name);
    }

    public Object getOldData(String name) {
        return this.dataCli.get(name);
    }

    public Object getNewData(String name) {
        return this.new_dataCli.get(name);
    }

    public Object getWithTranscode(String name) {
        return null;
    }

    @Override
    public void set(String name, Object data) {
        this.new_dataCli.set(name, 0, data);
    }

    @Override
    public void add(String name, Object data) {
        this.new_dataCli.add(name, 0, data);
    }

    @Override
    public Object getCache(String name) {
        return this.cacheCli.get(name);
    }

    @Override
    public void setCache(String name, int expire, Object data) {
        this.cacheCli.set(name, expire, data);
    }

    public void shutdown() {
        if (this.new_dataCli != null) {
            this.new_dataCli.shutdown(3, TimeUnit.SECONDS);
        }
        if (this.cacheCli != null) {
            this.cacheCli.shutdown(3, TimeUnit.SECONDS);
        }
    }

    public long getCASValue(String name) throws DataControllerException {
        CASValue casVak = this.new_dataCli.gets(name);
        if (casVak == null) {
            return 0;
        }
        return casVak.getCas();
    }

    public CASValue getS(String name) {
        return this.new_dataCli.gets(name);
    }

    public CASResponse checkAndSet(String name, long casValue, Object data) throws DataControllerException {
        return this.new_dataCli.cas(name, casValue, data);
    }

    @Override
    public Map<String, Object> multiget(List<String> keys) {
        return this.new_dataCli.getBulk(keys);
    }

    @Override
    public void delete(String name) throws DataControllerException {
        this.new_dataCli.delete(name);
    }

    @Override
    public void deleteCache(String name) {
        this.cacheCli.delete(name);
    }
}

