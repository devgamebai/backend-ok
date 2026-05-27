/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  org.json.JSONException
 *  org.json.JSONObject
 */
package com.vinplay.iap.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class Purchase {
    private String mItemType;
    private String mOrderId;
    private String mPackageName;
    private String mSku;
    private long mPurchaseTime;
    private int mPurchaseState;
    private String mDeveloperPayload;
    private String mToken;
    private String mOriginalJson;
    private String mSignature;

    public Purchase(String itemType, String jsonPurchaseInfo, String signature) throws JSONException {
        this.mItemType = itemType;
        this.mOriginalJson = jsonPurchaseInfo;
        JSONObject o = new JSONObject(this.mOriginalJson);
        this.mOrderId = o.optString("orderId");
        this.mPackageName = o.optString("packageName");
        this.mSku = o.optString("productId");
        this.mPurchaseTime = o.optLong("purchaseTime");
        this.mPurchaseState = o.optInt("purchaseState");
        this.mDeveloperPayload = o.optString("developerPayload");
        this.mToken = o.optString("token", o.optString("purchaseToken"));
        this.mSignature = signature;
    }

    public String getItemType() {
        return this.mItemType;
    }

    public String getOrderId() {
        return this.mOrderId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getSku() {
        return this.mSku;
    }

    public long getPurchaseTime() {
        return this.mPurchaseTime;
    }

    public int getPurchaseState() {
        return this.mPurchaseState;
    }

    public String getDeveloperPayload() {
        return this.mDeveloperPayload;
    }

    public String getToken() {
        return this.mToken;
    }

    public String getOriginalJson() {
        return this.mOriginalJson;
    }

    public String getSignature() {
        return this.mSignature;
    }

    public String toString() {
        return "PurchaseInfo(type:" + this.mItemType + "):" + this.mOriginalJson;
    }
}

