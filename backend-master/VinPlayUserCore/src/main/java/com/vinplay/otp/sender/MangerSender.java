/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.otp.sender;

import com.vinplay.otp.sender.OTPSenderService;
import com.vinplay.otp.sender.impl.TelegramSender;
import java.util.HashMap;
import java.util.Map;

public class MangerSender {
    private static Map<String, OTPSenderService> mapSenders = new HashMap<String, OTPSenderService>();

    public static void init() {
        MangerSender.add(new TelegramSender());
    }

    public static void add(OTPSenderService sender) {
        mapSenders.put(sender.name().toString(), sender);
    }

    public static OTPSenderService getName(String name) {
        if (mapSenders.containsKey(name = name.toUpperCase())) {
            return mapSenders.get(name);
        }
        return null;
    }
}

