/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.usercore.entities.OTPSenderResponse
 *  com.vinplay.vbee.common.models.UserModel
 */
package com.vinplay.otp.sender;

import com.vinplay.usercore.entities.OTPSenderResponse;
import com.vinplay.vbee.common.models.UserModel;

public interface OTPSenderService {
    public Sender name();

    public OTPSenderResponse send(UserModel var1, String var2, String var3) throws Exception;

    public static enum Sender {
        SMS("SMS"),
        TELEGRAM("TELEGRAM");

        private String sender;

        private Sender(String sender) {
            this.sender = sender;
        }

        public String toString() {
            return this.sender;
        }
    }
}

