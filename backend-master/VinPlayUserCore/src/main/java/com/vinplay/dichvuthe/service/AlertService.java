/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.dichvuthe.service;

import java.util.List;

public interface AlertService {
    public boolean sendSMS2List(List<String> var1, String var2, boolean var3);

    public boolean sendSMS2One(String var1, String var2, boolean var3);

    public boolean sendSMS2User(String var1, String var2);

    public boolean sendEmail(String subject, String template, String params, List<String> receives);

    public boolean alert2List(List<String> var1, String var2, boolean var3);

    public boolean alert2One(String var1, String var2, boolean var3);

    public boolean SendSMSEsms(String var1, String var2);
}

