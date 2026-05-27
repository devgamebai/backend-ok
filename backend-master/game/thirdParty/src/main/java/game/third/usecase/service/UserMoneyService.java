/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.vinplay.vbee.common.response.MoneyResponse
 *  com.vinplay.vbee.common.statics.TransType
 */
package game.third.usecase.service;

import com.vinplay.vbee.common.response.MoneyResponse;
import com.vinplay.vbee.common.statics.TransType;
import java.util.Map;

public interface UserMoneyService {
    public long getBalance(String var1);

    public boolean bet(String var1, long var2, long var4, String var6, String var7, String var8, long var9, Map<String, Object> var11);

    /**
     * SUN-1205/1206 — bet variant accepting an explicit
     * {@code validBetAmount} for rebate volume override. Use when the
     * vendor-pushed bet differs from the commission-eligible portion
     * (Dream Gaming hedge bets: banker+player same amount → valid_bet=0;
     * banker 20k + player 30k → valid_bet=10k). The wallet still
     * deducts the full {@code money}; only the downstream rebate
     * pipeline sees the smaller volume. Pass {@code 0} for legacy.
     */
    public boolean bet(String var1, long var2, long var4, String var6, String var7, String var8, long var9, Map<String, Object> var11, long validBetAmount);

    public boolean reward(String var1, long var2, String var4, String var5, String var6, Map<String, Object> var7);

    public boolean reward(String var1, long var2, String var4, String var5, String var6, Map<String, Object> var7, boolean var8);

    public boolean isUser(String var1);

    public boolean isUserAndCheckBalance(String var1, double var2);

    public MoneyResponse updateMoney(String var1, long var2, String var4, String var5, String var6, String var7, long var8, Long var10, TransType var11);
}

