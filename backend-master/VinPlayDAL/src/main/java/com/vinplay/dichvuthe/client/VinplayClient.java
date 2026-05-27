/*
 * Decompiled with CFR 0.152.
 */
package com.vinplay.dichvuthe.client;

import java.util.List;

public class VinplayClient {
    public static Object cashOutByBank(String id, String bank, String account, String name, int amount, String sign) throws Exception {
        return new Object();
    }

    public static Object reCheckCashOutByBank(String id, String sign) throws Exception {
        return new Object();
    }

    public static String sendAleftSMS(List<String> receives, String content, boolean call) throws Exception {
        return "";
    }

    public static String aleft(List<String> receives, String content, boolean call) throws Exception {
        return "";
    }

    public static String sendEmail(String subject, String content, List<String> receives) throws Exception {
        return "";
    }

    // SUN-1289 build-fix: VinPlayUserCore.RechargeServiceImpl imports
    // com.vinplay.dichvuthe.client.VinplayClient and calls these two methods.
    // VinPlayUserCore depends on VinPlayDAL, and the gradle compile classpath
    // surfaces this stub before VinPlayUserCore's own VinplayClient.java in
    // the same package. Without these stub signatures the compile fails
    // ("cannot find symbol method rechargeByCard"). Stubs are unreachable from
    // VinPlayDAL itself — the real implementations live in
    // VinPlayUserCore's VinplayClient and win at runtime by being on the
    // game-server classpath. The duplicate-FQN-class layout itself is a
    // separate concern (track as SUN-1291).
    public static Object rechargeByCard(String id, String provider, String serial, String pin) throws Exception {
        return null;
    }

    public static Object rechargeByVinCard(String id, String partner, String serial, String pin, String target) throws Exception {
        return null;
    }
}

