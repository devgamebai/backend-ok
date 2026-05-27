package com.vinplay.livecasino.api.wsclient;

/**
 * Stub for LobbyModule compatibility.
 * The original live casino SDK is not available.
 */
public class TCGamingAPICommon {
    public static TCGamingAPICommon getInstance() { return new TCGamingAPICommon(); }
    public String getBalance(String username) { return "0"; }
    public String getToken(String username) { return ""; }
}
