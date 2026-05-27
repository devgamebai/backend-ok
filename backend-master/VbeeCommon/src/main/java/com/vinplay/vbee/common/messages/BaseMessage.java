/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.vbee.common.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public abstract class BaseMessage
implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String id;
    protected String createTime = VinPlayUtils.getCurrentDateTime();

    public byte[] toBytes() {
        byte[] bytes;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.flush();
            oos.reset();
            bytes = baos.toByteArray();
            oos.close();
            baos.close();
        }
        catch (IOException e) {
            bytes = new byte[]{};
        }
        return bytes;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString((Object)this);
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public static BaseMessage fromBytes(byte[] body) {
        BaseMessage obj = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(body);
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = (BaseMessage)ois.readObject();
            ois.close();
            bis.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return obj;
    }
}

