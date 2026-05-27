/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.payprince;

import com.vinplay.payprince.ResultNotifyDTO;
import java.io.Serializable;

public class NotifyDTO
implements Serializable {
    private String status;
    private ResultNotifyDTO result;
    private String sign;

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ResultNotifyDTO getResult() {
        return this.result;
    }

    public void setResult(ResultNotifyDTO result) {
        this.result = result;
    }

    public String getSign() {
        return this.sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public NotifyDTO(String status, ResultNotifyDTO result, String sign) {
        this.status = status;
        this.result = result;
        this.sign = sign;
    }

    public NotifyDTO() {
    }
}

