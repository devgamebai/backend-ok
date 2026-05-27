/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.usercore.service;

import com.vinplay.payment.entities.Response;
import com.vinplay.payment.entities.UserBank;

public interface UserBankService {
    public boolean isAddBank(String var1);

    public Response add(UserBank var1);

    public Response update(UserBank var1);

    public Response delete(long var1);

    public Response getById(long var1);

    public Response getByBankNumber(String var1, String var2);

    public UserBank getByDetail(String var1, String var2);

    public Response search(String var1, String var2, String var3, int var4, int var5, int var6);

    public Response search(String var1, String var2, String var3, String var4, int var5, int var6, int var7);
}

